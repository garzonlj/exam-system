package com.exam.boundary;

import com.exam.model.Exam;
import com.exam.model.Grade;
import com.exam.model.Student;
import com.exam.service.ExamService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REST API - Punto de entrada para la interfaz HTML.
 * Expone endpoints para listar exámenes, estudiantes y procesar submissions.
 */
@Path("/exams")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ExamResource {

    private static final Logger LOG = Logger.getLogger(ExamResource.class.getName());

    @Inject
    private ExamService examService;

    /**
     * GET /api/exams
     * Lista todos los exámenes disponibles.
     */
    @GET
    public Response getAllExams() {
        try {
            List<Exam> exams = examService.getAllExams();
            return Response.ok(exams).build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error obteniendo exámenes", e);
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    /**
     * GET /api/exams/{id}
     * Obtiene un examen con sus preguntas.
     */
    @GET
    @Path("/{id}")
    public Response getExam(@PathParam("id") Long id) {
        try {
            Exam exam = examService.getExamById(id);
            if (exam == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Examen no encontrado"))
                        .build();
            }
            return Response.ok(exam).build();
        } catch (Exception e) {
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }

    /**
     * GET /api/exams/students
     * Lista todos los estudiantes.
     */
    @GET
    @Path("/students/all")
    public Response getAllStudents() {
        try {
            List<Student> students = examService.getAllStudents();
            return Response.ok(students).build();
        } catch (Exception e) {
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }

    /**
     * GET /api/exams/students/{id}/grades
     * Historial de notas de un estudiante.
     */
    @GET
    @Path("/students/{id}/grades")
    public Response getStudentGrades(@PathParam("id") Long studentId) {
        try {
            List<Grade> grades = examService.getStudentGrades(studentId);
            return Response.ok(grades).build();
        } catch (Exception e) {
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }

    /**
     * POST /api/exams/{id}/submit
     * Envía las respuestas de un estudiante.
     *
     * Body esperado:
     * {
     *   "studentId": 1,
     *   "answers": { "1": 2, "2": 5, "3": 7 }
     * }
     * (questionId -> selectedOptionId)
     */
    @POST
    @Path("/{id}/submit")
    public Response submitExam(@PathParam("id") Long examId, Map<String, Object> body) {
        try {
            Long studentId = Long.valueOf(body.get("studentId").toString());

            @SuppressWarnings("unchecked")
            Map<String, Object> rawAnswers = (Map<String, Object>) body.get("answers");

            // Convertir claves y valores a Long
            java.util.Map<Long, Long> answers = new java.util.HashMap<>();
            for (Map.Entry<String, Object> entry : rawAnswers.entrySet()) {
                answers.put(Long.parseLong(entry.getKey()),
                        Long.parseLong(entry.getValue().toString()));
            }

            Map<String, Object> result = examService.submitExam(examId, studentId, answers);
            return Response.ok(result).build();

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error procesando examen", e);
            return Response.serverError()
                    .entity(Map.of("error", "Error procesando examen: " + e.getMessage()))
                    .build();
        }
    }
}
