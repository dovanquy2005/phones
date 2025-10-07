package com.fonestore.staff_api.repository;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
// CHÚ Ý: Repository ở đây là của Spring Data, KHÔNG phải @Repository annotation
import org.springframework.data.repository.Repository;

@org.springframework.stereotype.Repository
public interface ProductListRepository extends Repository<com.fonestore.staff_api.entity.Product, Long> {

    @Query(
      value = """
      SELECT
        p.product_id               AS id,
        p.name                     AS name,
        b.name                     AS brand,
        c.name                     AS category,
        MIN(v.list_price)          AS minPrice,
        MAX(v.sku_code)            AS sampleSku,
        SUM(CASE WHEN s.status='in_stock' THEN 1 ELSE 0 END)  AS qtyInStock,
        SUM(CASE WHEN s.status='reserved'  THEN 1 ELSE 0 END)  AS qtyReserved,
        SUM(CASE WHEN s.status='sold'      THEN 1 ELSE 0 END)  AS qtySold,
        CAST(CASE WHEN p.is_active=1 THEN 1 ELSE 0 END AS TINYINT) AS status
      FROM products p
      LEFT JOIN brands b            ON b.brand_id = p.brand_id
      LEFT JOIN categories c        ON c.cat_id   = p.cat_id
      LEFT JOIN product_variants v  ON v.product_id = p.product_id
      LEFT JOIN stock_imei s        ON s.sku_id     = v.sku_id
      GROUP BY p.product_id, p.name, b.name, c.name, p.is_active
      ORDER BY p.product_id DESC
      """,
      countQuery = "SELECT COUNT(1) FROM products",
      nativeQuery = true
    )
    Page<Row> page(Pageable pageable);

    // Projection interface: tên getter phải khớp alias trong SELECT
    interface Row {
        Long getId();
        String getName();
        String getBrand();
        String getCategory();
        BigDecimal getMinPrice();
        String getSampleSku();
        Integer getQtyInStock();
        Integer getQtyReserved();
        Integer getQtySold();
        Byte getStatus();
    }
}
