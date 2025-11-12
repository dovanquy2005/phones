package com.fonestore.user_api.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "voucher_usages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VoucherUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usage_id")
    private Long id;

    @Column(name = "voucher_id", nullable = false)
    private Long voucherId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "order_id")
    private Long orderId;

    /**
     * Dùng Instant cho kiểu timestamp. Nếu DB dùng datetime (SQL Server), JPA sẽ map OK.
     * Ghi giá trị mặc định lúc tạo entity trong code (Instant.now()).
     */
    @Column(name = "used_at", nullable = false)
    private Instant usedAt = Instant.now();
}
