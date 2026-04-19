package com.exam.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Consumer AMQP — WildFly 2 (Capa de Datos).
 *
 * Este Singleton @Startup escucha la cola "email.notification" en RabbitMQ.
 * Cuando recibe un mensaje:
 *  1. Deserializa el payload JSON
 *  2. Envía el correo al estudiante via JavaMail
 *  3. Marca email_sent=true en la base de datos de estudiantes (DB2)
 *  4. ACK o NACK según resultado
 *
 * Patrón: Consumer desacoplado por tiempo (tiempo de desempeño).
 */
@Singleton
@Startup
public class ExamResultConsumer {

    private static final Logger LOG = Logger.getLogger(ExamResultConsumer.class.getName());
    private static final String QUEUE = "email.notification";
    private static final String EXCHANGE = "exam.exchange";

    @Inject
    private EmailService emailService;

    @PersistenceContext(unitName = "StudentsPU")
    private EntityManager studentsEM;

    private Connection connection;
    private Channel channel;
    private ExecutorService executor;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @PostConstruct
    public void startConsuming() {
        executor = Executors.newSingleThreadExecutor();
        executor.submit(this::connectAndConsume);
    }

    private void connectAndConsume() {
        // Esperar a que RabbitMQ esté disponible
        int retries = 0;
        while (retries < 10) {
            try {
                Thread.sleep(5000);
                ConnectionFactory factory = new ConnectionFactory();
                factory.setHost(System.getenv().getOrDefault("RABBITMQ_HOST", "rabbitmq"));
                factory.setPort(Integer.parseInt(System.getenv().getOrDefault("RABBITMQ_PORT", "5672")));
                factory.setUsername(System.getenv().getOrDefault("RABBITMQ_USER", "admin"));
                factory.setPassword(System.getenv().getOrDefault("RABBITMQ_PASS", "admin123"));
                factory.setAutomaticRecoveryEnabled(true);
                factory.setNetworkRecoveryInterval(5000);

                connection = factory.newConnection();
                channel = connection.createChannel();

                // Declarar exchange y cola (idempotente)
                channel.exchangeDeclare(EXCHANGE, "direct", true);
                channel.queueDeclare(QUEUE, true, false, false, null);
                channel.queueBind(QUEUE, EXCHANGE, "email");

                // Prefetch: procesar de a 1 mensaje a la vez
                channel.basicQos(1);

                // Registrar consumer
            
               
                channel.basicConsume(QUEUE, false, buildConsumer(), consumerTag -> LOG.warning("Consumer cancelado: " + consumerTag));

                LOG.info("Consumer RabbitMQ iniciado - escuchando cola: " + QUEUE);
                return;

            } catch (Exception e) {
                retries++;
                LOG.log(Level.WARNING, "Intento " + retries + " de conexión RabbitMQ fallido: " + e.getMessage());
            }
        }
        LOG.severe("No se pudo conectar a RabbitMQ después de 10 intentos");
    }

    /**
     * Construye el DeliverCallback que procesa cada mensaje.
     */
    private DeliverCallback buildConsumer() {
        return (consumerTag, delivery) -> {
            String body = new String(delivery.getBody());
            LOG.info("Mensaje recibido de RabbitMQ: " + body);

            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> payload = mapper.readValue(body, Map.class);

                Long studentId      = Long.valueOf(payload.get("studentId").toString());
                String studentEmail = payload.get("studentEmail").toString();
                String studentName  = payload.get("studentName").toString();
                String examTitle    = payload.get("examTitle").toString();
                double score        = Double.parseDouble(payload.get("score").toString());
                boolean passed      = Boolean.parseBoolean(payload.get("passed").toString());
                Long submissionId   = Long.valueOf(payload.get("submissionId").toString());

                // Enviar correo
                boolean sent = emailService.sendExamResult(studentEmail, studentName, examTitle, score, passed);

                // Actualizar flag email_sent en DB Estudiantes
                if (sent) {
                    updateEmailSentFlag(studentId, submissionId);
                }

                // ACK: mensaje procesado correctamente
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                LOG.info("ACK enviado - correo procesado para: " + studentEmail);

            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Error procesando mensaje, NACK sin requeue: " + body, e);
                // NACK sin re-enqueue para evitar loops infinitos
                channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
            }
        };
    }

    /**
     * Actualiza el flag email_sent en la tabla grades (DB Estudiantes).
     */
    private void updateEmailSentFlag(Long studentId, Long submissionId) {
        try {
            studentsEM.createQuery(
                "UPDATE Grade g SET g.emailSent = true " +
                "WHERE g.studentId = :studentId AND g.examSubmissionId = :submissionId"
            )
            .setParameter("studentId", studentId)
            .setParameter("submissionId", submissionId)
            .executeUpdate();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "No se pudo actualizar email_sent", e);
        }
    }

    @PreDestroy
    public void stop() {
        try {
            if (channel != null && channel.isOpen()) channel.close();
            if (connection != null && connection.isOpen()) connection.close();
            if (executor != null) executor.shutdownNow();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error cerrando consumer RabbitMQ", e);
        }
    }
}
