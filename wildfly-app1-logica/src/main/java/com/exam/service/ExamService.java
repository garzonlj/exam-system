package com.exam.service;

import com.exam.messaging.RabbitMQConnection;
import com.exam.model.*;
import com.exam.service.StudentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servicio principal de lógica de negocio.
 *
 * Maneja la transacción distribuida entre las dos bases de datos:
 * 1. DB Exámenes (ExamsPU)  -> Guarda submission + respuestas del estudiante
 * 2. DB Estudiantes (StudentsPU) -> Guarda la nota del estudiante
 * 3. RabbitMQ -> Publica evento para envío de correo (desacoplado por tiempo)
 *
 * El patrón usado es Saga con compensación manual en caso de fallo.
 */
@Stateless
public class ExamService {

    private static final Logger LOG = Logger.getLogger(ExamService.class.getName());

    @PersistenceContext(unitName = "ExamsPU")
    private EntityManager examsEM;

    @Inject
    private StudentService studentService;

    @Inject
    private RabbitMQConnection rabbitMQ;

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    /**
     * Obtiene todos los exámenes disponibles.
     */
    public List<Exam> getAllExams() {
        return examsEM.createQuery("SELECT e FROM Exam e", Exam.class).getResultList();
    }

    /**
     * Obtiene un examen con sus preguntas y opciones.
     */
    public Exam getExamById(Long examId) {
        return examsEM.find(Exam.class, examId);
    }

    /**
     * Obtiene todos los estudiantes.
     */
    public List<Student> getAllStudents() {
        return studentService.findAllStudents();
    }

    /**
     * MÉTODO PRINCIPAL: Procesa la presentación de un examen.
     *
     * Implementa manejo de transacciones distribuidas usando el patrón SAGA:
     * - Fase 1: Persistir en DB Exámenes (respuestas + calificación)
     * - Fase 2: Persistir en DB Estudiantes (nota)
     * - Fase 3: Publicar en RabbitMQ para notificación por correo
     *
     * Si la Fase 2 falla, se ejecuta compensación en la Fase 1.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Map<String, Object> submitExam(Long examId, Long studentId,
                                           Map<Long, Long> answers) throws Exception {

        LOG.info("Procesando presentación: examen=" + examId + " estudiante=" + studentId);

        // === PASO 1: Obtener examen y estudiante ===
        Exam exam = examsEM.find(Exam.class, examId);
        if (exam == null) throw new IllegalArgumentException("Examen no encontrado: " + examId);

        Student student = studentService.findStudent(studentId);
        if (student == null) throw new IllegalArgumentException("Estudiante no encontrado: " + studentId);

        // === PASO 2: Calcular puntaje ===
        double score = calculateScore(exam, answers);
        boolean passed = score >= exam.getPassingScore();

        // === FASE 1: Persistir en DB Exámenes ===
        ExamSubmission submission = new ExamSubmission();
        submission.setExam(exam);
        submission.setStudentId(studentId);
        submission.setScore(score);
        submission.setStatus("SUBMITTED");

        List<StudentAnswer> studentAnswers = new ArrayList<>();
        for (Question question : exam.getQuestions()) {
            Long selectedOptionId = answers.get(question.getId());
            if (selectedOptionId != null) {
                StudentAnswer sa = new StudentAnswer();
                sa.setExamSubmission(submission);
                sa.setQuestion(question);
                AnswerOption option = examsEM.find(AnswerOption.class, selectedOptionId);
                sa.setSelectedOption(option);
                studentAnswers.add(sa);
            }
        }
        submission.setAnswers(studentAnswers);
        examsEM.persist(submission);
        examsEM.flush(); // Forzar el ID antes de continuar

        LOG.info("Fase 1 OK - Submission guardada con ID: " + submission.getId());

        // === FASE 2: Persistir en DB Estudiantes ===
        try {
            Grade grade = new Grade();
            grade.setStudentId(studentId);
            grade.setExamId(examId);
            grade.setExamSubmissionId(submission.getId());
            grade.setScore(score);
            grade.setPassed(passed);
            grade.setEmailSent(false);
            studentService.saveGrade(grade);

            LOG.info("Fase 2 OK - Nota guardada para estudiante: " + studentId);

            // === FASE 3: Publicar evento en RabbitMQ (desacoplado) ===
            publishEmailNotification(student, exam, score, passed, submission.getId());

        } catch (Exception e) {
            // Compensación SAGA: si falla la fase 2, actualizamos estado en DB Exámenes
            LOG.log(Level.SEVERE, "Fase 2 FALLÓ - Ejecutando compensación SAGA", e);
            submission.setStatus("ERROR_GRADE");
            examsEM.merge(submission);
            throw new RuntimeException("Error en transacción distribuida - compensación aplicada: " + e.getMessage(), e);
        }

        // Resultado final
        Map<String, Object> result = new HashMap<>();
        result.put("submissionId", submission.getId());
        result.put("studentName", student.getFirstName() + " " + student.getLastName());
        result.put("examTitle", exam.getTitle());
        result.put("score", Math.round(score * 100.0) / 100.0);
        result.put("passed", passed);
        result.put("passingScore", exam.getPassingScore());
        result.put("message", passed ? "¡Felicitaciones! Aprobaste el examen." : "No aprobaste. Puntaje mínimo: " + exam.getPassingScore() + "%");

        return result;
    }

    /**
     * Calcula el puntaje como porcentaje de respuestas correctas.
     */
    private double calculateScore(Exam exam, Map<Long, Long> answers) {
        if (exam.getQuestions() == null || exam.getQuestions().isEmpty()) return 0.0;

        long correct = exam.getQuestions().stream()
                .filter(q -> {
                    Long selectedId = answers.get(q.getId());
                    if (selectedId == null) return false;
                    return q.getOptions().stream()
                            .anyMatch(o -> o.getId().equals(selectedId) && o.getIsCorrect());
                })
                .count();

        return (correct * 100.0) / exam.getQuestions().size();
    }

    /**
     * Publica mensaje en RabbitMQ para que el consumer (WildFly 2)
     * procese el envío de correo de forma asíncrona y desacoplada.
     */
    private void publishEmailNotification(Student student, Exam exam,
                                           double score, boolean passed,
                                           Long submissionId) {
        if (!rabbitMQ.isConnected()) {
            LOG.warning("RabbitMQ no disponible - omitiendo notificación email");
            return;
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("studentId", student.getId());
            payload.put("studentName", student.getFirstName() + " " + student.getLastName());
            payload.put("studentEmail", student.getEmail());
            payload.put("examTitle", exam.getTitle());
            payload.put("score", score);
            payload.put("passed", passed);
            payload.put("submissionId", submissionId);
            payload.put("timestamp", System.currentTimeMillis());

            byte[] body = mapper.writeValueAsBytes(payload);

            rabbitMQ.getChannel().basicPublish(
                    RabbitMQConnection.EXCHANGE_NAME,
                    "email",
                    null,
                    body
            );

            LOG.info("Evento publicado en RabbitMQ para email: " + student.getEmail());
        } catch (Exception e) {
            // El fallo del email NO revierte la transacción principal (patrón desacoplado)
            LOG.log(Level.WARNING, "Error publicando evento email en RabbitMQ (no crítico)", e);
        }
    }

    /**
     * Obtiene el historial de notas de un estudiante.
     */
    public List<Grade> getStudentGrades(Long studentId) {
        return studentService.findGradesByStudent(studentId);
    }
}