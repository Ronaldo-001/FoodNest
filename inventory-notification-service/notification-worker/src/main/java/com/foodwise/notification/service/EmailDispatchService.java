package com.foodwise.notification.service;

import com.foodwise.notification.model.NotificationLog;
import com.foodwise.notification.model.NotificationSubscription;
import com.foodwise.notification.model.SurplusAlert;
import com.foodwise.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Dispatches email notifications to subscribers via SMTP.
 * Uses Mailhog in dev (smtp://mailhog:1025) — configure real SMTP for production.
 *
 * SECURITY: Email addresses come from the database (customer subscriptions), not from user input.
 * No template rendering with user-controlled HTML — uses plain text to prevent email injection.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailDispatchService {

    private final JavaMailSender mailSender;
    private final NotificationLogRepository logRepository;

    @Value("${app.mail.from}")
    private String fromAddress;

    public void sendSurplusAlert(SurplusAlert alert, List<NotificationSubscription> subscribers) {
        if (subscribers.isEmpty()) {
            log.debug("No subscribers for restaurant {} — skipping notification", alert.getRestaurantId());
            return;
        }

        String itemName = alert.getInventoryItem() != null ? alert.getInventoryItem().getName() : "Unknown item";
        String subject = "FoodWise Surplus Alert: " + itemName + " available at reduced price!";
        String body = buildEmailBody(alert, itemName);

        for (NotificationSubscription subscriber : subscribers) {
            sendEmailSafely(alert, subscriber.getCustomerEmail(), subject, body);
        }
    }

    private void sendEmailSafely(SurplusAlert alert, String toEmail, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);

            NotificationLog log = NotificationLog.builder()
                    .alert(alert)
                    .customerEmail(toEmail)
                    .subject(subject)
                    .status("SENT")
                    .sentAt(Instant.now())
                    .build();
            logRepository.save(log);
            this.log.debug("Email sent to {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
            NotificationLog failedLog = NotificationLog.builder()
                    .alert(alert)
                    .customerEmail(toEmail)
                    .subject(subject)
                    .status("FAILED")
                    .sentAt(Instant.now())
                    .errorMessage(e.getMessage())
                    .build();
            logRepository.save(failedLog);
        }
    }

    private String buildEmailBody(SurplusAlert alert, String itemName) {
        // SECURITY: plain text email — no HTML/template rendering with user data to prevent injection
        String reason = "EXPIRY_SOON".equals(alert.getReason())
            ? "is expiring soon and available at a special price"
            : "has surplus stock and is available at a special price";

        return String.format(
            "Hello FoodWise Customer!\n\n" +
            "%s %s.\n\n" +
            "Visit the restaurant to grab it before it's gone!\n\n" +
            "This alert was triggered at: %s\n\n" +
            "To unsubscribe from these alerts, log in to FoodWise and manage your subscriptions.\n\n" +
            "Best regards,\nThe FoodWise Team",
            itemName, reason, alert.getTriggeredAt()
        );
    }
}
