package com.fonestore.user_api.service;

import com.fonestore.staff_api.entity.ProductVariant;
import com.fonestore.staff_api.entity.Voucher;
import com.fonestore.staff_api.repository.product.ProductVariantRepository;
import com.fonestore.user_api.dto.cart.*;
import com.fonestore.user_api.dto.voucher.VoucherApplyRequest;
import com.fonestore.user_api.dto.voucher.VoucherApplyResponse;
import com.fonestore.user_api.entity.Order;
import com.fonestore.user_api.entity.OrderItem;
import com.fonestore.user_api.repository.order.UserOrderItemRepository;
import com.fonestore.user_api.repository.order.UserOrderRepository;
import com.fonestore.user_api.repository.voucher.UserVoucherRepository;
import com.fonestore.user_api.repository.voucher.UserVoucherUsageRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final UserOrderRepository orderRepo;
    private final UserOrderItemRepository itemRepo;
    private final ProductVariantRepository variantRepo;
    private final UserVoucherRepository voucherRepo;
    private final UserVoucherUsageRepository voucherUsageRepo;

    /* ======= Public APIs using userId (no auth in this module) ======= */

    @Transactional
    public CartDTO getCart(Long userId) {
        Order draft = getOrCreateDraft(userId);
        return toDTO(draft);
    }

    @Transactional
    public CartDTO addItem(Long userId, AddItemRequest req) {
        if (req.qty() == null || req.qty() <= 0) {
            throw new IllegalArgumentException("qty must be > 0");
        }
        Order draft = getOrCreateDraft(userId);

        ProductVariant v = variantRepo.findById(req.skuId())
                .orElseThrow(() -> new IllegalArgumentException("SKU not found"));

        // Có thể chặn nếu SKU inactive
        if (Boolean.FALSE.equals(v.getIsActive())) {
            throw new IllegalArgumentException("SKU is inactive");
        }

        OrderItem item = itemRepo.findByOrder_IdAndSkuId(draft.getId(), v.getId())
                .orElseGet(() -> {
                    OrderItem ni = new OrderItem();
                    ni.setOrder(draft);
                    ni.setSkuId(v.getId());
                    ni.setQuantity(0);
                    // snapshot price từ list_price (Long) -> BigDecimal
                    ni.setUnitPrice(longToMoney(v.getListPrice()));
                    return ni;
                });

        int newQty = item.getQuantity() + req.qty();
        if (newQty <= 0) throw new IllegalArgumentException("qty must be > 0");

        item.setQuantity(newQty);
        itemRepo.save(item);

        recalc(draft);
        return toDTO(draft);
    }

    @Transactional
    public CartDTO updateItem(Long userId, UpdateItemRequest req) {
        Order draft = requireDraft(userId);
        OrderItem it = itemRepo.findById(req.itemId())
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));
        if (!Objects.equals(it.getOrder().getId(), draft.getId())) {
            throw new IllegalArgumentException("Item not in current cart");
        }

        if (req.qty() == null) throw new IllegalArgumentException("Missing qty");
        if (req.qty() <= 0) {
            itemRepo.delete(it);
        } else {
            // Nếu muốn thêm rào inactive/validate SKU:
            variantRepo.findById(it.getSkuId())
                    .orElseThrow(() -> new IllegalArgumentException("SKU not found"));
            it.setQuantity(req.qty());
            // giữ nguyên unitPrice (snapshot lúc thêm)
            itemRepo.save(it);
        }
        recalc(draft);
        return toDTO(draft);
    }

    @Transactional
    public CartDTO removeItem(Long userId, Long itemId) {
        Order draft = requireDraft(userId);
        OrderItem it = itemRepo.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));

        if (!Objects.equals(it.getOrder().getId(), draft.getId())) {
            throw new IllegalArgumentException("Item not in current cart");
        }
        itemRepo.delete(it);
        recalc(draft);
        return toDTO(draft);
    }

    @Transactional
    public CartDTO clear(Long userId) {
        Order draft = requireDraft(userId);
        itemRepo.deleteByOrder_Id(draft.getId());
        draft.setSubtotal(ZERO);
        draft.setDiscount(nullToZero(draft.getDiscount()));
        draft.setShippingFee(nullToZero(draft.getShippingFee()));
        draft.setTotal(calcTotal(draft.getSubtotal(), draft.getDiscount(), draft.getShippingFee()));
        draft.setUpdatedAt(Instant.now());
        orderRepo.save(draft);
        return toDTO(draft);
    }

    @Transactional
    public CartDTO checkout(Long userId) {
        Order draft = requireDraft(userId);
        if (itemRepo.countByOrder_Id(draft.getId()) == 0) {
            throw new IllegalStateException("Cart is empty");
        }
        draft.setStatus("CREATED"); // khóa giỏ → tạo đơn
        draft.setUpdatedAt(Instant.now());
        orderRepo.save(draft);
        return toDTO(draft);
    }

    @Transactional
    public CartDTO merge(Long userId, MergeCartRequest req) {
        Order draft = getOrCreateDraft(userId);
        if (req.items() == null || req.items().isEmpty()) return toDTO(draft);

        for (MergeCartRequest.Item x : req.items()) {
            if (x == null || x.skuId() == null || x.qty() == null || x.qty() <= 0) continue;

            ProductVariant v = variantRepo.findById(x.skuId()).orElse(null);
            if (v == null || Boolean.FALSE.equals(v.getIsActive())) continue;

            OrderItem item = itemRepo.findByOrder_IdAndSkuId(draft.getId(), v.getId())
                    .orElseGet(() -> {
                        OrderItem ni = new OrderItem();
                        ni.setOrder(draft);
                        ni.setSkuId(v.getId());
                        ni.setQuantity(0);
                        ni.setUnitPrice(longToMoney(v.getListPrice()));
                        return ni;
                    });

            int newQty = item.getQuantity() + x.qty();
            if (newQty <= 0) continue;

            item.setQuantity(newQty);
            itemRepo.save(item);
        }
        recalc(draft);
        return toDTO(draft);
    }

    /* ======= internals ======= */

    private Order requireDraft(Long userId) {
        return orderRepo.findByUserIdAndStatus(userId, "DRAFT")
                .orElseThrow(() -> new IllegalStateException("No cart (DRAFT)"));
    }

    private Order getOrCreateDraft(Long userId) {
        return orderRepo.findByUserIdAndStatus(userId, "DRAFT")
                .orElseGet(() -> {
                    Order o = new Order();
                    o.setUserId(userId);
                    o.setStatus("DRAFT");
                    o.setSubtotal(ZERO);
                    o.setDiscount(ZERO);
                    o.setShippingFee(ZERO);
                    o.setTotal(ZERO);
                    o.setCreatedAt(Instant.now());
                    o.setUpdatedAt(Instant.now());
                    return orderRepo.save(o);
                });
    }

    private void recalc(Order o) {
        BigDecimal subtotal = itemRepo.sumLineTotal(o.getId());
        if (subtotal == null) subtotal = ZERO;
        o.setSubtotal(subtotal);
        o.setDiscount(nullToZero(o.getDiscount()));
        o.setShippingFee(nullToZero(o.getShippingFee()));
        o.setTotal(calcTotal(o.getSubtotal(), o.getDiscount(), o.getShippingFee()));
        o.setUpdatedAt(Instant.now());
        orderRepo.save(o);
    }

    private static BigDecimal nullToZero(BigDecimal x) {
        return x == null ? ZERO : x;
    }

    private static BigDecimal longToMoney(Long v) {
        return BigDecimal.valueOf(v == null ? 0L : v);
    }

    private static BigDecimal calcTotal(BigDecimal sub, BigDecimal disc, BigDecimal ship) {
        if (sub == null) sub = ZERO;
        if (disc == null) disc = ZERO;
        if (ship == null) ship = ZERO;
        return sub.subtract(disc).add(ship);
    }

 
// trong CartService
private CartDTO toDTO(Order o) {
    var rows = itemRepo.findLinesWithInfo(o.getId());

    var itemDTOs = rows.stream().map(r -> new CartItemDTO(
            r.getId(),
            r.getSkuId(),
            r.getQuantity(),
            nullToZero(r.getUnitPrice()),
            nullToZero(r.getUnitPrice()).multiply(java.math.BigDecimal.valueOf(r.getQuantity())),
            r.getProductName(),
            r.getImagePath()
    )).toList();

    return new CartDTO(
            o.getId(),
            o.getStatus(),
            nullToZero(o.getSubtotal()),
            nullToZero(o.getDiscount()),
            nullToZero(o.getShippingFee()),
            nullToZero(o.getTotal()),
            itemDTOs
    );
}

    /**
     * Apply voucher to current user's draft cart.
     * - Validate voucher locally (active, dates, minOrder, usage limits if usage repo provided)
     * - Compute discount and persist into Order.discount (BigDecimal, VND)
     *
     * NOTE: This implementation DOES NOT call external staff service.
     * If you want to persist voucher code into Order, add a field order.setVoucherCode(...) in Order entity.
     */
    @Transactional
    public VoucherApplyResponse applyVoucher(Long userId, VoucherApplyRequest req, String ignoredBearer) {
        if (req == null || req.getCode() == null || req.getCode().trim().isEmpty()) {
            return VoucherApplyResponse.error("Vui lòng nhập mã.");
        }
        String code = req.getCode().trim();
        // 1) load draft
        Order draft = getOrCreateDraft(userId);

        // 2) find voucher locally (use codeNorm uppercase)
        Optional<Voucher> ov = voucherRepo.findByCodeNorm(code.toUpperCase());
        if (ov.isEmpty()) {
            return VoucherApplyResponse.error("Mã không tồn tại.");
        }
        Voucher v = ov.get();

        // 3) validate active / date window
        if (!v.isActive()) return VoucherApplyResponse.error("Mã đã bị vô hiệu hoá.");
        if (v.getStartsAt() != null && Instant.now().isBefore(v.getStartsAt().toInstant(java.time.ZoneOffset.UTC))) {
            return VoucherApplyResponse.error("Mã chưa bắt đầu.");
        }
        if (v.getEndsAt() != null && Instant.now().isAfter(v.getEndsAt().toInstant(java.time.ZoneOffset.UTC))) {
            return VoucherApplyResponse.error("Mã đã hết hạn.");
        }

        // 4) compute subtotal in VND (BigDecimal) using existing repo logic
        recalc(draft); // ensure subtotal up-to-date
        BigDecimal subtotal = nullToZero(draft.getSubtotal());

        // 5) check minOrder
        if (v.getMinOrder() != null && subtotal.compareTo(v.getMinOrder()) < 0) {
            return VoucherApplyResponse.error("Đơn hàng chưa đủ điều kiện tối thiểu: cần tối thiểu " +
                    v.getMinOrder().setScale(2, RoundingMode.HALF_UP).toPlainString() + " đ.");
        }

        // 6) usage limits (simple checks) - only if voucherUsageRepo implemented
        if (voucherUsageRepo != null) {
            if (v.getUsageLimit() != null && v.getUsageLimit() > 0) {
                long usedTotal = voucherUsageRepo.countByVoucherId(v.getVoucherId());
                if (usedTotal >= v.getUsageLimit()) {
                    return VoucherApplyResponse.error("Mã đã đạt giới hạn sử dụng.");
                }
            }
            if (v.getPerUserLimit() != null && v.getPerUserLimit() > 0) {
                long usedByUser = voucherUsageRepo.countByVoucherIdAndUserId(v.getVoucherId(), userId);
                if (usedByUser >= v.getPerUserLimit()) {
                    return VoucherApplyResponse.error("Bạn đã dùng mã này quá số lần cho phép.");
                }
            }
        }

        // 7) compute discount value (BigDecimal VND)
        BigDecimal discount;
        String type = v.getType() == null ? "fixed" : v.getType().trim().toLowerCase();
        if ("percent".equalsIgnoreCase(type)) {
            BigDecimal pct = v.getValue() == null ? BigDecimal.ZERO : v.getValue();
            // pct is like 10.00 for 10%
            discount = subtotal.multiply(pct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            // fixed value in v.getValue() (VND)
            discount = v.getValue() == null ? BigDecimal.ZERO : v.getValue().setScale(2, RoundingMode.HALF_UP);
        }

        // ensure discount not exceed subtotal
        if (discount.compareTo(subtotal) > 0) discount = subtotal;

        // 8) persist discount into order.discount (existing field)
        draft.setDiscount(discount);
        draft.setUpdatedAt(Instant.now());
        orderRepo.save(draft);

        // 9) (optional) record usage reservation - if voucherUsageRepo supports it
        if (voucherUsageRepo != null) {
            // usageRepo.createReservation(...) - implement as needed
        }

        // 10) build response DTO (note: return discount in VND as BigDecimal)
        VoucherApplyResponse out = new VoucherApplyResponse();
        out.setOk(true);
        out.setCode(v.getCode());
        out.setType(type);
        if ("percent".equalsIgnoreCase(type)) {
            out.setDiscountPercent(v.getValue() == null ? 0 : v.getValue().setScale(0, RoundingMode.HALF_UP).intValue());
            out.setDiscount(null);
        } else {
            // send discount in VND (same unit as Order.discount)
            out.setDiscount(draft.getDiscount());
            out.setDiscountPercent(null);
        }
        out.setMessage("Áp dụng mã thành công.");
        return out;
    }

    /**
     * Remove currently applied voucher effect from draft cart.
     * This implementation only clears Order.discount (does not delete voucher data).
     */
    @Transactional
    public boolean removeVoucher(Long userId) {
        Order draft = orderRepo.findByUserIdAndStatus(userId, "DRAFT").orElse(null);
        if (draft == null) return false;
        draft.setDiscount(BigDecimal.ZERO);
        draft.setUpdatedAt(Instant.now());
        orderRepo.save(draft);
        return true;
    }

    // --- add to CartService (inside the class) ---

    /**
     * Ensure the user's draft order exists, recalc totals and persist.
     * Returns the persisted Order entity (fresh).
     *
     * Use when the caller needs entity-level access (e.g. CheckoutService).
     */
    @Transactional
    public Order refreshDraftAndGetEntity(Long userId) {
        Order draft = getOrCreateDraft(userId); // existing helper in your service
        // reuse private recalc(Order) logic which persists totals
        recalc(draft);
        // re-fetch or return draft (draft should be attached because we are in @Transactional)
        return draft;
    }

    /**
     * Ensure the user's draft order exists and return a CartDTO snapshot.
     * Prefer this when controller/service only needs DTO data.
     */
    @Transactional
    public com.fonestore.user_api.dto.cart.CartDTO refreshDraftAndGetDto(Long userId) {
        Order draft = refreshDraftAndGetEntity(userId);
        // toDTO uses itemRepo.findLinesWithInfo(draft.getId()) which you already have
        return toDTO(draft);
    }


}
