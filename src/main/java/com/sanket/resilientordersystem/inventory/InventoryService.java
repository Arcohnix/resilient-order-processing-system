package com.sanket.resilientordersystem.inventory;

import com.sanket.resilientordersystem.observability.InventoryMetrics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryMetrics inventoryMetrics;

    public InventoryService(
            InventoryRepository inventoryRepository,
            InventoryMetrics inventoryMetrics
    ) {
        this.inventoryRepository = inventoryRepository;
        this.inventoryMetrics = inventoryMetrics;
    }

    @Transactional
    public void reserve(String productId, int quantity) {

        Inventory inventory = inventoryRepository
                .findByProductIdForUpdate(productId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Product not found: " + productId
                        )
                );

        if (inventory.getQuantity() < quantity) {
            inventoryMetrics.reservationRejected();

            throw new IllegalStateException(
                    "Insufficient inventory for product: " + productId
            );
        }

        inventory.setQuantity(
                inventory.getQuantity() - quantity
        );

        inventoryRepository.save(inventory);

        inventoryMetrics.reservationSuccessful();
    }

    @Transactional
    public void release(String productId, int quantity) {

        Inventory inventory = inventoryRepository
                .findByProductIdForUpdate(productId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Product not found: " + productId
                        )
                );

        inventory.setQuantity(
                inventory.getQuantity() + quantity
        );

        inventoryRepository.save(inventory);
    }
}