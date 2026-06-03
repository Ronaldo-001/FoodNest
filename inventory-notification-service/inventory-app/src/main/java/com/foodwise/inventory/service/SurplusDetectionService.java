package com.foodwise.inventory.service;

import com.foodwise.inventory.model.InventoryItem;
import com.foodwise.inventory.model.SurplusAlert;
import com.foodwise.inventory.repository.InventoryItemRepository;
import com.foodwise.inventory.repository.SurplusAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Detects surplus conditions and creates alerts.
 *
 * Surplus logic:
 *  - quantity < threshold  → LOW_STOCK
 *  - expiry_date within configured hours → EXPIRY_SOON
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SurplusDetectionService {

    private final InventoryItemRepository inventoryItemRepository;
    private final SurplusAlertRepository surplusAlertRepository;

    @Value("${app.surplus.expiry-hours:24}")
    private int surplusExpiryHours;

    /**
     * Called after any stock change to immediately detect surplus.
     */
    @Transactional
    public void checkAndMarkSurplus(InventoryItem item) {
        boolean isLowStock = item.getQuantity() < item.getThreshold();
        boolean isExpiringSoon = item.getExpiryDate() != null &&
                !item.getExpiryDate().isAfter(LocalDate.now().plusDays(
                    (long) Math.ceil((double) surplusExpiryHours / 24)));

        boolean wasSurplus = item.isSurplus();
        boolean nowSurplus = isLowStock || isExpiringSoon;

        item.setSurplus(nowSurplus);
        inventoryItemRepository.save(item);

        // Only create alert if newly becoming surplus
        if (nowSurplus && !wasSurplus) {
            String reason = isExpiringSoon ? "EXPIRY_SOON" : "LOW_STOCK";
            SurplusAlert alert = SurplusAlert.builder()
                    .inventoryItem(item)
                    .restaurantId(item.getRestaurantId())
                    .reason(reason)
                    .build();
            surplusAlertRepository.save(alert);
            log.info("Surplus alert created: item={}, reason={}, restaurantId={}",
                item.getId(), reason, item.getRestaurantId());
        }
    }

    /**
     * Scheduled scan of all inventory items for new surplus conditions.
     * Runs every hour to catch expiry transitions that weren't triggered by stock changes.
     */
    @Scheduled(fixedRateString = "${surplus.scan.interval-ms:3600000}")
    @Transactional
    public void scanAllForSurplus() {
        LocalDate expiryThreshold = LocalDate.now().plusDays(
            (long) Math.ceil((double) surplusExpiryHours / 24));
        List<InventoryItem> newlySurplus = inventoryItemRepository.findNewlySurplusItems(expiryThreshold);

        for (InventoryItem item : newlySurplus) {
            checkAndMarkSurplus(item);
        }

        if (!newlySurplus.isEmpty()) {
            log.info("Surplus scan: {} new surplus items detected", newlySurplus.size());
        }
    }
}
