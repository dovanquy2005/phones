package com.fonestore.user_api.service;

import com.fonestore.staff_api.repository.product.ProductVariantRepository;
import com.fonestore.user_api.dto.*;
import com.fonestore.user_api.dto.order.CreateOrderRequest;
import com.fonestore.user_api.dto.order.OrderResponse;
import com.fonestore.user_api.entity.Order;
import com.fonestore.user_api.entity.OrderItem;
import com.fonestore.user_api.entity.Payment;
import com.fonestore.user_api.entity.Shipment;
import com.fonestore.user_api.repository.*;
import com.fonestore.user_api.repository.order.OrderRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service("userOrderService")
@RequiredArgsConstructor
public class OrderService {

    private static final int SCALE = 0; // VND không lẻ
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE);

    private final OrderRepository orderRepo;
    private final ProductVariantRepository variantRepo;
    private final UserProductRepository productRepo;
    private final PaymentRepository paymentRepo;
    private final ShipmentRepository shipmentRepo;

    private static BigDecimal bd(long v) { return new BigDecimal(v).setScale(SCALE); }
    private static BigDecimal ensure(BigDecimal v) { return v == null ? ZERO : v.setScale(SCALE); }

    // ---------- CREATE ----------
    @Transactional
    public OrderResponse create(CreateOrderRequest req) {
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

        List<OrderResponse.Line> lines = new ArrayList<>();

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

            BigDecimal unit = ensure(BigDecimal.valueOf(v.getListPrice())); // listPrice là long? convert
            subtotal = subtotal.add(unit.multiply(BigDecimal.valueOf(qty)));

            OrderItem oi = new OrderItem();
            oi.setOrder(o);
            oi.setSkuId(v.getId());
            oi.setQuantity(qty);
            oi.setUnitPrice(unit);
            o.getItems().add(oi);

            lines.add(new OrderResponse.Line(v.getId(), qty, unit, p.getName()));
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

    // ---------- LIST BY USER ----------
    @Transactional(readOnly = true)
    public List<OrderResponse> findByUserId(Long userId) {
        return orderRepo.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(o -> {
                    var lines = o.getItems() == null ? List.<OrderResponse.Line>of()
                            : o.getItems().stream().map(it ->
                                new OrderResponse.Line(
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
    public OrderResponse getById(Long orderId) {
        var o = orderRepo.findByIdFetchItems(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        var lines = o.getItems() == null ? List.<OrderResponse.Line>of()
                : o.getItems().stream().map(it ->
                    new OrderResponse.Line(
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

    private OrderResponse toResponse(
            Order o,
            List<OrderResponse.Line> lines,
            List<PaymentDTO> pays,
            List<ShipmentDTO> ships
    ) {
        return new OrderResponse(
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
}
