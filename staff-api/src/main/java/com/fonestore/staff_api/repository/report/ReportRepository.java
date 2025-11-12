// com.fonestore.staff_api.repository.report.ReportRepository
package com.fonestore.staff_api.repository.report;

import com.fonestore.user_api.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;

public interface ReportRepository extends JpaRepository<Order, Long> {

  // Doanh thu theo ngày
  @Query(value = """
    SELECT
      CAST(o.created_at AS date) AS d,
      COALESCE(SUM(CAST(oi.qty AS decimal(19,2)) * CAST(oi.unit_price AS decimal(19,2))), 0) AS revenue
    FROM orders o
    JOIN order_items oi ON oi.order_id = o.order_id
    WHERE o.created_at >= :fromTs
      AND o.created_at <  :toTs
      AND (o.status IS NULL OR o.status <> 'CANCELED')
    GROUP BY CAST(o.created_at AS date)
    ORDER BY d
  """, nativeQuery = true)
  List<Object[]> dailyRevenue(@Param("fromTs") Timestamp fromTs,
                              @Param("toTs")   Timestamp toTs);

  // Doanh thu theo tháng
  @Query(value = """
    SELECT
      MONTH(o.created_at) AS m,
      COALESCE(SUM(CAST(oi.qty AS decimal(19,2)) * CAST(oi.unit_price AS decimal(19,2))), 0) AS revenue
    FROM orders o
    JOIN order_items oi ON oi.order_id = o.order_id
    WHERE o.created_at >= :startYear
      AND o.created_at <  :endYear
      AND (o.status IS NULL OR o.status <> 'CANCELED')
    GROUP BY MONTH(o.created_at)
    ORDER BY m
  """, nativeQuery = true)
  List<Object[]> monthlyRevenue(@Param("startYear") Timestamp startYear,
                                @Param("endYear")   Timestamp endYear);

  // Top bán chạy theo SKU (KHÔNG dùng :limit để tránh lỗi bind; cắt limit ở Service)
  @Query(value = """
    SELECT
      CAST(oi.sku_id AS bigint) AS skuId,
      SUM(CAST(oi.qty AS bigint)) AS qty,
      SUM(CAST(oi.qty AS decimal(19,2)) * CAST(oi.unit_price AS decimal(19,2))) AS revenue
    FROM orders o
    JOIN order_items oi ON oi.order_id = o.order_id
    WHERE o.created_at >= :fromTs
      AND o.created_at <  :toTs
      AND (o.status IS NULL OR o.status <> 'CANCELED')
    GROUP BY oi.sku_id
    ORDER BY revenue DESC
  """, nativeQuery = true)
  List<Object[]> topSkuRaw(@Param("fromTs") Timestamp fromTs,
                           @Param("toTs")   Timestamp toTs);
}
