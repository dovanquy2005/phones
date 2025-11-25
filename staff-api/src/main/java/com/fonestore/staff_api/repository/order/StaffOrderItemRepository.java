package com.fonestore.staff_api.repository.order;

import com.fonestore.staff_api.repository.order.view.OrderItemDetailView;
import com.fonestore.user_api.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface StaffOrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderIdOrderByIdAsc(Long orderId);

    @Query("""
       SELECT 
         oi.id as orderItemId,
         oi.order.id as orderId,
         oi.quantity as qty,
         oi.unitPrice as unitPrice,
         pv.skuCode as skuCode,
         p.name as productName,
      
         pi.filePath as imageUrl,
         
         CONCAT(COALESCE(pv.color, ''), ' ', COALESCE(pv.capacity, '')) as variantInfo,
         b.name as brandName
         
       FROM OrderItem oi
       LEFT JOIN ProductVariant pv ON oi.skuId = pv.id
       LEFT JOIN Product p ON p.id = pv.productId
       LEFT JOIN ProductImage pi ON (pi.productId = p.id AND pi.sortOrder = 0)
       LEFT JOIN Brand b ON b.id = p.brandId
       WHERE oi.order.id = :orderId
    """)
    List<OrderItemDetailView> findDetailsByOrderId(Long orderId);
}