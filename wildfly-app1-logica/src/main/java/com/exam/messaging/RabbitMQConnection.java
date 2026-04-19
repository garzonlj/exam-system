package com.exam.messaging;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton EJB que mantiene la conexión AMQP con RabbitMQ.
 * Declara las colas y exchanges al inicio (Startup).
 */
@Singleton
@Startup
public class RabbitMQConnection {

    private static final Logger LOG = Logger.getLogger(RabbitMQConnection.class.getName());

    public static final String EXAM_SUBMISSION_QUEUE = "exam.submission";
    public static final String EMAIL_NOTIFICATION_QUEUE = "email.notification";
    public static final String EXCHANGE_NAME = "exam.exchange";

    private Connection connection;
    private Channel channel;

    @PostConstruct
    public void init() {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(System.getenv().getOrDefault("RABBITMQ_HOST", "rabbitmq"));
            factory.setPort(Integer.parseInt(System.getenv().getOrDefault("RABBITMQ_PORT", "5672")));
            factory.setUsername(System.getenv().getOrDefault("RABBITMQ_USER", "admin"));
            factory.setPassword(System.getenv().getOrDefault("RABBITMQ_PASS", "admin123"));
            factory.setAutomaticRecoveryEnabled(true);
            factory.setNetworkRecoveryInterval(5000);

            connection = factory.newConnection();
            channel = connection.createChannel();

            // Declarar exchange tipo direct
            channel.exchangeDeclare(EXCHANGE_NAME, "direct", true);

            // Declarar colas durables
            channel.queueDeclare(EXAM_SUBMISSION_QUEUE, true, false, false, null);
            channel.queueDeclare(EMAIL_NOTIFICATION_QUEUE, true, false, false, null);

            // Bind colas al exchange
            channel.queueBind(EXAM_SUBMISSION_QUEUE, EXCHANGE_NAME, "submission");
            channel.queueBind(EMAIL_NOTIFICATION_QUEUE, EXCHANGE_NAME, "email");

            LOG.info("Conexión RabbitMQ establecida correctamente");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error conectando a RabbitMQ: " + e.getMessage(), e);
        }
    }

    public Channel getChannel() {
        return channel;
    }

    public boolean isConnected() {
        return connection != null && connection.isOpen();
    }

    @PreDestroy
    public void close() {
        try {
            if (channel != null && channel.isOpen()) channel.close();
            if (connection != null && connection.isOpen()) connection.close();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error cerrando conexión RabbitMQ", e);
        }
    }
}
