package com.fonestore.staff_api.repo;

import com.fonestore.staff_api.entity.Product;
import com.fonestore.staff_api.repo.proj.ProductSummaryRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductQueryRepository extends JpaRepository<Product, Long> {

    @Query(value = """

      SELECT 
        ps.product_id                              AS productId,

        MAX(ps.product_name)                       AS productName,

        MAX(ps.brand_name)                         AS brandName,

        MAX(ps.category_name)                      AS categoryName,

        MIN(ps.sku_code)                           AS sampleSku,

        SUM(ISNULL(ps.qty_in_stock,0))             AS qtyInStock,

        SUM(ISNULL(ps.qty_reserved,0))             AS qtyReserved,

        SUM(ISNULL(ps.qty_sold,0))                 AS qtySold,

        MIN(ps.list_price)                         AS minPrice

      FROM v_product_stock ps

      GROUP BY ps.product_id

      ORDER BY MAX(ps.product_name)

      """,

      countQuery = """

        SELECT COUNT(DISTINCT ps.product_id)

        FROM v_product_stock ps

      """,

      nativeQuery = true)

    Page<ProductSummaryRow> pageProductSummary(Pageable pageable);

}

