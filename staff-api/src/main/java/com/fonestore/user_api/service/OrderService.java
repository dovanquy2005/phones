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
import com.fonestore.user_api.entity.Shipment;
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

@Service("userOrderService")
@RequiredArgsConstructor
public class OrderService {

    private static final int SCALE = 0; // VND không lẻ
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE);

    private final com.fonestore.user_api.repository.order.UserOrderItemRepository itemRepo;
    private final UserOrderRepository orderRepo;
    private final ProductVariantRepository variantRepo;
    private final UserProductRepository productRepo;
    private final PaymentRepository paymentRepo;
    private final ShipmentRepository shipmentRepo;

    private static BigDecimal bd(long v) { return new BigDecimal(v).setScale(SCALE); }
    private static BigDecimal ensure(BigDecimal v) { return v == null ? ZERO : v.setScale(SCALE); }

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

            lines.add(new PagedOrderResponse.Line(v.getId(), qty, unit, p.getName()));
        }

        o.setSubtotal(subtotal);
        o.setDiscount(discount);
        o.setShippingFee(shipping);
        o.setTotal(subtotal.subtract(discount).add(shipping));
        orderRepo.saveAndFlush(o);

        var pays  = mapPayments(paymentRepo.findByOrderIdOrderByCreatedAtAsc(o.getId()));
        var ships = mapShipments(shipmentRepo.findByOrderIdOrderByCreatedAtAsc(o.getId()));
        return toResponse(o, lines, pays, ships);
    }

    // ---------- LIST BY USER (no pagination) ----------
    @Transactional(readOnly = true)
    public List<PagedOrderResponse> listOrdersForUser(Long userId, String status) {
        List<Order> orders;

        if (status != null && !status.isBlank()) {
            // nếu client yêu cầu status cụ thể — trả đúng filter đó (có thể là "DRAFT" nếu client muốn)
            orders = orderRepo.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status);
        } else {
            // default: lịch sử orders — exclude DRAFT
            List<String> visible = List.of(
                "CREATED", "PAID", "SHIPPED","SHIPPING", "DELIVERED", "COMPLETED", "CANCELED", "FAILED"
            );
            orders = orderRepo.findByUserIdAndStatusInOrderByCreatedAtDesc(userId, visible);
        }

        return orders.stream().map(o -> {
            var lines = o.getItems() == null ? List.<PagedOrderResponse.Line>of()
                : o.getItems().stream().map(it ->
                    new PagedOrderResponse.Line(
                        it.getSkuId(),
                        it.getQuantity(),
                        ensure(it.getUnitPrice()),
                        resolveProductName(it.getSkuId())
                    )
                ).toList();
            var pays  = mapPayments(paymentRepo.findByOrderIdOrderByCreatedAtAsc(o.getId()));
            var ships = mapShipments(shipmentRepo.findByOrderIdOrderByCreatedAtAsc(o.getId()));
            return toResponse(o, lines, pays, ships);
        }).toList();
    }


    // ---------- find all for user (existing non-paged method) ----------
    @Transactional(readOnly = true)
    public List<PagedOrderResponse> findByUserId(Long userId) {
        return orderRepo.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(o -> {
                    var lines = o.getItems() == null ? List.<PagedOrderResponse.Line>of()
                            : o.getItems().stream().map(it ->
                                new PagedOrderResponse.Line(
                                    it.getSkuId(),
                                    it.getQuantity(),
                                    ensure(it.getUnitPrice()),
                                    resolveProductName(it.getSkuId())
                                )
                            ).toList();
                    var pays  = mapPayments(paymentRepo.findByOrderIdOrderByCreatedAtAsc(o.getId()));
                    var ships = mapShipments(shipmentRepo.findByOrderIdOrderByCreatedAtAsc(o.getId()));
                    return toResponse(o, lines, pays, ships);
                })
                .toList();
    }

    // ---------- DETAIL ----------
    @Transactional(readOnly = true)
    public PagedOrderResponse getById(Long orderId) {
        var o = orderRepo.findByIdFetchItems(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        var lines = o.getItems() == null ? List.<PagedOrderResponse.Line>of()
                : o.getItems().stream().map(it ->
                    new PagedOrderResponse.Line(
                        it.getSkuId(),
                        it.getQuantity(),
                        ensure(it.getUnitPrice()),
                        resolveProductName(it.getSkuId())
                    )
                ).toList();

        var pays  = mapPayments(paymentRepo.findByOrderIdOrderByCreatedAtAsc(o.getId()));
        var ships = mapShipments(shipmentRepo.findByOrderIdOrderByCreatedAtAsc(o.getId()));
        return toResponse(o, lines, pays, ships);
    }

    // ---------- helpers ----------
    private String resolveProductName(Long skuId) {
        var v = variantRepo.findById(skuId).orElse(null);
        if (v == null) return null;
        var p = productRepo.findById(v.getProductId()).orElse(null);
        return p != null ? p.getName() : null;
    }

    private List<PaymentDTO> mapPayments(List<Payment> list) {
        return list.stream().map(p ->
            new PaymentDTO(
                p.getId(),
                p.getMethod(),
                ensure(p.getAmount()),
                p.getStatus(),
                p.getTxnRef(),
                p.getCreatedAt()
            )
        ).toList();
    }

    private List<ShipmentDTO> mapShipments(List<Shipment> list) {
        return list.stream().map(s ->
            new ShipmentDTO(
                s.getId(),
                s.getCarrier(),
                s.getTrackingNo(),
                s.getStatus(),
                ensure(s.getFee()),
                s.getCreatedAt(),
                s.getUpdatedAt()
            )
        ).toList();
    }

    private PagedOrderResponse toResponse(
            Order o,
            List<PagedOrderResponse.Line> lines,
            List<PaymentDTO> pays,
            List<ShipmentDTO> ships
    ) {
        return new PagedOrderResponse(
                o.getId(),
                o.getStatus(),
                ensure(o.getSubtotal()),
                ensure(o.getDiscount()),
                ensure(o.getShippingFee()),
                ensure(o.getTotal()),
                o.getCreatedAt(),
                o.getAddressSnapshot(),
                o.getNote(),
                lines, pays, ships
        );
    }

    // phương thức thêm vào OrderService
    @Transactional
    public UserOrderDetailDTO getOrderDetail(Long userId, Long orderId) {
        // đảm bảo order thuộc về user
        var opt = orderRepo.findByIdAndUserIdFetchItems(orderId, userId);
        if (opt.isEmpty()) return null;
        Order o = opt.get();

        // Lấy list OrderItem (fallback nếu bạn không có projection)
        List<OrderItem> rows = itemRepo.findByOrder_Id(o.getId());

        // Map từng OrderItem -> OrderItemSummaryDTO
        List<UserOrderItemSummaryDTO> items = rows.stream().map(r -> {
            UserOrderItemSummaryDTO it = new UserOrderItemSummaryDTO();
            it.setItemId(r.getId());
            it.setSkuId(r.getSkuId()); // Assuming OrderItem has getSkuId()
            // it.setProductName(r.getProductName());
            // it.setImagePath(r.getImagePath());
            it.setQty(r.getQuantity());
            it.setUnitPrice(r.getUnitPrice());
            it.setLineTotal(r.getUnitPrice() != null
                    ? r.getUnitPrice().multiply(java.math.BigDecimal.valueOf(r.getQuantity()))
                    : java.math.BigDecimal.ZERO);
            return it;
        }).collect(Collectors.toList());

        // Build OrderDetailDTO
        UserOrderDetailDTO dto = new UserOrderDetailDTO();
        dto.setOrderId(o.getId());
        dto.setStatus(o.getStatus());
        dto.setCreatedAt(o.getCreatedAt());
        dto.setSubtotal(o.getSubtotal());
        dto.setDiscount(o.getDiscount());
        dto.setShippingFee(o.getShippingFee());
        dto.setTotal(o.getTotal());
        dto.setItems(items);

        // optional fields (nếu Order entity có)
        try { // lấy payments & shipments (nếu cần)
            List<PaymentDTO> pays = mapPayments(paymentRepo.findByOrderIdOrderByCreatedAtAsc(o.getId()));
            // set paymentMethod: lấy method của payment đầu tiên (hoặc join tất cả methods nếu bạn muốn)
            if (pays != null && !pays.isEmpty()) {
                // option A: chỉ lấy method của payment đầu tiên (thường là order-level payment)
                dto.setPaymentMethod(pays.get(0).method());

                // option B (nếu muốn một chuỗi tất cả methods): 
                // dto.setPaymentMethod(pays.stream().map(PaymentDTO::method).collect(Collectors.joining(", ")));
            } else {
                dto.setPaymentMethod(null);
        }} catch (Throwable ignored) {}
        try { dto.setShippingAddress(o.getAddressSnapshot()); } catch (Throwable ignored) {}
        try { dto.setNote(o.getNote()); } catch (Throwable ignored) {}

        return dto;
    }

}
