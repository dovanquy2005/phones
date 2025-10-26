package com.fonestore.staff_api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "staff", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
@Getter @Setter
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "staff_id")
    private Long staffId;

    @Column(nullable = false, length = 160)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false, length = 16)
    private String role;              // "manager" | "staff" (lowercase)

    @Column(length = 80)
    private String position;

    @Column(length = 20)
    private String phone;             // mới thêm

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;  // mới thêm

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Getter theo JavaBean cho boolean:
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }
}
