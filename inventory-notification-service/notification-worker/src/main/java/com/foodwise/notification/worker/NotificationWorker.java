package com.foodwise.notification.worker;

import com.foodwise.notification.model.NotificationSubscription;
import com.foodwise.notification.model.SurplusAlert;
import com.foodwise.notification.repository.NotificationSubscriptionRepository;
import com.foodwise.notification.repository.SurplusAlertRepository;
import com.foodwise.notification.service.EmailDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Polls the surplus_alerts table for unnotified alerts and dispatches emails.
 *
 * Design: polling pattern over message queue for simplicity in dev.
 * In production, consider replacing with a message broker (Kafka/RabbitMQ).
 * TODO: Consider event-driven notification via message broker for production scale.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationWorker {

    private final SurplusAlertRepository surplusAlertRepository;
    private final NotificationSubscriptionRepository subscriptionRepository;
    private final EmailDispatchService emailDispatchService;

    @Scheduled(fixedRateString = "${app.notification.poll-interval-ms:60000}")
    @Transactional
    public void processUnnotifiedAlerts() {
        List<SurplusAlert> pendingAlerts = surplusAlertRepository.findByNotifiedFalseOrderByTriggeredAtAsc();

        if (pendingAlerts.isEmpty()) {
            log.debug("No pending surplus alerts to process");
            return;
        }

        log.info("Processing {} surplus alerts", pendingAlerts.size());

        for (SurplusAlert alert : pendingAlerts) {
            try {
                List<NotificationSubscription> subscribers =
                        subscriptionRepository.findByRestaurantId(alert.getRestaurantId());

                emailDispatchService.sendSurplusAlert(alert, subscribers);

                alert.setNotified(true);
                alert.setNotifiedAt(Instant.now());
                surplusAlertRepository.save(alert);

                log.info("Processed alert {} for restaurant {} — notified {} subscribers",
                    alert.getId(), alert.getRestaurantId(), subscribers.size());

            } catch (Exception e) {
                log.error("Failed to process surplus alert {}: {}", alert.getId(), e.getMessage());
                // Continue processing other alerts — don't fail the whole batch
            }
        }
    }
}
