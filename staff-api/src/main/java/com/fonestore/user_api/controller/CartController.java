package com.fonestore.user_api.controller;

import com.fonestore.user_api.dto.cart.*;
import com.fonestore.user_api.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;

import com.fonestore.user_api.dto.voucher.VoucherApplyRequest;
import com.fonestore.user_api.dto.voucher.VoucherApplyResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * CartController: robust resolveUserId WITHOUT compile-time dependency on org.springframework.security.oauth2.jwt.Jwt
 */

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(CartController.class);
    private final CartService cart;

    /**
     * Resolve user id from either:
     *  - provided userId param (but only accepted if it matches token principal)
     *  - or, resolve from SecurityContextHolder Authentication principal (Jwt via reflection, UserDetails, Map, String, Number)
     *
     * Returns null if unable to resolve or not authenticated.
     */
    private Long resolveUserId(Long userIdParam) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;

        Object principal = auth.getPrincipal();
        Long resolved = null;

        try {
            // 0) If principal is a Number (e.g. Long), accept it directly
            if (principal instanceof Number) {
                resolved = ((Number) principal).longValue();
            }

            // 1) Try reflectively detect Jwt class if present
            if (resolved == null) {
                try {
                    Class<?> jwtClass = Class.forName("org.springframework.security.oauth2.jwt.Jwt");
                    if (jwtClass.isInstance(principal)) {
                        try {
                            Method getClaimAsString = jwtClass.getMethod("getClaimAsString", String.class);
                            Object claimUserId = null;
                            try { claimUserId = getClaimAsString.invoke(principal, "userId"); } catch (Throwable ignore) {}
                            if (claimUserId == null) {
                                try { claimUserId = getClaimAsString.invoke(principal, "uid"); } catch (Throwable ignore) {}
                            }
                            if (claimUserId == null) {
                                try { claimUserId = getClaimAsString.invoke(principal, "id"); } catch (Throwable ignore) {}
                            }
                            if (claimUserId == null) {
                                // fallback to getSubject()
                                try {
                                    Method getSub = jwtClass.getMethod("getSubject");
                                    claimUserId = getSub.invoke(principal);
                                } catch (Throwable ignore) {}
                            }
                            resolved = parseLongClaim(claimUserId);
                        } catch (NoSuchMethodException nsme) {
                            // ignore
                        }
                    }
                } catch (ClassNotFoundException cnfe) {
                    // Jwt class not present - skip
                }
            }

            // 2) If still null and principal is UserDetails (or custom)
            if (resolved == null && principal != null && principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                try {
                    Method m = principal.getClass().getMethod("getId");
                    Object idVal = m.invoke(principal);
                    resolved = parseLongClaim(idVal);
                } catch (NoSuchMethodException nsme) {
                    resolved = parseLongClaim(auth.getName());
                }
            }

            // 3) principal is Map-like (claims)
            if (resolved == null && principal instanceof Map) {
                Map<?,?> map = (Map<?,?>) principal;
                Object idVal = map.get("userId");
                if (idVal == null) idVal = map.get("uid");
                if (idVal == null) idVal = map.get("user_id");
                if (idVal == null) idVal = map.get("id");
                if (idVal == null) idVal = map.get("sub");
                resolved = parseLongClaim(idVal);
            }

            // 4) principal is String
            if (resolved == null && principal instanceof String) {
                resolved = parseLongClaim((String) principal);
                if (resolved == null) resolved = parseLongClaim(auth.getName());
            }

        } catch (Exception ignored) {
            // swallow and proceed to final checks
        }

        // If client provided userId param, require it to match resolved id.
        if (userIdParam != null) {
            if (resolved == null) return null;
            if (!userIdParam.equals(resolved)) return null;
            return resolved;
        }

        return resolved;
    }

    // utility to convert claim into Long (handles Number, numeric String, etc.)
    private Long parseLongClaim(Object claim) {
        if (claim == null) return null;
        if (claim instanceof Number) {
            return ((Number) claim).longValue();
        }
        try {
            String s = String.valueOf(claim).trim();
            if (s.isEmpty()) return null;
            return Long.valueOf(s);
        } catch (Exception e) {
            return null;
        }
    }
    
    private String extractBearer(HttpServletRequest request) {
        if (request == null) return null;
        String h = request.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) return h.substring(7);
        return null;
    }
    @GetMapping
    public ResponseEntity<CartDTO> getCart(@RequestParam(required = false) Long userId) {
        Long uid = resolveUserId(userId);
        if (uid == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(cart.getCart(uid));
    }

    @PostMapping("/items")
    public ResponseEntity<CartDTO> addItem(@RequestParam(required = false) Long userId,
                                           @RequestBody @Valid AddItemRequest req) {
        Long uid = resolveUserId(userId);
        if (uid == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(cart.addItem(uid, req));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartDTO> updateItem(@RequestParam(required = false) Long userId,
                                              @PathVariable Long itemId,
                                              @RequestBody @Valid UpdateItemRequest req) {
        Long uid = resolveUserId(userId);
        if (uid == null) return ResponseEntity.status(401).build();
        UpdateItemRequest newReq = new UpdateItemRequest(itemId, req.qty());
        return ResponseEntity.ok(cart.updateItem(uid, newReq));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartDTO> removeItem(@RequestParam(required = false) Long userId,
                                              @PathVariable Long itemId) {
        Long uid = resolveUserId(userId);
        if (uid == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(cart.removeItem(uid, itemId));
    }

    @PostMapping("/clear")
    public ResponseEntity<CartDTO> clear(@RequestParam(required = false) Long userId) {
        Long uid = resolveUserId(userId);
        if (uid == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(cart.clear(uid));
    }

    //  @PostMapping("/checkout")
    
    //  public ResponseEntity<CartDTO> checkout(@RequestParam(required = false) Long userId) {
    //      Long uid = resolveUserId(userId);
    //      if (uid == null) return ResponseEntity.status(401).build();
    //      return ResponseEntity.ok(cart.checkout(uid));
    //  }

    @PostMapping("/merge")
    public ResponseEntity<CartDTO> merge(@RequestParam(required = false) Long userId,
                                        @RequestBody @Valid MergeCartRequest req) {
        // --- DEBUG: inspect SecurityContext for this request ---
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        logger.debug("CartController.merge - Authentication: {}, principal: {}, authorities: {}",
                    auth, auth == null ? null : auth.getPrincipal(), auth == null ? null : auth.getAuthorities());
        // -------------------------------------------------------

        Long uid = resolveUserId(userId);
        if (uid == null) {
            logger.debug("CartController.merge - cannot resolve userId, returning 401");
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(cart.merge(uid, req));
    }
    //======== voucher =========
    /**
     * POST /api/cart/voucher/apply
     * Body: { "code": "SUMMER2025" }
     * Optional: ?userId=123 (dev); otherwise rely on JWT principal via resolveUserId
     */
    @PostMapping("/voucher/apply")
    public ResponseEntity<VoucherApplyResponse> applyVoucher(@RequestParam(required = false) Long userId,
                                                            @RequestBody @Valid VoucherApplyRequest req,
                                                            HttpServletRequest request) {
        Long uid = resolveUserId(userId);
        if (uid == null) return ResponseEntity.status(401).build();

        // optional: forward bearer if you want cartService to use it (you can ignore)
        String bearer = extractBearer(request);

        VoucherApplyResponse res;
        try {
            res = cart.applyVoucher(uid, req, bearer);
        } catch (Exception ex) {
            logger.error("applyVoucher error for user {} code {}: {}", uid, req == null ? null : req.getCode(), ex.getMessage(), ex);
            return ResponseEntity.status(500).body(VoucherApplyResponse.error("Internal error"));
        }

        if (res == null) return ResponseEntity.status(500).body(VoucherApplyResponse.error("Internal error"));
        if (res.isOk()) return ResponseEntity.ok(res);
        // invalid voucher -> 400 with detail from service
        return ResponseEntity.status(400).body(res);
    }



    /**
     * DELETE /api/cart/voucher
     * Query: ?userId=123 (optional dev helper)
     */
    @DeleteMapping("/voucher")
    public ResponseEntity<Map<String,Object>> removeVoucher(@RequestParam(required = false) Long userId,
                                                        HttpServletRequest request) {
        Long uid = resolveUserId(userId);
        if (uid == null) return ResponseEntity.status(401).build();

        // lấy token (nếu muốn log hoặc forward). service hiện tại không cần token.
       // String bearer = extractBearer(request);

        boolean ok;
        try {
            // dùng signature hiện tại của service. Nếu bạn thay service để nhận bearer, đổi dòng này thành:
            // ok = cart.removeVoucher(uid, bearer);
            ok = cart.removeVoucher(uid);
        } catch (Exception ex) {
            logger.error("removeVoucher error for user {}: {}", uid, ex.getMessage(), ex);
            return ResponseEntity.status(500).body(Map.of("ok", false, "message", "Internal error"));
        }

        if (ok) return ResponseEntity.ok(Map.of("ok", true));
        return ResponseEntity.status(404).body(Map.of("ok", false, "message", "Cart not found"));
    }


}
