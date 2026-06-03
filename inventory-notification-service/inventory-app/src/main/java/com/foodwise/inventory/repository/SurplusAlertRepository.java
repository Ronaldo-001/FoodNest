package com.foodwise.inventory.repository;

import com.foodwise.inventory.model.SurplusAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SurplusAlertRepository extends JpaRepository<SurplusAlert, Long> {

    List<SurplusAlert> findByRestaurantIdOrderByTriggeredAtDesc(Long restaurantId);

    List<SurplusAlert> findByNotifiedFalseOrderByTriggeredAtAsc();
}
