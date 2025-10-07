package com.fonestore.staff_api.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "stock_imei")
public class StockImei {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "imei_id")
    private Long imeiId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sku_id", nullable = false)
    private Variant variant;

    @Column(name = "imei", nullable = false, length = 64)
    private String imei;

    @Column(name = "status", nullable = false, length = 32)
    private String status; // in_stock / reserved / sold / warranty

    // getters/setters
    public Long getImeiId() { return imeiId; }
    public Variant getVariant() { return variant; }
    public void setVariant(Variant variant) { this.variant = variant; }
    public String getImei() { return imei; }
    public void setImei(String imei) { this.imei = imei; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
