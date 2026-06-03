package com.foodwise.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class InventoryAppApplication {
    public static void main(String[] args) {
        SpringApplication.run(InventoryAppApplication.class, args);
    }
}
