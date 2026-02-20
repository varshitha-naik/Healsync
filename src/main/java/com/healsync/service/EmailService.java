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
                        <p>Your appointment request has been received and is pending confirmation from the doctor.</p>

                        <!-- Appointment Details Card -->
                        <div style="background-color: #ffffff; border: 1px solid #e5e7eb; border-radius: 8px; padding: 20px; margin: 25px 0; box-shadow: 0 2px 4px rgba(0,0,0,0.05);">
                            <h3 style="margin-top: 0; color: #111827; border-bottom: 2px solid #10B981; padding-bottom: 10px; display: inline-block;">Appointment Details</h3>
                            <table style="width: 100%%; border-collapse: collapse; margin-top: 10px;">
                                <tr>
                                    <td style="padding: 8px 0; color: #6b7280; font-weight: bold; width: 120px;">Doctor:</td>
                                    <td style="padding: 8px 0; color: #111827;">%s</td>
                                </tr>
                                <tr>
                                    <td style="padding: 8px 0; color: #6b7280; font-weight: bold;">Date & Time:</td>
                                    <td style="padding: 8px 0; color: #111827;">%s</td>
                                </tr>
                                <tr>
                                    <td style="padding: 8px 0; color: #6b7280; font-weight: bold;">Status:</td>
                                    <td style="padding: 8px 0;"><span style="background-color: #fff7ed; color: #c2410c; padding: 4px 10px; border-radius: 12px; font-weight: bold; font-size: 12px; border: 1px solid #ffedd5;">PENDING</span></td>
                                </tr>
                            </table>
                        </div>

                        <!-- Professional Guidance -->
                        <div style="background-color: #f9fafb; padding: 15px; border-radius: 6px; margin-bottom: 25px; border-left: 4px solid #10B981;">
                            <p style="margin: 0 0 10px 0; font-weight: bold; color: #374151;">What to expect:</p>
                            <ul style="margin: 0; padding-left: 20px; color: #4b5563;">
                                <li style="margin-bottom: 5px;">You will be notified once the doctor confirms.</li>
                                <li style="margin-bottom: 5px;">Please verify your contact details in the dashboard.</li>
                            </ul>
                        </div>

                        <div style="text-align: center; margin: 30px 0;">
                            <a href="http://localhost:8080/patient/appointments" style="background-color: #10B981; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold; font-size: 16px; display: inline-block;">View My Appointments</a>
                        </div>
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

                        <!-- Appointment Details Card -->
                        <div style="background-color: #ffffff; border: 1px solid #d1fae5; border-radius: 8px; padding: 20px; margin: 25px 0; box-shadow: 0 2px 4px rgba(0,0,0,0.05);">
                            <h3 style="margin-top: 0; color: #111827; border-bottom: 2px solid #059669; padding-bottom: 10px; display: inline-block;">Appointment Details</h3>
                            <table style="width: 100%%; border-collapse: collapse; margin-top: 10px;">
                                <tr>
                                    <td style="padding: 8px 0; color: #6b7280; font-weight: bold; width: 120px;">Doctor:</td>
                                    <td style="padding: 8px 0; color: #111827;">%s</td>
                                </tr>
                                <tr>
                                    <td style="padding: 8px 0; color: #6b7280; font-weight: bold;">Date & Time:</td>
                                    <td style="padding: 8px 0; color: #111827;">%s</td>
                                </tr>
                                <tr>
                                    <td style="padding: 8px 0; color: #6b7280; font-weight: bold;">Status:</td>
                                    <td style="padding: 8px 0;"><span style="background-color: #ecfdf5; color: #047857; padding: 4px 10px; border-radius: 12px; font-weight: bold; font-size: 12px; border: 1px solid #a7f3d0;">CONFIRMED</span></td>
                                </tr>
                            </table>
                        </div>

                        <!-- Professional Guidance -->
                        <div style="background-color: #f0fdf4; padding: 15px; border-radius: 6px; margin-bottom: 25px; border-left: 4px solid #059669;">
                            <p style="margin: 0 0 10px 0; font-weight: bold; color: #065f46;">What to expect:</p>
                            <ul style="margin: 0; padding-left: 20px; color: #1f2937;">
                                <li style="margin-bottom: 5px;">Please arrive <strong>10 minutes early</strong>.</li>
                                <li style="margin-bottom: 5px;">Bring previous medical reports if available.</li>
                                <li style="margin-bottom: 5px;">Contact the clinic if rescheduling is required.</li>
                            </ul>
                        </div>

                        <div style="text-align: center; margin: 30px 0;">
                            <a href="http://localhost:8080/patient/appointments" style="background-color: #059669; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold; font-size: 16px; display: inline-block;">View Appointment</a>
                        </div>
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

                        <!-- Appointment Details Card -->
                        <div style="background-color: #ffffff; border: 1px solid #fee2e2; border-radius: 8px; padding: 20px; margin: 25px 0; box-shadow: 0 2px 4px rgba(0,0,0,0.05);">
                            <h3 style="margin-top: 0; color: #111827; border-bottom: 2px solid #dc2626; padding-bottom: 10px; display: inline-block;">Cancellation Details</h3>
                            <table style="width: 100%%; border-collapse: collapse; margin-top: 10px;">
                                <tr>
                                    <td style="padding: 8px 0; color: #6b7280; font-weight: bold; width: 120px;">Doctor:</td>
                                    <td style="padding: 8px 0; color: #111827;">%s</td>
                                </tr>
                                <tr>
                                    <td style="padding: 8px 0; color: #6b7280; font-weight: bold;">Original Date:</td>
                                    <td style="padding: 8px 0; color: #111827;">%s</td>
                                </tr>
                                <tr>
                                    <td style="padding: 8px 0; color: #6b7280; font-weight: bold;">Reason:</td>
                                    <td style="padding: 8px 0; color: #dc2626;">%s</td>
                                </tr>
                                <tr>
                                    <td style="padding: 8px 0; color: #6b7280; font-weight: bold;">Status:</td>
                                    <td style="padding: 8px 0;"><span style="background-color: #fef2f2; color: #991b1b; padding: 4px 10px; border-radius: 12px; font-weight: bold; font-size: 12px; border: 1px solid #fecaca;">CANCELLED</span></td>
                                </tr>
                            </table>
                        </div>

                        <p>We apologize for any inconvenience. If this cancellation was unexpected, please contact the clinic or book a new appointment.</p>

                        <div style="text-align: center; margin: 30px 0;">
                            <a href="http://localhost:8080/patient/appointments" style="background-color: #4b5563; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold; font-size: 16px; display: inline-block;">Reschedule Appointment</a>
                        </div>
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
                                body { margin: 0; padding: 0; font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; background-color: #f3f4f6; color: #374151; }
                                .container { max-width: 600px; margin: 40px auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05); }
                                .header { background: linear-gradient(135deg, #10B981 0%%, #047857 100%%); color: white; padding: 30px 0; text-align: center; }
                                .header h1 { margin: 0; font-size: 28px; font-weight: 800; letter-spacing: -0.5px; }
                                .content { padding: 40px 30px; line-height: 1.7; font-size: 16px; }
                                .footer { background-color: #f9fafb; padding: 30px; text-align: center; color: #9ca3af; font-size: 13px; border-top: 1px solid #e5e7eb; }
                                .footer p { margin: 5px 0; }
                                @media only screen and (max-width: 600px) {
                                    .container { width: 100%% !important; margin: 0 !important; border-radius: 0; }
                                    .content { padding: 20px; }
                                }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <div class="header">
                                    <h1>🩺 HealSync</h1>
                                </div>
                                <div class="content">
                                    <!-- Dynamic Body -->
                                    %s
                                </div>
                                <div class="footer">
                                    <p>&copy; 2026 HealSync Healthcare Platform. All rights reserved.</p>
                                    <div style="width: 40px; height: 2px; background-color: #e5e7eb; margin: 15px auto;"></div>
                                    <p>This is an automated email — please do not reply.</p>
                                </div>
                            </div>
                        </body>
                        </html>
                        """,
                title, bodyContent);
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
