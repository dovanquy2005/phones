package com.fonestore.staff_api.dto.order;

import com.fonestore.staff_api.repository.order.view.OrderItemDetailView;

import java.math.BigDecimal;

public record OrderItemDetailDTO(
        Long orderItemId,
        Long orderId,
        Integer qty,
        BigDecimal unitPrice,
        String skuCode,
        String productName,
        
        // --- CÁC TRƯỜNG MỚI THÊM VÀO ---
        String imageUrl,      // Để hiện ảnh
        String variantInfo,   // Để hiện màu sắc/bộ nhớ
        BigDecimal subtotal,   // Tiền hàng (đơn giá * số lượng)
        String brandName
        
) {
    public static OrderItemDetailDTO from(OrderItemDetailView v) {
        // Tính thành tiền
        BigDecimal total = (v.getUnitPrice() != null && v.getQty() != null)
                ? v.getUnitPrice().multiply(BigDecimal.valueOf(v.getQty()))
                : BigDecimal.ZERO;

        return new OrderItemDetailDTO(
                v.getOrderItemId(),
                v.getOrderId(),
                v.getQty(),
                v.getUnitPrice(),
                v.getSkuCode(),
                v.getProductName(),
                
                // Map các trường mới (Lưu ý: Interface View phải có getter tương ứng)
                v.getImageUrl(),
                v.getVariantInfo(),
                total,
                v.getBrandName()
        );
    }
}