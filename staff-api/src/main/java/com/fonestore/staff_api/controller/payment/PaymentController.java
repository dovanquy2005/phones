package com.fonestore.staff_api.controller.payment;

import com.fonestore.staff_api.dto.payment.*;
import com.fonestore.staff_api.service.payment.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class PaymentController {

    private final PaymentService service;
    public PaymentController(PaymentService service) { this.service = service; }

    // FE có thể gọi để lấy payment kèm status
    @GetMapping("/orders/{orderId}/payment")
    public ResponseEntity<PaymentDTO> getByOrder(@PathVariable Long orderId){
        return ResponseEntity.ok(service.getByOrderId(orderId));
    }

    // Tạo/cập nhật bản ghi payment (không đổi status)
    @PostMapping("/orders/{orderId}/payment")
    public ResponseEntity<PaymentDTO> upsert(@PathVariable Long orderId,
                                                  @RequestBody PaymentUpsertRequest req){
        return ResponseEntity.ok(service.upsert(orderId, req));
    }

    // Đổi trạng thái thanh toán: { "status": "PAID" | "UNPAID" }
    // Khi PAID -> tự set orders.status = DELIVERED
    @PatchMapping("/orders/{orderId}/payment-status")
    public ResponseEntity<PaymentDTO> updateStatus(@PathVariable Long orderId,
                                                        @RequestBody PaymentStatusUpdate req){
        return ResponseEntity.ok(service.updateStatusByOrder(orderId, req));
    }
}
