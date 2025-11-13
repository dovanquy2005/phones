package com.fonestore.user_api.controller;

import com.fonestore.user_api.service.CartService;
import com.fonestore.user_api.service.CheckoutService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fonestore.user_api.util.SecurityUtil;
import com.fonestore.user_api.dto.checkout.CheckoutDTO;
import com.fonestore.user_api.dto.checkout.PlaceOrderRequest;
import com.fonestore.user_api.dto.checkout.CheckoutOrderResponse;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CheckoutController {
    private final CheckoutService CheckoutService;
    private final SecurityUtil securityUtil;

    @GetMapping("/checkout")
    public ResponseEntity<CheckoutDTO> getCheckout(@RequestParam(required = false) Long userId) {
        Long uid = securityUtil.resolveUserId(userId);
        if (uid == null) return ResponseEntity.status(401).build();
        CheckoutDTO dto = CheckoutService.buildCheckout(uid);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/checkout")
    public ResponseEntity<CheckoutOrderResponse > placeOrder(@RequestParam(required = false) Long userId,
                                                    @RequestBody PlaceOrderRequest req) {
        Long uid = securityUtil.resolveUserId(userId);
        if (uid == null) return ResponseEntity.status(401).build();
        CheckoutOrderResponse  res = CheckoutService.placeOrder(uid, req);
        if (!res.isOk()) {
            if (res.getStatusCode() == 409) return ResponseEntity.status(409).body(res);
            return ResponseEntity.status(400).body(res);
        }
        return ResponseEntity.status(201).body(res);
    }
}
