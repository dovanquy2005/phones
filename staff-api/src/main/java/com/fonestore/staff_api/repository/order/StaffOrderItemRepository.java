// staff_api/repository/order/StaffOrderItemRepository.java
package com.fonestore.staff_api.repository.order;

import com.fonestore.staff_api.repository.order.view.OrderItemDetailView;
import com.fonestore.user_api.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StaffOrderItemRepository extends JpaRepository<OrderItem, Long> {

    // Lấy list OrderItem entity nếu bạn vẫn cần
    List<OrderItem> findByOrderIdOrderByIdAsc(Long orderId);

    // Lấy chi tiết có kèm sku_code + product_name
    @Query(value = """
        SELECT 
            oi.order_item_id  AS orderItemId,
            oi.order_id       AS orderId,
            oi.qty            AS qty,
            oi.unit_price     AS unitPrice,
            pv.sku_code       AS skuCode,
            p.name            AS productName
        FROM order_items oi
        JOIN product_variants pv ON oi.sku_id = pv.sku_id
        JOIN products        p  ON pv.product_id = p.product_id
        WHERE oi.order_id = :orderId
        ORDER BY oi.order_item_id
        """, nativeQuery = true)
    List<OrderItemDetailView> findDetailsByOrderId(@Param("orderId") Long orderId);
}
