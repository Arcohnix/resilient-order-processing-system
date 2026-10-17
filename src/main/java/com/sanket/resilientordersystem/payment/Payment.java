package com.sanket.resilientordersystem.payment;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "payments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_payment_order_id",
                        columnNames = "orderId"
                )
        }
)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private String productId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, unique = true)
    private String paymentId;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column
    private Long recoveredOrderId;

    private LocalDateTime createdAt;

    public Payment() {
    }

    public Payment(
            Long orderId,
            String productId,
            Integer quantity,
            String paymentId,
            PaymentStatus status
    ) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.paymentId = paymentId;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getProductId() {
        return productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Long getRecoveredOrderId() {
        return recoveredOrderId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public void setRecoveredOrderId(Long recoveredOrderId) {
        this.recoveredOrderId = recoveredOrderId;
    }
}