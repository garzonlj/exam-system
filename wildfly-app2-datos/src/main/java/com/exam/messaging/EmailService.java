package com.exam.messaging;

import jakarta.ejb.Stateless;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servicio de envío de correo electrónico usando JavaMail (Jakarta Mail).
 * Configurado para SMTP con TLS (compatible con Gmail/SendGrid).
 */
@Stateless
public class EmailService {

    private static final Logger LOG = Logger.getLogger(EmailService.class.getName());

    private final String mailHost   = System.getenv().getOrDefault("MAIL_HOST", "smtp.gmail.com");
    private final String mailPort   = System.getenv().getOrDefault("MAIL_PORT", "587");
    private final String mailUser   = System.getenv().getOrDefault("MAIL_USER", "noreply@exam.edu.co");
    private final String mailPass   = System.getenv().getOrDefault("MAIL_PASS", "");

    /**
     * Envía el correo de resultado del examen al estudiante.
     */
    public boolean sendExamResult(String toEmail, String studentName,
                                   String examTitle, double score, boolean passed) {
        try {
            Session session = buildSession();
            MimeMessage message = new MimeMessage(session);

            message.setFrom(new InternetAddress(mailUser, "Sistema de Exámenes"));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject(passed
                    ? "✅ Resultado Examen: " + examTitle + " - APROBADO"
                    : "❌ Resultado Examen: " + examTitle + " - NO APROBADO");
            message.setContent(buildHtmlBody(studentName, examTitle, score, passed), "text/html; charset=utf-8");

            Transport.send(message);
            LOG.info("Correo enviado exitosamente a: " + toEmail);
            return true;

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error enviando correo a " + toEmail, e);
            return false;
        }
    }

    private Session buildSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host", mailHost);
        props.put("mail.smtp.port", mailPort);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.trust", mailHost);

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(mailUser, mailPass);
            }
        });
    }

    /**
     * Genera el HTML del correo de resultado.
     */
    private String buildHtmlBody(String studentName, String examTitle,
                                  double score, boolean passed) {
        String color     = passed ? "#27ae60" : "#e74c3c";
        String icon      = passed ? "✅" : "❌";
        String statusMsg = passed ? "APROBADO" : "NO APROBADO";
        String scoreStr  = String.format("%.1f%%", score);

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body style='font-family:Arial,sans-serif;background:#f5f6fa;margin:0;padding:20px'>" +
               "<div style='max-width:600px;margin:0 auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,.1)'>" +
               "<div style='background:#2c3e50;padding:32px;text-align:center'>" +
               "<h1 style='color:#fff;margin:0;font-size:24px'>Sistema de Evaluaciones</h1>" +
               "<p style='color:#bdc3c7;margin:8px 0 0'>Universidad</p></div>" +
               "<div style='padding:40px'>" +
               "<p style='font-size:16px;color:#555'>Estimado/a <strong>" + studentName + "</strong>,</p>" +
               "<p style='color:#555'>A continuación encontrará el resultado de su evaluación:</p>" +
               "<div style='background:#f8f9fa;border-radius:10px;padding:24px;margin:24px 0;text-align:center'>" +
               "<p style='font-size:14px;color:#888;margin:0 0 8px;text-transform:uppercase;letter-spacing:1px'>Examen</p>" +
               "<h2 style='color:#2c3e50;margin:0 0 20px'>" + examTitle + "</h2>" +
               "<div style='display:inline-block;background:" + color + ";color:#fff;padding:16px 40px;border-radius:8px;font-size:28px;font-weight:700'>" +
               scoreStr + "</div>" +
               "<p style='font-size:20px;color:" + color + ";font-weight:700;margin:16px 0 0'>" + icon + " " + statusMsg + "</p></div>" +
               "<p style='color:#777;font-size:14px'>Si tiene alguna consulta sobre su resultado, comuníquese con su docente.</p>" +
               "</div>" +
               "<div style='background:#ecf0f1;padding:20px;text-align:center'>" +
               "<p style='color:#999;font-size:12px;margin:0'>Este es un mensaje automático del Sistema de Evaluaciones. Por favor no responda este correo.</p>" +
               "</div></div></body></html>";
    }
}
