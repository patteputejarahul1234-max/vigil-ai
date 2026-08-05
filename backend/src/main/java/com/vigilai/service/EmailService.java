package com.vigilai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sends transactional emails. Wrapped so it fails soft in local/dev
 * (no SMTP creds configured) instead of blocking registration/reset flows.
 * @Async so a slow/unreachable SMTP server never delays the HTTP response —
 * the caller (AuthService) returns as soon as the DB write is done.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String frontendUrl;

    public EmailService(JavaMailSender mailSender, @Value("${app.frontend-url}") String frontendUrl) {
        this.mailSender = mailSender;
        this.frontendUrl = frontendUrl;
    }

    @Async
    public void sendVerificationEmail(String toEmail, String token) {
        String link = frontendUrl + "/verify-email?token=" + token;
        send(toEmail, "Verify your Vigil AI account",
                "Welcome to Vigil AI! Verify your email by visiting:\n" + link +
                        "\n\nThis link expires in 24 hours.");
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String token) {
        String link = frontendUrl + "/reset-password?token=" + token;
        send(toEmail, "Reset your Vigil AI password",
                "We received a request to reset your password. Visit:\n" + link +
                        "\n\nThis link expires in 30 minutes. If you didn't request this, ignore this email.");
    }

    @Async
    public void sendEscalationAlert(String toEmail, String taskName, String userName) {
        send(toEmail, "Vigil AI alert: " + userName + " missed a task",
                userName + " hasn't completed \"" + taskName + "\" and could use a nudge.");
    }

    private void send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception ex) {
            // Don't let a misconfigured/local SMTP setup break registration or reset flows.
            log.warn("Could not send email to {}: {}", to, ex.getMessage());
        }
    }
}
