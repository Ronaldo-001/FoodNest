package com.foodwise.notification.repository;

import com.foodwise.notification.model.SurplusAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SurplusAlertRepository extends JpaRepository<SurplusAlert, Long> {
    List<SurplusAlert> findByNotifiedFalseOrderByTriggeredAtAsc();
}
