package com.sanket.resilientordersystem.inventory;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryRepository inventoryRepository;

    public InventoryController(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Inventory> getInventory(
            @PathVariable String productId
    ) {
        return inventoryRepository
                .findByProductId(productId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Inventory> createInventory(
            @RequestParam String productId,
            @RequestParam Integer quantity
    ) {
        Inventory inventory = inventoryRepository
                .findByProductId(productId)
                .orElse(new Inventory(productId, quantity));

        inventory.setQuantity(quantity);

        return ResponseEntity.ok(
                inventoryRepository.save(inventory)
        );
    }
}