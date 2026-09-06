package com.shop.shop.service;

import com.shop.shop.entity.Order;
import com.shop.shop.entity.OrderItem;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.admin-email:krishnansopi@gmail.com}")
    private String adminEmail;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${resend.api.key:${RESEND_API_KEY:}}")
    private String resendApiKey;

    @Value("${brevo.api.key:${BREVO_API_KEY:}}")
    private String brevoApiKey;

    private final java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @Autowired
    public EmailService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendNewOrderAdminAlert(Order order) {
        if (!mailEnabled) return;

        String subject = "💎 New Order Alert: #" + order.getOrderNumber() + " - ₹" + order.getTotalAmount();
        String htmlContent = buildAdminOrderAlertHtml(order);

        sendEmail(adminEmail, subject, htmlContent, "Admin Alert #" + order.getOrderNumber());
    }

    @Async
    public void sendOrderConfirmationCustomer(Order order) {
        if (!mailEnabled || order.getUser() == null || order.getUser().getEmail() == null || order.getUser().getEmail().isBlank()) {
            return;
        }

        String customerEmail = order.getUser().getEmail();
        String subject = "✨ Order Confirmed: #" + order.getOrderNumber() + " - Royal Jewellery";
        String htmlContent = buildCustomerOrderReceiptHtml(order);

        sendEmail(customerEmail, subject, htmlContent, "Customer Receipt #" + order.getOrderNumber());
    }

    private void sendEmail(String toEmail, String subject, String htmlContent, String logContext) {
        // Priority 1: Resend HTTP API (Port 443 - Never blocked on Render)
        if (resendApiKey != null && !resendApiKey.trim().isEmpty()) {
            try {
                String payload = objectMapper.writeValueAsString(java.util.Map.of(
                        "from", "Royal Jewellery <onboarding@resend.dev>",
                        "to", java.util.List.of(toEmail),
                        "subject", subject,
                        "html", htmlContent
                ));

                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create("https://api.resend.com/emails"))
                        .header("Authorization", "Bearer " + resendApiKey.trim())
                        .header("Content-Type", "application/json")
                        .timeout(java.time.Duration.ofSeconds(10))
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString(payload))
                        .build();

                java.net.http.HttpResponse<String> response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    logger.info("[EmailService] Email dispatched successfully via Resend API to {} ({})", toEmail, logContext);
                    return;
                } else {
                    logger.warn("[EmailService] Resend API returned status {}: {}. Falling back to SMTP...", response.statusCode(), response.body());
                }
            } catch (Exception e) {
                logger.error("[EmailService] Resend API dispatch error: {}. Falling back to SMTP...", e.getMessage());
            }
        }

        // Priority 2: Brevo HTTP API (Port 443 - Never blocked on Render)
        if (brevoApiKey != null && !brevoApiKey.trim().isEmpty()) {
            try {
                String payload = objectMapper.writeValueAsString(java.util.Map.of(
                        "sender", java.util.Map.of("name", "Royal Jewellery Store", "email", (mailUsername != null && !mailUsername.isBlank()) ? mailUsername : "noreply@royaljewellery.com"),
                        "to", java.util.List.of(java.util.Map.of("email", toEmail)),
                        "subject", subject,
                        "htmlContent", htmlContent
                ));

                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create("https://api.brevo.com/v3/smtp/email"))
                        .header("api-key", brevoApiKey.trim())
                        .header("Content-Type", "application/json")
                        .timeout(java.time.Duration.ofSeconds(10))
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString(payload))
                        .build();

                java.net.http.HttpResponse<String> response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    logger.info("[EmailService] Email dispatched successfully via Brevo API to {} ({})", toEmail, logContext);
                    return;
                } else {
                    logger.warn("[EmailService] Brevo API returned status {}: {}. Falling back to SMTP...", response.statusCode(), response.body());
                }
            } catch (Exception e) {
                logger.error("[EmailService] Brevo API dispatch error: {}. Falling back to SMTP...", e.getMessage());
            }
        }

        // Priority 3: Fallback to Standard SMTP
        if (mailSender != null && mailUsername != null && !mailUsername.trim().isEmpty()) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                String fromAddress = (mailUsername != null && !mailUsername.isBlank()) ? mailUsername : "noreply@royaljewellery.com";
                helper.setFrom(fromAddress, "Royal Jewellery Store");
                helper.setTo(toEmail);
                helper.setSubject(subject);
                helper.setText(htmlContent, true);

                mailSender.send(message);
                logger.info("[EmailService] Email successfully dispatched via SMTP to {} ({})", toEmail, logContext);
            } catch (Exception e) {
                logger.error("[EmailService] Failed to send email via SMTP to {}: {}", toEmail, e.getMessage());
            }
        } else {
            logger.info("[EmailService] No email provider (Resend API / Brevo API / SMTP) configured for dispatching to {}.", toEmail);
        }
    }

    public boolean isMailConfigured() {
        return mailEnabled && (
                (resendApiKey != null && !resendApiKey.trim().isEmpty()) ||
                (brevoApiKey != null && !brevoApiKey.trim().isEmpty()) ||
                (mailSender != null && mailUsername != null && !mailUsername.trim().isEmpty())
        );
    }

    private String buildAdminOrderAlertHtml(Order order) {
        StringBuilder itemsHtml = new StringBuilder();
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                itemsHtml.append("<tr>")
                        .append("<td style='padding: 10px 12px; border-bottom: 1px solid #e2e8f0; font-size: 14px; color: #1e293b;'>")
                        .append(escapeHtml(item.getProductName()))
                        .append("</td>")
                        .append("<td style='padding: 10px 12px; border-bottom: 1px solid #e2e8f0; font-size: 14px; text-align: center; color: #475569;'>")
                        .append(item.getQuantity())
                        .append("</td>")
                        .append("<td style='padding: 10px 12px; border-bottom: 1px solid #e2e8f0; font-size: 14px; text-align: right; color: #475569;'>₹")
                        .append(item.getProductPrice())
                        .append("</td>")
                        .append("<td style='padding: 10px 12px; border-bottom: 1px solid #e2e8f0; font-size: 14px; font-weight: 600; text-align: right; color: #0f172a;'>₹")
                        .append(item.getSubtotal())
                        .append("</td>")
                        .append("</tr>");
            }
        }

        String recipient = order.getRecipientName() != null ? order.getRecipientName() : (order.getUser() != null ? order.getUser().getFullName() : "Valued Customer");
        String phone = order.getRecipientPhone() != null ? order.getRecipientPhone() : (order.getUser() != null ? order.getUser().getPhone() : "N/A");
        String customerEmail = order.getUser() != null ? order.getUser().getEmail() : "N/A";
        String dateStr = order.getCreatedAt() != null ? order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")) : "Just now";

        return "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 24px; color: #334155;'>" +
                "<table align='center' border='0' cellpadding='0' cellspacing='0' width='100%' style='max-width: 600px; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.08); border: 1px solid #e2e8f0;'>" +
                "<tr><td style='background: linear-gradient(135deg, #1e1b4b, #312e81); padding: 24px; text-align: center; color: #ffffff;'>" +
                "<h1 style='margin: 0; font-size: 24px; font-weight: 700; letter-spacing: 0.5px; color: #facc15;'>💎 ROYAL JEWELLERY</h1>" +
                "<p style='margin: 6px 0 0 0; font-size: 14px; color: #c7d2fe;'>Store Owner & Admin Order Notification</p>" +
                "</td></tr>" +
                "<tr><td style='padding: 24px;'>" +
                "<div style='background-color: #ecfdf5; border-left: 4px solid #10b981; padding: 12px 16px; border-radius: 6px; margin-bottom: 20px;'>" +
                "<strong style='color: #065f46; font-size: 15px;'>🎉 New Order Received!</strong>" +
                "<p style='margin: 4px 0 0 0; font-size: 13px; color: #047857;'>A new customer has just placed an order on your online store.</p>" +
                "</div>" +
                "<table width='100%' style='border-collapse: collapse; margin-bottom: 20px;'>" +
                "<tr><td style='padding: 6px 0; font-size: 13px; color: #64748b;'>Order ID:</td><td style='padding: 6px 0; font-size: 14px; font-weight: bold; color: #0f172a; text-align: right;'>#" + escapeHtml(order.getOrderNumber()) + "</td></tr>" +
                "<tr><td style='padding: 6px 0; font-size: 13px; color: #64748b;'>Order Date:</td><td style='padding: 6px 0; font-size: 13px; color: #1e293b; text-align: right;'>" + dateStr + "</td></tr>" +
                "<tr><td style='padding: 6px 0; font-size: 13px; color: #64748b;'>Customer Name:</td><td style='padding: 6px 0; font-size: 13px; font-weight: 600; color: #1e293b; text-align: right;'>" + escapeHtml(recipient) + "</td></tr>" +
                "<tr><td style='padding: 6px 0; font-size: 13px; color: #64748b;'>Customer Phone:</td><td style='padding: 6px 0; font-size: 13px; color: #1e293b; text-align: right;'>" + escapeHtml(phone) + "</td></tr>" +
                "<tr><td style='padding: 6px 0; font-size: 13px; color: #64748b;'>Customer Email:</td><td style='padding: 6px 0; font-size: 13px; color: #1e293b; text-align: right;'>" + escapeHtml(customerEmail) + "</td></tr>" +
                "<tr><td style='padding: 6px 0; font-size: 13px; color: #64748b;'>Payment Method:</td><td style='padding: 6px 0; font-size: 13px; color: #1e293b; text-align: right;'>" + escapeHtml(order.getPaymentMethod()) + "</td></tr>" +
                "<tr><td style='padding: 6px 0; font-size: 13px; color: #64748b; vertical-align: top;'>Delivery Address:</td><td style='padding: 6px 0; font-size: 13px; color: #1e293b; text-align: right; max-width: 300px;'>" + escapeHtml(order.getDeliveryAddress()) + "</td></tr>" +
                "</table>" +
                "<h3 style='margin: 20px 0 10px 0; font-size: 15px; color: #0f172a; border-bottom: 2px solid #f1f5f9; padding-bottom: 6px;'>Order Summary</h3>" +
                "<table width='100%' style='border-collapse: collapse; margin-bottom: 20px;'>" +
                "<thead><tr style='background-color: #f1f5f9;'>" +
                "<th style='padding: 8px 12px; text-align: left; font-size: 12px; color: #475569; text-transform: uppercase;'>Item</th>" +
                "<th style='padding: 8px 12px; text-align: center; font-size: 12px; color: #475569; text-transform: uppercase;'>Qty</th>" +
                "<th style='padding: 8px 12px; text-align: right; font-size: 12px; color: #475569; text-transform: uppercase;'>Price</th>" +
                "<th style='padding: 8px 12px; text-align: right; font-size: 12px; color: #475569; text-transform: uppercase;'>Subtotal</th>" +
                "</tr></thead>" +
                "<tbody>" + itemsHtml + "</tbody>" +
                "<tfoot><tr>" +
                "<td colspan='3' style='padding: 12px; font-size: 15px; font-weight: bold; text-align: right; color: #0f172a;'>Total Amount:</td>" +
                "<td style='padding: 12px; font-size: 16px; font-weight: bold; text-align: right; color: #b45309;'>₹" + order.getTotalAmount() + "</td>" +
                "</tr></tfoot>" +
                "</table>" +
                "<div style='text-align: center; margin-top: 28px;'>" +
                "<a href='http://localhost:8080/index.html' style='display: inline-block; background-color: #4f46e5; color: #ffffff; text-decoration: none; font-weight: 600; font-size: 14px; padding: 12px 24px; border-radius: 8px;'>Open Admin Dashboard</a>" +
                "</div>" +
                "</td></tr>" +
                "<tr><td style='background-color: #f8fafc; padding: 16px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0;'>" +
                "Royal Jewellery Automated Store Management System" +
                "</td></tr>" +
                "</table></body></html>";
    }

    private String buildCustomerOrderReceiptHtml(Order order) {
        StringBuilder itemsHtml = new StringBuilder();
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                itemsHtml.append("<tr>")
                        .append("<td style='padding: 10px 12px; border-bottom: 1px solid #e2e8f0; font-size: 14px; color: #1e293b;'>")
                        .append(escapeHtml(item.getProductName()))
                        .append("</td>")
                        .append("<td style='padding: 10px 12px; border-bottom: 1px solid #e2e8f0; font-size: 14px; text-align: center; color: #475569;'>")
                        .append(item.getQuantity())
                        .append("</td>")
                        .append("<td style='padding: 10px 12px; border-bottom: 1px solid #e2e8f0; font-size: 14px; text-align: right; color: #475569;'>₹")
                        .append(item.getProductPrice())
                        .append("</td>")
                        .append("<td style='padding: 10px 12px; border-bottom: 1px solid #e2e8f0; font-size: 14px; font-weight: 600; text-align: right; color: #0f172a;'>₹")
                        .append(item.getSubtotal())
                        .append("</td>")
                        .append("</tr>");
            }
        }

        String recipient = order.getRecipientName() != null ? order.getRecipientName() : (order.getUser() != null ? order.getUser().getFullName() : "Valued Customer");

        return "<!DOCTYPE html>" +
                "<html><head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 24px; color: #334155;'>" +
                "<table align='center' border='0' cellpadding='0' cellspacing='0' width='100%' style='max-width: 600px; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.08); border: 1px solid #e2e8f0;'>" +
                "<tr><td style='background: linear-gradient(135deg, #1e1b4b, #312e81); padding: 24px; text-align: center; color: #ffffff;'>" +
                "<h1 style='margin: 0; font-size: 24px; font-weight: 700; letter-spacing: 0.5px; color: #facc15;'>💎 ROYAL JEWELLERY</h1>" +
                "<p style='margin: 6px 0 0 0; font-size: 14px; color: #c7d2fe;'>Order Confirmation & Receipt</p>" +
                "</td></tr>" +
                "<tr><td style='padding: 24px;'>" +
                "<p style='font-size: 15px; margin: 0 0 16px 0;'>Dear <strong>" + escapeHtml(recipient) + "</strong>,</p>" +
                "<p style='font-size: 14px; line-height: 1.6; color: #475569;'>Thank you for your order! We have received your order <strong>#" + escapeHtml(order.getOrderNumber()) + "</strong> and our team is preparing it with care.</p>" +
                "<h3 style='margin: 20px 0 10px 0; font-size: 15px; color: #0f172a; border-bottom: 2px solid #f1f5f9; padding-bottom: 6px;'>Order Summary</h3>" +
                "<table width='100%' style='border-collapse: collapse; margin-bottom: 20px;'>" +
                "<thead><tr style='background-color: #f1f5f9;'>" +
                "<th style='padding: 8px 12px; text-align: left; font-size: 12px; color: #475569; text-transform: uppercase;'>Item</th>" +
                "<th style='padding: 8px 12px; text-align: center; font-size: 12px; color: #475569; text-transform: uppercase;'>Qty</th>" +
                "<th style='padding: 8px 12px; text-align: right; font-size: 12px; color: #475569; text-transform: uppercase;'>Price</th>" +
                "<th style='padding: 8px 12px; text-align: right; font-size: 12px; color: #475569; text-transform: uppercase;'>Subtotal</th>" +
                "</tr></thead>" +
                "<tbody>" + itemsHtml + "</tbody>" +
                "<tfoot><tr>" +
                "<td colspan='3' style='padding: 12px; font-size: 15px; font-weight: bold; text-align: right; color: #0f172a;'>Total Amount:</td>" +
                "<td style='padding: 12px; font-size: 16px; font-weight: bold; text-align: right; color: #b45309;'>₹" + order.getTotalAmount() + "</td>" +
                "</tr></tfoot>" +
                "</table>" +
                "<p style='font-size: 13px; color: #64748b;'><strong>Delivery Address:</strong> " + escapeHtml(order.getDeliveryAddress()) + "</p>" +
                "<p style='font-size: 13px; color: #64748b;'><strong>Payment Method:</strong> " + escapeHtml(order.getPaymentMethod()) + "</p>" +
                "</td></tr>" +
                "<tr><td style='background-color: #f8fafc; padding: 16px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0;'>" +
                "Thank you for choosing Royal Jewellery! If you have questions, please reach out to us." +
                "</td></tr>" +
                "</table></body></html>";
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    // Setters for unit testing
    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    public void setMailEnabled(boolean mailEnabled) {
        this.mailEnabled = mailEnabled;
    }

    public void setMailUsername(String mailUsername) {
        this.mailUsername = mailUsername;
    }
}
