package com.fonestore.staff_api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
// Voucher.java
@Entity
@Getter @Setter
@Table(name = "vouchers")
public class Voucher {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "voucher_id")
    private Long id;

    @Column(name = "code",unique = true, nullable = false)
    private String code;

    @Column(name = "code_norm", insertable = false, updatable = false)
    private String codeNorm;


    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "value", nullable = false, precision = 18, scale = 2)
    private BigDecimal value;

    @Column(name = "min_order", nullable = false)  // nếu DB NOT NULL
    private BigDecimal minOrder;

    @Column(name = "usage_limit", nullable = false) // nếu DB NOT NULL
    private Integer usageLimit;

    @Column(name = "per_user_limit", nullable = false) // nếu DB NOT NULL
    private Integer perUserLimit;

    @Column(name = "starts_at")
    private LocalDateTime startsAt;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist @PreUpdate
    private void normalize() {
        if (code != null) {
            String c = code.trim();
            this.code = c;
            this.codeNorm = c.toUpperCase(Locale.ROOT); // ✨ luôn fill code_norm
        }
        if (type != null) type = type.trim().toLowerCase(Locale.ROOT);
        if (minOrder == null) minOrder = BigDecimal.ZERO;     // nếu DB NOT NULL
        if (usageLimit == null) usageLimit = 0;               // nếu DB NOT NULL
        if (perUserLimit == null) perUserLimit = 0;           // nếu DB NOT NULL
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
