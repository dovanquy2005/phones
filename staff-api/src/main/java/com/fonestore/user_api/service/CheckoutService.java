package com.fonestore.user_api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fonestore.staff_api.entity.Product; // Import Product
import com.fonestore.staff_api.entity.ProductVariant;
import com.fonestore.staff_api.entity.enums.PaymentStatus;
import com.fonestore.user_api.dto.checkout.CheckoutDTO;
import com.fonestore.user_api.dto.checkout.CheckoutOrderResponse;
import com.fonestore.user_api.dto.checkout.PlaceOrderRequest;
import com.fonestore.user_api.entity.Order;
import com.fonestore.user_api.entity.Payment;
import com.fonestore.user_api.repository.PaymentRepository;
import com.fonestore.user_api.repository.UserProductRepository; // Import Repo này
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
    private final UserProductRepository productRepo; // 1. Inject thêm cái này
    private final PaymentRepository paymentRepo;
    
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
        
        // Validate giá và tồn kho (đã sửa logic lấy tồn kho từ Product)
        cartDto.items().forEach(it -> {
            Optional<ProductVariant> pv = variantRepo.findById(it.skuId());
            if (pv.isPresent()) {
                ProductVariant cur = pv.get();
                
                // Check giá
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
                
                // Check tồn kho (SỬA: Lấy từ Product cha)
                Product p = productRepo.findById(cur.getProductId()).orElse(null);
                if (p != null) {
                    long stock = (long) (p.getQuantity() == null ? 0 : p.getQuantity());
                    if (it.qty() > stock) {
                        var w = new CheckoutDTO.Warning();
                        w.setType("out_of_stock");
                        w.setSku(String.valueOf(it.skuId()));
                        w.setReason("available=" + stock);
                        warnings.add(w);
                    }
                }
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

        // --- 1. VALIDATE LẠI TRƯỚC KHI CHỐT ---
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
            
            // Check giá
            BigDecimal currentPrice = BigDecimal.valueOf(cur.getListPrice());
            BigDecimal snapshot = r.getUnitPrice() == null ? BigDecimal.ZERO : r.getUnitPrice();
            if (currentPrice.compareTo(snapshot) != 0) {
                var w = new CheckoutDTO.Warning(); w.setType("price_changed"); w.setSku(String.valueOf(r.getSkuId()));
                w.setOld(snapshot.longValue()); w.setNeo(currentPrice.longValue());
                warnings.add(w);
            }
            
            // Check tồn kho (SỬA: Lấy từ Product cha)
            Product p = productRepo.findById(cur.getProductId()).orElse(null);
            if (p != null) {
                long stock = (long) (p.getQuantity() == null ? 0 : p.getQuantity());
                if (r.getQuantity() > stock) {
                    var w = new CheckoutDTO.Warning(); w.setType("out_of_stock"); w.setSku(String.valueOf(r.getSkuId()));
                    w.setReason("available=" + stock);
                    warnings.add(w);
                }
            }
        }

        if (!warnings.isEmpty()) {
            return CheckoutOrderResponse.conflict("Validation failed", warnings);
        }

        // --- 2. TRỪ TỒN KHO (ĐOẠN QUAN TRỌNG ĐÃ FIX) ---
        for (var r : rows) {
            Optional<ProductVariant> pv = variantRepo.findById(r.getSkuId());
            if (pv.isPresent()) {
                ProductVariant cur = pv.get();
                
                // Lấy sản phẩm cha
                Product p = productRepo.findById(cur.getProductId()).orElse(null);
                
                if (p != null) {
                    // Lấy số lượng hiện tại
                    int currentQty = (p.getQuantity() == null) ? 0 : p.getQuantity();
                    
                    // Trừ đi số lượng khách mua (đảm bảo không âm)
                    int newQty = Math.max(0, currentQty - r.getQuantity());
                    
                    // Cập nhật và lưu lại
                    p.setQuantity(newQty);
                    productRepo.save(p); // Lưu ý: phải save Product, không phải Variant
                }
            }
        }

        // 3. Lưu thông tin địa chỉ & ghi chú
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

        // 4. Tạo bản ghi Thanh toán
        Payment p = new Payment();
        p.setOrderId(draft.getId());
        p.setMethod(req.getPayment_method() != null ? req.getPayment_method() : "cod");
        p.setAmount(draft.getTotal());
        p.setStatus(PaymentStatus.UNPAID);
        paymentRepo.save(p);

        // 5. Cập nhật trạng thái đơn hàng
        draft.setStatus("CREATED");
        draft.setUpdatedAt(Instant.now());
        orderRepo.save(draft);

        return CheckoutOrderResponse.success(String.valueOf(draft.getId()));
    }
}