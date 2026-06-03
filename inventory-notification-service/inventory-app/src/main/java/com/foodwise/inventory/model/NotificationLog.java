package com.foodwise.inventory.model;

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
    @JoinColumn(name = "alert_id", nullable = false)
    private SurplusAlert alert;

    @Column(name = "customer_email", nullable = false, length = 255)
    private String customerEmail;

    @Column(nullable = false, length = 512)
    private String subject;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "SENT";

    @Column(name = "sent_at", nullable = false)
    @Builder.Default
    private Instant sentAt = Instant.now();

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
