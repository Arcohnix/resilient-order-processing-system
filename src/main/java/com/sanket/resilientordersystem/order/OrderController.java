package com.sanket.resilientordersystem.order;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestParam String productId,
            @RequestParam Integer quantity
    ) {
        Order order = orderService.createOrder(
                idempotencyKey,
                productId,
                quantity
        );

        return ResponseEntity.ok(order);
    }
}