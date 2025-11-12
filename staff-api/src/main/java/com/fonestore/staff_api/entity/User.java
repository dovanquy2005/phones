package com.fonestore.staff_api.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_id")
  private Long id;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(name = "full_name")
  private String fullName;

  private String phone;
  private String gender;
  private java.time.LocalDate dob;

  // Địa chỉ chuỗi ngắn; nếu cần dài hơn thì tăng length hoặc dùng NVARCHAR(MAX) tuỳ DB
  @Column(name = "address", length = 255)
  private String address;

  // >>> THÊM FIELD 2FA
  @Column(name = "twofa_secret", length = 64)
  private String twofaSecret;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  // new role field (mapped to DB column 'role'), default 'user'
  @Builder.Default
  @Column(name = "role", nullable = false)
  private String role = "user";

  @PrePersist void pre() { createdAt = updatedAt = LocalDateTime.now(); }
  @PreUpdate  void upd() { updatedAt = LocalDateTime.now(); }
}
