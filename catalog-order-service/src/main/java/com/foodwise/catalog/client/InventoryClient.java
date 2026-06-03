package com.foodwise.catalog.client;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

/**
 * Client for calling inventory-service internal endpoints.
 * Uses INTERNAL_SERVICE_TOKEN for authentication.
 */
@Slf4j
@Component
public class InventoryClient {

    private final WebClient webClient;
    private final String internalToken;

    public InventoryClient(
            WebClient.Builder webClientBuilder,
            @Value("${app.services.inventory-url}") String inventoryUrl,
            @Value("${app.services.internal-token}") String internalToken) {
        this.webClient = webClientBuilder.baseUrl(inventoryUrl).build();
        this.internalToken = internalToken;
    }

    /**
     * Deducts stock after an order is placed.
     * Non-fatal — logs warning if inventory service is unavailable.
     * The order has already been saved; inventory will need manual reconciliation.
     */
    public boolean deductStock(Long menuItemId, int quantity) {
        try {
            DeductRequest request = new DeductRequest(menuItemId, quantity);
            webClient.post()
                    .uri("/inventory/deduct")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + internalToken)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block(Duration.ofSeconds(5));
            return true;
        } catch (WebClientResponseException e) {
            log.warn("Inventory deduction failed for item {} qty {}: {} {}",
                menuItemId, quantity, e.getStatusCode(), e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Inventory service unreachable for deduction: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Restores stock when an order is cancelled.
     */
    public boolean restoreStock(Long menuItemId, int quantity) {
        try {
            DeductRequest request = new DeductRequest(menuItemId, quantity);
            webClient.post()
                    .uri("/inventory/restore")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + internalToken)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block(Duration.ofSeconds(5));
            return true;
        } catch (Exception e) {
            log.error("Inventory restore failed for item {} qty {}: {}", menuItemId, quantity, e.getMessage());
            return false;
        }
    }

    @Data
    private static class DeductRequest {
        private final Long menuItemId;
        private final int quantity;
    }
}
