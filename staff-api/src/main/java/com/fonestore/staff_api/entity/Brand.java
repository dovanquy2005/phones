package com.fonestore.staff_api.entity;
import jakarta.persistence.*; import lombok.Getter; import lombok.Setter;
@Entity @Table(name = "brands") @Getter @Setter
public class Brand {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brand_id") private Long id;
    @Column(nullable = false) private String name;
}
