package com.foodwise.inventory.repository;

import com.foodwise.inventory.model.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    Page<NotificationLog> findByCustomerEmailOrderBySentAtDesc(String email, Pageable pageable);
}
