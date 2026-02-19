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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

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
        String formattedDate = formatDateTime(dateTime);
        String subject = "Appointment Requested - HealSync";
        String body = String.format(
                """
                        <p>Dear <strong>%s</strong>,</p>
                        <p>Your appointment request has been received and is pending confirmation.</p>

                        <div style="background-color: #f3f4f6; padding: 15px; border-radius: 8px; margin: 20px 0;">
                            <p style="margin: 5px 0;"><strong>Doctor:</strong> %s</p>
                            <p style="margin: 5px 0;"><strong>Date & Time:</strong> %s</p>
                            <p style="margin: 5px 0;"><strong>Status:</strong> <span style="color: #ea580c; font-weight: bold;">PENDING</span></p>
                        </div>

                        <p>You will receive another email once the doctor confirms the appointment.</p>
                        """,
                patientName, ensureDoctorTitle(doctorName), formattedDate);

        sendHtmlEmail(to, subject, createEmailTemplate("Appointment Requested", body));
    }

    @Async
    public void sendAppointmentConfirmed(String to, String patientName, String doctorName, String dateTime) {
        String formattedDate = formatDateTime(dateTime);
        String subject = "Appointment Confirmed - HealSync";
        String body = String.format(
                """
                        <p>Dear <strong>%s</strong>,</p>
                        <p>Good news! Your appointment has been officially confirmed.</p>

                        <div style="background-color: #ecfdf5; border: 1px solid #d1fae5; padding: 15px; border-radius: 8px; margin: 20px 0;">
                            <p style="margin: 5px 0; color: #065f46;"><strong>Doctor:</strong> %s</p>
                            <p style="margin: 5px 0; color: #065f46;"><strong>Date & Time:</strong> %s</p>
                            <p style="margin: 5px 0; color: #065f46;"><strong>Status:</strong> <span style="color: #059669; font-weight: bold;">CONFIRMED</span></p>
                        </div>

                        <p>Please arrive <strong>10 minutes early</strong> to complete any necessary check-in procedures.</p>
                        """,
                patientName, ensureDoctorTitle(doctorName), formattedDate);

        sendHtmlEmail(to, subject, createEmailTemplate("Appointment Confirmed", body));
    }

    @Async
    public void sendAppointmentCancelled(String to, String patientName, String doctorName, String dateTime,
            String reason) {
        String formattedDate = formatDateTime(dateTime);
        String subject = "Appointment Cancelled - HealSync";
        String body = String.format(
                """
                        <p>Dear <strong>%s</strong>,</p>
                        <p>We regret to inform you that your appointment has been cancelled.</p>

                        <div style="background-color: #fef2f2; border: 1px solid #fee2e2; padding: 15px; border-radius: 8px; margin: 20px 0;">
                            <p style="margin: 5px 0; color: #991b1b;"><strong>Doctor:</strong> %s</p>
                            <p style="margin: 5px 0; color: #991b1b;"><strong>Date:</strong> %s</p>
                            <p style="margin: 5px 0; color: #991b1b;"><strong>Reason:</strong> %s</p>
                            <p style="margin: 5px 0; color: #991b1b;"><strong>Status:</strong> <span style="color: #dc2626; font-weight: bold;">CANCELLED</span></p>
                        </div>

                        <p>We apologize for any inconvenience. Please visit the portal to reschedule your appointment.</p>
                        """,
                patientName, ensureDoctorTitle(doctorName), formattedDate, reason);

        sendHtmlEmail(to, subject, createEmailTemplate("Appointment Cancelled", body));
    }

    @Async
    public void sendReportUploaded(String to, String patientName, String reportTitle) {
        String subject = "New Medical Report Uploaded - HealSync";
        String body = String.format(
                """
                        <p>Dear <strong>%s</strong>,</p>
                        <p>A new medical report has been uploaded to your profile by your healthcare provider.</p>

                        <div style="background-color: #eff6ff; border: 1px solid #dbeafe; padding: 15px; border-radius: 8px; margin: 20px 0;">
                            <p style="margin: 5px 0; color: #1e40af;"><strong>Report Title:</strong> %s</p>
                            <p style="margin: 5px 0; color: #1e40af;"><strong>Availability:</strong> Ready for Download</p>
                        </div>

                        <p>You can view within your <strong>Medical History</strong> section in the dashboard.</p>
                        """,
                patientName, reportTitle);

        sendHtmlEmail(to, subject, createEmailTemplate("New Report Available", body));
    }

    @Async
    public void sendPrescriptionCreated(String to, String patientName, String doctorName) {
        String subject = "New Prescription Issued - HealSync";
        String body = String.format(
                """
                        <p>Dear <strong>%s</strong>,</p>
                        <p><strong>%s</strong> has issued a new digital prescription for you.</p>

                        <div style="background-color: #f0fdf4; border: 1px solid #dcfce7; padding: 15px; border-radius: 8px; margin: 20px 0;">
                            <p style="margin: 5px 0; color: #166534;"><strong>Type:</strong> Digital Prescription</p>
                            <p style="margin: 5px 0; color: #166534;"><strong>Issued By:</strong> %s</p>
                        </div>

                        <p>Please log in to your dashboard to view the full details and pharmacy instructions.</p>
                        """,
                patientName, ensureDoctorTitle(doctorName), ensureDoctorTitle(doctorName));

        sendHtmlEmail(to, subject, createEmailTemplate("New Prescription", body));
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

    // --- Helpers ---

    private String createEmailTemplate(String title, String bodyContent) {
        return String.format(
                """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <title>%s</title>
                            <style>
                                body { margin: 0; padding: 0; font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; background-color: #f3f4f6; }
                                .container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }
                                .header { background: linear-gradient(135deg, #10B981 0%%, #059669 100%%); color: white; padding: 20px; text-align: center; }
                                .header h1 { margin: 0; font-size: 24px; font-weight: 700; letter-spacing: 1px; }
                                .content { padding: 30px 20px; color: #374151; line-height: 1.6; font-size: 16px; }
                                .footer { background-color: #f9fafb; padding: 20px; text-align: center; color: #6b7280; font-size: 12px; border-top: 1px solid #e5e7eb; }
                                .button { display: inline-block; padding: 10px 20px; background-color: #10B981; color: white; text-decoration: none; border-radius: 5px; margin-top: 20px; font-weight: bold; }
                                @media only screen and (max-width: 600px) {
                                    .container { width: 100%% !important; border-radius: 0; }
                                }
                            </style>
                        </head>
                        <body>
                            <div style="padding: 20px;">
                                <div class="container">
                                    <div class="header">
                                        <h1>🩺 HealSync</h1>
                                    </div>
                                    <div class="content">
                                        <h2 style="color: #111827; margin-top: 0; font-size: 20px;">%s</h2>
                                        %s
                                    </div>
                                    <div class="footer">
                                        <p>&copy; 2026 HealSync Healthcare Platform. All rights reserved.</p>
                                        <p>This is an automated message. Please do not reply directly to this email.</p>
                                    </div>
                                </div>
                            </div>
                        </body>
                        </html>
                        """,
                title, title, bodyContent);
    }

    private String formatDateTime(String dateTimeStr) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' h:mm a", Locale.ENGLISH);
            return dateTime.format(formatter);
        } catch (Exception e) {
            // Fallback if parsing fails
            return dateTimeStr;
        }
    }

    private String ensureDoctorTitle(String name) {
        if (name == null || name.isEmpty() || name.equalsIgnoreCase("Doctor")) {
            return "Doctor"; // Basic Fallback
        }
        if (!name.toLowerCase().startsWith("dr.")) {
            return "Dr. " + name;
        }
        return name;
    }
}
