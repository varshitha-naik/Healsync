package com.healsync.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Async
    public void sendAppointmentRequested(String to, String patientName, String doctorName, String dateTime) {
        String subject = "Appointment Requested - HealSync";
        String content = String.format(
                """
                        <div style="font-family: Arial, sans-serif; color: #333;">
                            <h2>Appointment Requested</h2>
                            <p>Dear %s,</p>
                            <p>Your appointment request with <strong>%s</strong> on <strong>%s</strong> has been received and is pending confirmation.</p>
                            <p>You will receive another email once the doctor confirms the appointment.</p>
                            <br>
                            <p>Best regards,<br>HealSync Team</p>
                        </div>
                        """,
                patientName, doctorName, dateTime);
        sendHtmlEmail(to, subject, content);
    }

    @Async
    public void sendAppointmentConfirmed(String to, String patientName, String doctorName, String dateTime) {
        String subject = "Appointment Confirmed - HealSync";
        String content = String.format(
                """
                        <div style="font-family: Arial, sans-serif; color: #333;">
                            <h2 style="color: #28a745;">Appointment Confirmed</h2>
                            <p>Dear %s,</p>
                            <p>Your appointment with <strong>%s</strong> on <strong>%s</strong> has been <strong>CONFIRMED</strong>.</p>
                            <p>Please arrive 10 minutes early.</p>
                            <br>
                            <p>Best regards,<br>HealSync Team</p>
                        </div>
                        """,
                patientName, doctorName, dateTime);
        sendHtmlEmail(to, subject, content);
    }

    @Async
    public void sendAppointmentCancelled(String to, String patientName, String doctorName, String dateTime,
            String reason) {
        String subject = "Appointment Cancelled - HealSync";
        String content = String.format(
                """
                        <div style="font-family: Arial, sans-serif; color: #333;">
                            <h2 style="color: #dc3545;">Appointment Cancelled</h2>
                            <p>Dear %s,</p>
                            <p>Your appointment with <strong>%s</strong> on <strong>%s</strong> has been <strong>CANCELLED</strong>.</p>
                            <p><strong>Reason:</strong> %s</p>
                            <p>Please reschedule via the portal.</p>
                            <br>
                            <p>Best regards,<br>HealSync Team</p>
                        </div>
                        """,
                patientName, doctorName, dateTime, reason);
        sendHtmlEmail(to, subject, content);
    }

    @Async
    public void sendReportUploaded(String to, String patientName, String reportTitle) {
        String subject = "New Medical Report Uploaded - HealSync";
        String content = String.format("""
                <div style="font-family: Arial, sans-serif; color: #333;">
                    <h2>New Medical Report Available</h2>
                    <p>Dear %s,</p>
                    <p>A new medical report titled <strong>"%s"</strong> has been uploaded to your profile.</p>
                    <p>You can view it in your Medical History section.</p>
                    <br>
                    <p>Best regards,<br>HealSync Team</p>
                </div>
                """, patientName, reportTitle);
        sendHtmlEmail(to, subject, content);
    }

    @Async
    public void sendPrescriptionCreated(String to, String patientName, String doctorName) {
        String subject = "New Prescription Issued - HealSync";
        String content = String.format("""
                <div style="font-family: Arial, sans-serif; color: #333;">
                    <h2>New Prescription Issued</h2>
                    <p>Dear %s,</p>
                    <p><strong>Dr. %s</strong> has issued a new prescription for you.</p>
                    <p>You can view the details in your dashboard.</p>
                    <br>
                    <p>Best regards,<br>HealSync Team</p>
                </div>
                """, patientName, doctorName);
        sendHtmlEmail(to, subject, content);
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}", to, e);
        }
    }
}
