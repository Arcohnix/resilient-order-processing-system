package com.sanket.resilientordersystem.recovery;

import com.sanket.resilientordersystem.order.Order;
import com.sanket.resilientordersystem.payment.Payment;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recovery")
public class RecoveryController {

    private final RecoveryService recoveryService;

    public RecoveryController(RecoveryService recoveryService) {
        this.recoveryService = recoveryService;
    }

    @GetMapping("/payments")
    public List<Payment> findOrphanedPayments() {
        return recoveryService.findOrphanedPayments();
    }

    @PostMapping("/payments/{orderId}")
    public Order recoverPayment(
            @PathVariable Long orderId
    ) {
        return recoveryService.recoverPayment(orderId);
    }
}