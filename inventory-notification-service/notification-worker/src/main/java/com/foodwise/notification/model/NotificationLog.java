package com.foodwise.notification.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "notification_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id")
    private SurplusAlert alert;

    @Column(name = "customer_email")
    private String customerEmail;

    private String subject;

    @Builder.Default
    private String status = "SENT";

    @Column(name = "sent_at")
    @Builder.Default
    private Instant sentAt = Instant.now();

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
