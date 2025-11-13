package com.fonestore.user_api.service;

import com.fonestore.staff_api.entity.ProductVariant;
import com.fonestore.user_api.dto.checkout.CheckoutDTO;
import com.fonestore.user_api.dto.checkout.CheckoutOrderResponse ;
import com.fonestore.user_api.dto.checkout.PlaceOrderRequest;
import com.fonestore.user_api.entity.Order;

import com.fonestore.user_api.repository.order.OrderItemRepository;
import com.fonestore.user_api.repository.order.OrderRepository;
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

    private final CartService cartService; // reuse to get cart/draft
    private final OrderRepository orderRepo;
    private final OrderItemRepository itemRepo;
    private final ProductVariantRepository variantRepo;

    // Preview: read-only-ish (may call recalc via CartService)
    @Transactional
    public CheckoutDTO buildCheckout(Long userId) {
        // reuse cartService.getCart which returns CartDTO (or fetch draft order directly)
        var cartDto = cartService.getCart(userId);

        // map CartDTO -> CheckoutDTO (compute shipping options, warnings, payment methods)
        CheckoutDTO dto = new CheckoutDTO();
        dto.setSubtotal(cartDto.subtotal().longValue());
        dto.setDiscount(cartDto.discount().longValue());
        dto.setTotal(cartDto.total().longValue());
        // minimal shipping options example
        List<CheckoutDTO.ShippingOption> shipping = new ArrayList<>();
        CheckoutDTO.ShippingOption s = new CheckoutDTO.ShippingOption();
        s.setId("std"); s.setName("Giao tiêu chuẩn"); s.setFee(25000L); s.setEta("3-5 ngày");
        shipping.add(s);
        dto.setShipping_options(shipping);
        dto.setSelected_shipping("std");
        dto.setTax(0L);
        dto.setPayment_methods(List.of("cod","card","bank_transfer"));

        // collect item-level warnings by checking current product variant state
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
                // stock check if available
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
                catch (Exception ex) { /* ignore reflection errors */ }
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

    // Place order: full validation + finalize
    @Transactional
    public CheckoutOrderResponse  placeOrder(Long userId, PlaceOrderRequest req) {
        // get draft Order entity
        // get draft and ensure totals persisted
        Order draft = cartService.refreshDraftAndGetEntity(userId);
        if (draft == null) {
            return CheckoutOrderResponse.conflict("Cart not found", List.of());
        }

        if (itemRepo.countByOrder_Id(draft.getId()) == 0) {
            return CheckoutOrderResponse .conflict("Cart is empty", List.of());
        }

        // validate items
        List<CheckoutDTO.Warning> warnings = new ArrayList<>();
        var rows = itemRepo.findLinesWithInfo(draft.getId());
        for (var r : rows) {
            Optional<ProductVariant> pv = variantRepo.findById(r.getSkuId());
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
            // stock check (reflection or direct getter)
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
            return CheckoutOrderResponse .conflict("Validation failed", warnings);
        }

        // all good -> finalize order
        // reduce stock (ensure you have proper locking in real world)
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

        draft.setStatus("CREATED");
        draft.setUpdatedAt(Instant.now());
        orderRepo.save(draft);

        return CheckoutOrderResponse .success(String.valueOf(draft.getId()));
    }
}
