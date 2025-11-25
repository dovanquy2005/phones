package com.fonestore.staff_api.dto.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

// Cập nhật thêm trường paymentMethod, paymentStatus và đổi list items
public record OrderDetailDTO(
        Long id,
        String code,
        String status,
        String customerName,
        String customerPhone,
        String addressSnapshot,
        
        // Đổi từ OrderItemDTO -> OrderItemDetailDTO (để có ảnh + tên sp)
        List<OrderItemDetailDTO> items, 
        
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal shippingFee,
        BigDecimal total,
        
        // THÊM 2 TRƯỜNG NÀY ĐỂ HẾT LỖI
        String paymentMethod,
        String paymentStatus,
        String voucherCode,

        String note,
        Instant createdAt,
        Instant updatedAt
) {}