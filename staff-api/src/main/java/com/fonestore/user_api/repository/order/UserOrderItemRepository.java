package com.fonestore.user_api.repository.order;

import com.fonestore.user_api.entity.OrderItem;
import com.fonestore.user_api.repository.CartLineRow;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;                // NEW

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface UserOrderItemRepository extends JpaRepository<OrderItem, Long> {

    // === giữ nguyên các method cũ ===
    List<OrderItem> findAllByOrder_Id(Long orderId);
        // optional helper to load items by order
    List<OrderItem> findByOrder_Id(Long orderId);

    Optional<OrderItem> findByOrder_IdAndSkuId(Long orderId, Long skuId);

    long countByOrder_Id(Long orderId);

    void deleteByOrder_Id(Long orderId);

    @Query("select coalesce(sum(i.unitPrice * i.quantity), 0) from OrderItem i where i.order.id = :orderId")
    BigDecimal sumLineTotal(@Param("orderId") Long orderId);     // thêm @Param cho chắc

    // === NEW: trả về dòng giỏ có kèm tên & ảnh sản phẩm ===
// repository snippet (nativeQuery for SQL Server)
@Query(value = """
    SELECT
      i.order_item_id   AS id,
      i.sku_id          AS skuId,
      i.qty             AS quantity,
      i.unit_price      AS unitPrice,
      COALESCE(p.name, CONCAT('SKU #', i.sku_id)) AS productName,
      pi.file_path AS imagePath
    FROM order_items i
    LEFT JOIN product_variants v ON v.sku_id = i.sku_id
    LEFT JOIN products p        ON p.product_id = v.product_id
    OUTER APPLY (
      SELECT TOP 1 file_path
      FROM product_images x
      WHERE x.product_id = p.product_id
      ORDER BY x.sort_order ASC
    ) pi
    WHERE i.order_id = :orderId
    ORDER BY i.order_item_id ASC
    """, nativeQuery = true)
List<CartLineRow> findLinesWithInfo(@Param("orderId") Long orderId);


}
