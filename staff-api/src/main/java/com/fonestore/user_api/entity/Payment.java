package com.fonestore.user_api.entity;

import com.fonestore.staff_api.entity.converter.PaymentStatusConverter;
import com.fonestore.staff_api.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "payments", indexes = {
    @Index(name = "ix_payments_order", columnList = "order_id", unique = true)
})
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    // NOTE: nên là BIGINT trong DB
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "method", length = 30, nullable = false)
    private String method; // "cod" | "bank" | ...

    @Column(name = "amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal amount;

    // DB lưu 'pending' / 'paid' → map sang UNPAID / PAID
    @Convert(converter = PaymentStatusConverter.class)
    @Column(name = "status", length = 20, nullable = false)
    private PaymentStatus status;

    @Column(name = "txn_ref", length = 64)
    private String txnRef;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate(){
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null)    status = PaymentStatus.UNPAID;
        if (method == null)    method = "cod";
        if (amount == null)    amount = BigDecimal.ZERO;
    }
}
