package com.sanket.resilientordersystem.inventory;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class InventoryInitializer implements CommandLineRunner {

    private final InventoryRepository inventoryRepository;

    public InventoryInitializer(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public void run(String... args) {

        if (inventoryRepository.findByProductId("PROD-001").isEmpty()) {
            inventoryRepository.save(
                    new Inventory("PROD-001", 1)
            );
        }
    }
}