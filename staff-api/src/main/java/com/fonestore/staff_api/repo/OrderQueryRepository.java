package com.fonestore.staff_api.repo;

import com.fonestore.staff_api.entity.Order;
import com.fonestore.staff_api.repo.proj.OrderSummaryRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderQueryRepository extends JpaRepository<Order, Long> {

    @Query(value = """
        SELECT
          o.order_id   AS orderId,
          o.user_id    AS userId,
          o.total_amt  AS totalAmt,
          o.status     AS status,
          o.created_at AS createdAt
        FROM orders o
        ORDER BY o.created_at DESC
    """, nativeQuery = true)
    List<OrderSummaryRow> findAllSummary();
}
