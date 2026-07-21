package com.buffturf.buffturf_backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends a beautiful HTML booking confirmation email to the user.
     */
    public void sendBookingConfirmation(
            String toEmail,
            String userName,
            String bookingCode,
            String turfName,
            String location,
            String sportType,
            String bookingDate,
            String timeRange,
            double pricePerHour
    ) {
        // Safe check: if placeholder config, skip silently
        if (fromEmail == null || fromEmail.contains("your_gmail") || fromEmail.trim().isEmpty()) {
            System.out.println("⚠️ Gmail not configured. Skipping email notification for booking: " + bookingCode);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "BuffTurf Booking");
            helper.setTo(toEmail);
            helper.setSubject("✅ Booking Confirmed! " + bookingCode + " | " + turfName);

            String verifyUrl  = frontendUrl + "/verify-booking/" + bookingCode;
            String turfsUrl   = frontendUrl + "/turfs";
            String bookingsUrl = frontendUrl + "/my-bookings";

            String htmlBody = buildHtmlEmail(
                    userName, bookingCode, turfName, location,
                    sportType, bookingDate, timeRange,
                    pricePerHour, verifyUrl, turfsUrl, bookingsUrl
            );

            helper.setText(htmlBody, true); // true = isHtml
            mailSender.send(message);
            System.out.println("✅ Booking confirmation email sent to: " + toEmail);

        } catch (MessagingException e) {
            System.err.println("❌ Failed to send booking confirmation email: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Unexpected error while sending email: " + e.getMessage());
        }
    }

    public void sendPasswordResetOtp(String toEmail, String otp) {
        if (fromEmail == null || fromEmail.contains("your_gmail") || fromEmail.trim().isEmpty()) {
            System.out.println("⚠️ Gmail not configured. Skipping OTP email for: " + toEmail);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "BuffTurf Security");
            helper.setTo(toEmail);
            helper.setSubject("🔒 Password Reset OTP");

            String htmlBody = "<!DOCTYPE html>" +
                "<html lang='en'>" +
                "<body style='margin:0;padding:40px 20px;background-color:#0f172a;font-family:Arial,sans-serif;color:#f1f5f9;text-align:center;'>" +
                "  <div style='max-width:500px;margin:0 auto;background:#1e293b;padding:40px;border-radius:16px;border-top:4px solid #3b82f6;'>" +
                "    <h1 style='margin:0 0 20px;font-size:24px;color:#ffffff;'>Password Reset Request</h1>" +
                "    <p style='color:#94a3b8;font-size:15px;margin-bottom:30px;line-height:1.6;'>" +
                "      We received a request to reset your BuffTurf password. Your OTP is below:" +
                "    </p>" +
                "    <div style='background:#0f172a;border:2px dashed #3b82f6;border-radius:12px;padding:24px;margin-bottom:30px;'>" +
                "      <p style='margin:0;color:#3b82f6;font-size:36px;font-weight:700;letter-spacing:6px;font-family:monospace;'>" + otp + "</p>" +
                "    </div>" +
                "    <p style='color:#ef4444;font-size:13px;font-weight:600;margin-bottom:20px;'>" +
                "      ⏳ This code will expire in 15 minutes." +
                "    </p>" +
                "    <p style='color:#64748b;font-size:12px;line-height:1.6;'>" +
                "      If you did not request this, please ignore this email or contact support if you have concerns." +
                "    </p>" +
                "  </div>" +
                "</body>" +
                "</html>";

            helper.setText(htmlBody, true);
            mailSender.send(message);
            System.out.println("✅ Password Reset OTP sent to: " + toEmail);

        } catch (Exception e) {
            System.err.println("❌ Failed to send OTP email: " + e.getMessage());
        }
    }

    private String buildHtmlEmail(
            String userName,
            String bookingCode,
            String turfName,
            String location,
            String sportType,
            String bookingDate,
            String timeRange,
            double pricePerHour,
            String verifyUrl,
            String turfsUrl,
            String bookingsUrl
    ) {
        String sportEmoji = getSportEmoji(sportType);

        return "<!DOCTYPE html>" +
            "<html lang='en'>" +
            "<head>" +
            "  <meta charset='UTF-8'/>" +
            "  <meta name='viewport' content='width=device-width, initial-scale=1.0'/>" +
            "  <title>Booking Confirmed - BuffTurf</title>" +
            "</head>" +
            "<body style='margin:0;padding:0;background-color:#0f172a;font-family:Arial,Helvetica,sans-serif;'>" +
            "  <table width='100%' cellpadding='0' cellspacing='0' style='background-color:#0f172a;padding:40px 20px;'>" +
            "    <tr><td align='center'>" +
            "      <table width='600' cellpadding='0' cellspacing='0' style='max-width:600px;width:100%;'>" +

            // ── HEADER ──
            "        <tr><td style='background:linear-gradient(135deg,#10b981,#059669);border-radius:16px 16px 0 0;padding:40px 32px;text-align:center;'>" +
            "          <div style='font-size:48px;margin-bottom:12px;'>" + sportEmoji + "</div>" +
            "          <h1 style='margin:0;color:#ffffff;font-size:28px;font-weight:700;letter-spacing:-0.5px;'>Booking Confirmed!</h1>" +
            "          <p style='margin:8px 0 0;color:rgba(255,255,255,0.85);font-size:15px;'>Your turf is reserved. Get ready to play!</p>" +
            "        </td></tr>" +

            // ── BODY ──
            "        <tr><td style='background:#1e293b;padding:36px 32px;'>" +
            "          <p style='margin:0 0 24px;color:#94a3b8;font-size:15px;'>Hi <strong style='color:#f1f5f9;'>" + escapeHtml(userName) + "</strong>,</p>" +
            "          <p style='margin:0 0 28px;color:#94a3b8;font-size:15px;line-height:1.6;'>Your booking at <strong style='color:#10b981;'>" + escapeHtml(turfName) + "</strong> has been successfully confirmed. Here are your booking details:</p>" +

            // ── BOOKING CODE BOX ──
            "          <div style='background:#0f172a;border:2px dashed #10b981;border-radius:12px;padding:20px;text-align:center;margin-bottom:28px;'>" +
            "            <p style='margin:0 0 6px;color:#64748b;font-size:12px;letter-spacing:2px;text-transform:uppercase;'>Booking Ticket Code</p>" +
            "            <p style='margin:0;color:#10b981;font-size:26px;font-weight:700;letter-spacing:4px;font-family:monospace;'>" + bookingCode + "</p>" +
            "          </div>" +

            // ── DETAILS TABLE ──
            "          <table width='100%' cellpadding='0' cellspacing='0' style='margin-bottom:28px;'>" +
            detailRow("🏟️", "Turf Name", escapeHtml(turfName)) +
            detailRow("📍", "Location", escapeHtml(location)) +
            detailRow("🎯", "Sport", escapeHtml(sportType)) +
            detailRow("📅", "Date", escapeHtml(bookingDate)) +
            detailRow("⏰", "Time Slot", escapeHtml(timeRange)) +
            detailRow("💰", "Rate", "₹" + pricePerHour + " per hour") +
            "          </table>" +

            // ── ENTRY PASS BUTTON ──
            "          <div style='text-align:center;margin-bottom:28px;'>" +
            "            <a href='" + verifyUrl + "' style='display:inline-block;background:linear-gradient(135deg,#10b981,#059669);color:#ffffff;text-decoration:none;font-size:16px;font-weight:700;padding:16px 40px;border-radius:50px;letter-spacing:0.5px;'>🎟️ View Entry Pass</a>" +
            "          </div>" +
            "          <p style='margin:0 0 28px;color:#64748b;font-size:13px;text-align:center;line-height:1.6;'>Show this entry pass link at the turf for scanning. The QR code on this page is your digital entry ticket.</p>" +

            // ── DIVIDER ──
            "          <hr style='border:none;border-top:1px solid #334155;margin:0 0 24px;'/>" +

            // ── SECONDARY LINKS ──
            "          <table width='100%' cellpadding='0' cellspacing='0'><tr>" +
            "            <td align='center' width='50%'>" +
            "              <a href='" + bookingsUrl + "' style='color:#10b981;text-decoration:none;font-size:14px;font-weight:600;'>📋 My Bookings</a>" +
            "            </td>" +
            "            <td align='center' width='50%'>" +
            "              <a href='" + turfsUrl + "' style='color:#10b981;text-decoration:none;font-size:14px;font-weight:600;'>🔍 Browse More Turfs</a>" +
            "            </td>" +
            "          </tr></table>" +
            "        </td></tr>" +

            // ── FOOTER ──
            "        <tr><td style='background:#0f172a;border-radius:0 0 16px 16px;padding:24px 32px;text-align:center;'>" +
            "          <p style='margin:0 0 8px;color:#10b981;font-size:18px;font-weight:700;'>⚽ BuffTurf</p>" +
            "          <p style='margin:0 0 8px;color:#475569;font-size:12px;'>Book. Play. Win.</p>" +
            "          <p style='margin:0;color:#334155;font-size:11px;'>This is an automated email. Please do not reply to this email.</p>" +
            "        </td></tr>" +

            "      </table>" +
            "    </td></tr>" +
            "  </table>" +
            "</body></html>";
    }

    private String detailRow(String emoji, String label, String value) {
        return "<tr>" +
            "<td style='padding:10px 0;border-bottom:1px solid #1e293b;'>" +
            "  <span style='color:#64748b;font-size:13px;'>" + emoji + " " + label + "</span>" +
            "</td>" +
            "<td style='padding:10px 0;border-bottom:1px solid #1e293b;text-align:right;'>" +
            "  <span style='color:#f1f5f9;font-size:14px;font-weight:600;'>" + value + "</span>" +
            "</td>" +
            "</tr>";
    }

    private String getSportEmoji(String sportType) {
        if (sportType == null) return "🏟️";
        return switch (sportType.toLowerCase()) {
            case "football", "soccer" -> "⚽";
            case "cricket"            -> "🏏";
            case "badminton"          -> "🏸";
            case "tennis"             -> "🎾";
            case "basketball"         -> "🏀";
            case "volleyball"         -> "🏐";
            case "hockey"             -> "🏑";
            default                  -> "🏟️";
        };
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;");
    }
}
