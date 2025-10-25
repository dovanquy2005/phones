package com.fonestore.user_api.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Entity
@Data
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "status")
    private String status;

    @Column(name = "subtotal", precision = 18, scale = 0)
    private BigDecimal subtotal;

    @Column(name = "discount", precision = 18, scale = 0)
    private BigDecimal discount;

    @Column(name = "shipping_fee", precision = 18, scale = 0)
    private BigDecimal shippingFee;

    @Column(name = "total", precision = 18, scale = 0)
    private BigDecimal total;

    @Column(name = "address_snapshot", columnDefinition = "nvarchar(max)")
    private String addressSnapshot;

    @Column(name = "note")
    private String note;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items;

    /* ======= AUTO TIMESTAMP ======= */
    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /* ======= ORDER CODE ======= */
    @Transient
    public String getCode() {
        // Sinh mã dạng OD-000007 (6 chữ số)
        if (this.id == null) return "OD-000000";
        return String.format("OD-%06d", this.id);
    }
}
