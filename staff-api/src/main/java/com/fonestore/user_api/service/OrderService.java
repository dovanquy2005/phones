package com.fonestore.user_api.service;

import com.fonestore.user_api.dto.order.UserOrderDetailDTO;
import com.fonestore.staff_api.repository.product.ProductVariantRepository;
import com.fonestore.user_api.dto.*;
import com.fonestore.user_api.dto.order.CreateOrderRequest;
import com.fonestore.user_api.dto.order.UserOrderItemSummaryDTO;
import com.fonestore.user_api.dto.order.PagedOrderResponse;
import com.fonestore.user_api.entity.Order;
import com.fonestore.user_api.entity.OrderItem;
import com.fonestore.user_api.entity.Payment;
// IMPORT QUAN TRỌNG:
import com.fonestore.user_api.repository.*;
import com.fonestore.user_api.repository.order.UserOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.fonestore.staff_api.repository.product.ProductImageRepository;

@Service("userOrderService")
@RequiredArgsConstructor
public class OrderService {

    private static final int SCALE = 0;
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE);

    private final com.fonestore.user_api.repository.order.UserOrderItemRepository itemRepo;
    private final UserOrderRepository orderRepo;
    private final ProductVariantRepository variantRepo;
    private final UserProductRepository productRepo;
    private final PaymentRepository paymentRepo;
    
    // Inject thêm repo này để lấy ảnh
    private final ProductImageRepository imageRepo;

    private static BigDecimal bd(long v) { return new BigDecimal(v).setScale(SCALE); }
    private static BigDecimal ensure(BigDecimal v) { return v == null ? ZERO : v.setScale(SCALE); }

    // Hàm helper để lấy ảnh bìa từ ProductImageRepository
    private String getCoverImage(Long productId) {
        if (productId == null) return null;
        return imageRepo.findByProductIdOrderBySortOrderAsc(productId)
                .stream().findFirst()
                .map(img -> img.getFilePath())
                .orElse(null);
    }

    // ---------- CREATE ----------
    @Transactional
    public PagedOrderResponse create(CreateOrderRequest req) {
        BigDecimal shipping = bd(30000);
        BigDecimal discount = ZERO;
        BigDecimal subtotal = ZERO;

        Order o = new Order();
        o.setUserId(req.userId());
        o.setStatus("PENDING");
        o.setAddressSnapshot(req.addressSnapshot());
        o.setNote(req.note());
        o.setCreatedAt(Instant.now());
        o.setItems(new ArrayList<>());

        List<PagedOrderResponse.Line> lines = new ArrayList<>();

        for (CreateOrderRequest.Item it : req.items()) {
            var v = variantRepo.findById(it.skuId())
                    .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + it.skuId()));
            var p = productRepo.findById(v.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found for SKU: " + it.skuId()));

            int qty = it.qty();
            if (p.getQuantity() == null || p.getQuantity() < qty) {
                throw new IllegalStateException("Not enough stock for product_id=" + p.getId());
            }
            p.setQuantity(p.getQuantity() - qty);
            productRepo.save(p);

            BigDecimal unit = ensure(BigDecimal.valueOf(v.getListPrice()));
            subtotal = subtotal.add(unit.multiply(BigDecimal.valueOf(qty)));

            OrderItem oi = new OrderItem();
            oi.setOrder(o);
            oi.setSkuId(v.getId());
            oi.setQuantity(qty);
            oi.setUnitPrice(unit);
            o.getItems().add(oi);

            // CẬP NHẬT: Truyền đủ 7 tham số (bao gồm ảnh, màu, dung lượng)
            // Dùng hàm getCoverImage(p.getId()) thay vì p.getImagePath()
            lines.add(new PagedOrderResponse.Line(
                v.getId(), 
                qty, 
                unit, 
                p.getName(),
                getCoverImage(p.getId()), // <--- Đã sửa
                v.getColor(),     
                v.getCapacity()   
            ));
        }

        o.setSubtotal(subtotal);
        o.setDiscount(discount);
        o.setShippingFee(shipping);
        o.setTotal(subtotal.subtract(discount).add(shipping));
        orderRepo.saveAndFlush(o);

        var pays = mapPayments(paymentRepo.findByOrderIdOrderByCreatedAtAsc(o.getId()));
        return toResponse(o, lines, pays);
    }

    // ---------- LIST (Sử dụng hàm helper mapOrderToResponse) ----------
    @Transactional(readOnly = true)
    public List<PagedOrderResponse> listOrdersForUser(Long userId, String status) {
        List<Order> orders;
        if (status != null && !status.isBlank()) {
            orders = orderRepo.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status);
        } else {
            List<String> visible = List.of("CREATED", "PAID", "SHIPPED","SHIPPING", "DELIVERED", "COMPLETED", "CANCELED", "FAILED");
            orders = orderRepo.findByUserIdAndStatusInOrderByCreatedAtDesc(userId, visible);
        }
        return orders.stream().map(this::mapOrderToResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PagedOrderResponse> findByUserId(Long userId) {
        return orderRepo.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::mapOrderToResponse).toList();
    }

    // Hàm helper để map Order entity sang PagedOrderResponse cho List view
    // Hàm helper an toàn hơn để tránh lỗi 500 khi dữ liệu bẩn
    private PagedOrderResponse mapOrderToResponse(Order o) {
        var lines = o.getItems() == null ? List.<PagedOrderResponse.Line>of()
            : o.getItems().stream().map(it -> {
                Long skuId = it.getSkuId();
                
                // 1. Phòng thủ: Nếu skuId trong order_items bị null -> trả về item rỗng
                if (skuId == null) {
                    return new PagedOrderResponse.Line(
                        0L, it.getQuantity(), ensure(it.getUnitPrice()), 
                        "[Sản phẩm lỗi - Mất SKU]", null, null, null
                    );
                }

                // 2. Tìm Variant, nếu không thấy (đã bị xóa) -> trả về null
                var v = variantRepo.findById(skuId).orElse(null);
                
                // 3. Tìm Product, nếu Variant null hoặc Product null -> trả về null
                var p = (v != null) ? productRepo.findById(v.getProductId()).orElse(null) : null;
                
                return new PagedOrderResponse.Line(
                    skuId,
                    it.getQuantity(),
                    ensure(it.getUnitPrice()),
                    // Nếu Product còn tồn tại thì lấy tên, không thì hiển thị fallback
                    (p != null) ? p.getName() : "Sản phẩm không tồn tại (ID: " + skuId + ")",
                    (p != null) ? getCoverImage(p.getId()) : null, 
                    (v != null) ? v.getColor() : null,     
                    (v != null) ? v.getCapacity() : null   
                );
            }).toList();

        var pays = mapPayments(paymentRepo.findByOrderIdOrderByCreatedAtAsc(o.getId()));
        return toResponse(o, lines, pays);
    }

    // ---------- DETAIL (Quan trọng cho trang order-detail.html) ----------
    @Transactional(readOnly = true)
    public PagedOrderResponse getById(Long orderId) {
        var o = orderRepo.findByIdFetchItems(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        // SỬ DỤNG findLinesWithInfo ĐỂ LẤY FULL THÔNG TIN (Ảnh, Màu, Dung lượng)
        List<CartLineRow> rows = itemRepo.findLinesWithInfo(orderId);

        List<PagedOrderResponse.Line> lines = rows.stream().map(r -> 
            new PagedOrderResponse.Line(
                r.getSkuId(),
                r.getQuantity(),
                ensure(r.getUnitPrice()),
                r.getProductName(),
                r.getImagePath(), // Có ảnh (từ query)
                r.getColor(),     // Có màu
                r.getCapacity()   // Có dung lượng
            )
        ).toList();

        var pays = mapPayments(paymentRepo.findByOrderIdOrderByCreatedAtAsc(o.getId()));
        return toResponse(o, lines, pays);
    }

    // ---------- HELPERS ----------
    private List<PaymentDTO> mapPayments(List<Payment> list) {
        return list.stream().map(p ->
            new PaymentDTO(
                p.getId(), p.getMethod(), ensure(p.getAmount()),
                p.getStatus(), p.getTxnRef(), p.getCreatedAt()
            )
        ).toList();
    }

    private PagedOrderResponse toResponse(Order o, List<PagedOrderResponse.Line> lines, List<PaymentDTO> pays) {
        return new PagedOrderResponse(
            o.getId(), o.getStatus(), ensure(o.getSubtotal()), ensure(o.getDiscount()),
            ensure(o.getShippingFee()), ensure(o.getTotal()), o.getCreatedAt(),
            o.getAddressSnapshot(), o.getNote(), lines, pays
        );
    }

    // API nội bộ cho User (đã có sẵn, chỉ cập nhật để dùng chung logic nếu cần)
    @Transactional
    public UserOrderDetailDTO getOrderDetail(Long userId, Long orderId) {
        var opt = orderRepo.findByIdAndUserIdFetchItems(orderId, userId);
        if (opt.isEmpty()) return null;
        Order o = opt.get();
        List<CartLineRow> rows = itemRepo.findLinesWithInfo(o.getId());

        List<UserOrderItemSummaryDTO> items = rows.stream().map(r -> {
            UserOrderItemSummaryDTO it = new UserOrderItemSummaryDTO();
            it.setItemId(r.getId());
            it.setSkuId(r.getSkuId());
            it.setProductName(r.getProductName());
            it.setImagePath(r.getImagePath());
            it.setQty(r.getQuantity());
            it.setUnitPrice(r.getUnitPrice());
            it.setColor(r.getColor());
            it.setCapacity(r.getCapacity());
            it.setLineTotal(r.getUnitPrice() != null ? r.getUnitPrice().multiply(BigDecimal.valueOf(r.getQuantity())) : BigDecimal.ZERO);
            return it;
        }).collect(Collectors.toList());

        UserOrderDetailDTO dto = new UserOrderDetailDTO();
        dto.setOrderId(o.getId());
        dto.setStatus(o.getStatus());
        dto.setCreatedAt(o.getCreatedAt());
        dto.setSubtotal(o.getSubtotal());
        dto.setDiscount(o.getDiscount());
        dto.setShippingFee(o.getShippingFee());
        dto.setTotal(o.getTotal());
        dto.setItems(items);
        
        try {
             List<PaymentDTO> pays = mapPayments(paymentRepo.findByOrderIdOrderByCreatedAtAsc(o.getId()));
             if (pays != null && !pays.isEmpty()) dto.setPaymentMethod(pays.get(0).method());
        } catch (Throwable ignored) {}
        try { dto.setShippingAddress(o.getAddressSnapshot()); } catch (Throwable ignored) {}
        try { dto.setNote(o.getNote()); } catch (Throwable ignored) {}

        return dto;
    }
}