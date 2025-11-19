package com.fonestore.user_api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fonestore.staff_api.entity.ProductVariant;
import com.fonestore.staff_api.entity.enums.PaymentStatus;
import com.fonestore.user_api.dto.checkout.CheckoutDTO;
import com.fonestore.user_api.dto.checkout.CheckoutOrderResponse;
import com.fonestore.user_api.dto.checkout.PlaceOrderRequest;
import com.fonestore.user_api.entity.Order;
import com.fonestore.user_api.entity.Payment;
import com.fonestore.user_api.entity.Shipment;
import com.fonestore.user_api.repository.PaymentRepository;
import com.fonestore.user_api.repository.ShipmentRepository;
import com.fonestore.user_api.repository.order.UserOrderItemRepository;
import com.fonestore.user_api.repository.order.UserOrderRepository;
import com.fonestore.staff_api.repository.product.ProductVariantRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final CartService cartService;
    private final UserOrderRepository orderRepo;
    private final UserOrderItemRepository itemRepo;
    private final ProductVariantRepository variantRepo;
    
    // Thêm 2 Repo này để lưu thanh toán và vận chuyển
    private final PaymentRepository paymentRepo;
    private final ShipmentRepository shipmentRepo;
    
    private final ObjectMapper objectMapper;

    @Transactional
    public CheckoutDTO buildCheckout(Long userId) {
        var cartDto = cartService.getCart(userId);

        CheckoutDTO dto = new CheckoutDTO();
        dto.setSubtotal(cartDto.subtotal().longValue());
        dto.setDiscount(cartDto.discount().longValue());
        dto.setTotal(cartDto.total().longValue());
        
        List<CheckoutDTO.ShippingOption> shipping = new ArrayList<>();
        CheckoutDTO.ShippingOption s = new CheckoutDTO.ShippingOption();
        s.setId("std"); s.setName("Giao tiêu chuẩn"); s.setFee(25000L); s.setEta("3-5 ngày");
        shipping.add(s);
        dto.setShipping_options(shipping);
        dto.setSelected_shipping("std");
        dto.setTax(0L);
        dto.setPayment_methods(List.of("cod","card","bank_transfer"));

        List<CheckoutDTO.Warning> warnings = new ArrayList<>();
        cartDto.items().forEach(it -> {
            Optional<ProductVariant> pv = variantRepo.findById(it.skuId());
            if (pv.isPresent()) {
                ProductVariant cur = pv.get();
                BigDecimal currentPrice = BigDecimal.valueOf(cur.getListPrice());
                BigDecimal snapshot = it.unitPrice();
                if (currentPrice.compareTo(snapshot) != 0) {
                    var w = new CheckoutDTO.Warning();
                    w.setType("price_changed");
                    w.setSku(String.valueOf(it.skuId()));
                    w.setOld(snapshot.longValue());
                    w.setNeo(currentPrice.longValue());
                    warnings.add(w);
                }
                try {
                    Object stockObj = cur.getClass().getMethod("getStock").invoke(cur);
                    if (stockObj instanceof Number) {
                        long stock = ((Number) stockObj).longValue();
                        if (it.qty() > stock) {
                            var w = new CheckoutDTO.Warning();
                            w.setType("out_of_stock");
                            w.setSku(String.valueOf(it.skuId()));
                            w.setReason("available=" + stock);
                            warnings.add(w);
                        }
                    }
                } catch (NoSuchMethodException ignore) {}
                catch (Exception ex) { /* ignore */ }
            } else {
                var w = new CheckoutDTO.Warning();
                w.setType("sku_not_found"); w.setSku(String.valueOf(it.skuId()));
                w.setReason("sku not found");
                warnings.add(w);
            }
        });

        dto.setWarnings(warnings);
        return dto;
    }

    @Transactional
    public CheckoutOrderResponse placeOrder(Long userId, PlaceOrderRequest req) {
        Order draft = cartService.refreshDraftAndGetEntity(userId);
        if (draft == null) {
            return CheckoutOrderResponse.conflict("Cart not found", List.of());
        }

        if (itemRepo.countByOrder_Id(draft.getId()) == 0) {
            return CheckoutOrderResponse.conflict("Cart is empty", List.of());
        }

        // validate items
        List<CheckoutDTO.Warning> warnings = new ArrayList<>();
        var rows = itemRepo.findLinesWithInfo(draft.getId());
        for (var r : rows) {
            Long currentSkuId = r.getSkuId();
            if (currentSkuId == null) continue; 

            Optional<ProductVariant> pv = variantRepo.findById(currentSkuId);
            if (pv.isEmpty()) {
                var w = new CheckoutDTO.Warning(); w.setType("sku_not_found"); w.setSku(String.valueOf(r.getSkuId()));
                w.setReason("sku not found");
                warnings.add(w);
                continue;
            }
            ProductVariant cur = pv.get();
            BigDecimal currentPrice = BigDecimal.valueOf(cur.getListPrice());
            BigDecimal snapshot = r.getUnitPrice() == null ? BigDecimal.ZERO : r.getUnitPrice();
            if (currentPrice.compareTo(snapshot) != 0) {
                var w = new CheckoutDTO.Warning(); w.setType("price_changed"); w.setSku(String.valueOf(r.getSkuId()));
                w.setOld(snapshot.longValue()); w.setNeo(currentPrice.longValue());
                warnings.add(w);
            }
            try {
                Object stockObj = cur.getClass().getMethod("getStock").invoke(cur);
                if (stockObj instanceof Number) {
                    long stock = ((Number) stockObj).longValue();
                    if (r.getQuantity() > stock) {
                        var w = new CheckoutDTO.Warning(); w.setType("out_of_stock"); w.setSku(String.valueOf(r.getSkuId()));
                        w.setReason("available=" + stock);
                        warnings.add(w);
                    }
                }
            } catch (NoSuchMethodException ignore) {}
            catch (Exception ex) { /* ignore */ }
        }

        if (!warnings.isEmpty()) {
            return CheckoutOrderResponse.conflict("Validation failed", warnings);
        }

        // reduce stock
        for (var r : rows) {
            Optional<ProductVariant> pv = variantRepo.findById(r.getSkuId());
            if (pv.isPresent()) {
                ProductVariant cur = pv.get();
                try {
                    Object stockObj = cur.getClass().getMethod("getStock").invoke(cur);
                    if (stockObj instanceof Number) {
                        long stock = ((Number) stockObj).longValue();
                        long newStock = Math.max(0, stock - r.getQuantity());
                        try {
                            var setStock = cur.getClass().getMethod("setStock", stockObj.getClass());
                            if (stockObj instanceof Integer) setStock.invoke(cur, (int)newStock);
                            else setStock.invoke(cur, newStock);
                            variantRepo.save(cur);
                        } catch (NoSuchMethodException ignore) {}
                    }
                } catch (NoSuchMethodException ignore) {}
                catch (Exception ex) { /* log */ }
            }
        }

        // 1. Lưu thông tin địa chỉ & ghi chú
        try {
            if (req.getShipping_address() != null) {
                String jsonAddress = objectMapper.writeValueAsString(req.getShipping_address());
                draft.setAddressSnapshot(jsonAddress);
            }
            if (req.getNote() != null) {
                draft.setNote(req.getNote());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 2. Tạo bản ghi Thanh toán (Payment) khởi tạo
        Payment p = new Payment();
        p.setOrderId(draft.getId());
        p.setMethod(req.getPayment_method() != null ? req.getPayment_method() : "cod");
        p.setAmount(draft.getTotal());
        p.setStatus(PaymentStatus.UNPAID); // Mặc định chưa thanh toán
        paymentRepo.save(p);

        // 3. Tạo bản ghi Vận chuyển (Shipment) khởi tạo
        Shipment s = new Shipment();
        s.setOrderId(draft.getId());
        s.setFee(draft.getShippingFee());
        s.setStatus("PENDING"); // Mặc định chờ xử lý
        s.setCarrier("Tiêu chuẩn"); // Hoặc lấy từ request nếu có
        shipmentRepo.save(s);

        // 4. Cập nhật trạng thái đơn hàng
        draft.setStatus("CREATED");
        draft.setUpdatedAt(Instant.now());
        orderRepo.save(draft);

        return CheckoutOrderResponse.success(String.valueOf(draft.getId()));
    }
}