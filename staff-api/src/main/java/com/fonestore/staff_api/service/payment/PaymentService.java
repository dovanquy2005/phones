package com.fonestore.staff_api.service.payment;

import com.fonestore.staff_api.dto.payment.*;
import com.fonestore.user_api.entity.Payment;
import com.fonestore.user_api.repository.order.OrderRepository;
import com.fonestore.staff_api.entity.enums.PaymentStatus;
import com.fonestore.staff_api.exception.BadRequestException;
import com.fonestore.staff_api.exception.NotFoundException;
import com.fonestore.staff_api.repository.payment.StaffPaymentRepository;
import com.fonestore.user_api.entity.Order;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Locale;

@Service
public class PaymentService {

    private final StaffPaymentRepository payRepo;
    private final OrderRepository orderRepo;

    public PaymentService(StaffPaymentRepository payRepo, OrderRepository orderRepo) {
        this.payRepo = payRepo;
        this.orderRepo = orderRepo;
    }

    private static String normUpper(String s){
        return s==null? null : s.trim().toUpperCase(Locale.ROOT);
    }

    private PaymentDTO toResp(Payment p){
        return new PaymentDTO(
                p.getId(), p.getOrderId(), p.getMethod(),
                p.getAmount(), p.getStatus(), p.getTxnRef(), p.getCreatedAt()
        );
    }

    public PaymentDTO getByOrderId(Long orderId){
        Payment p = payRepo.findByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("Payment not found for order " + orderId));
        return toResp(p);
    }

    @Transactional
    public PaymentDTO upsert(Long orderId, PaymentUpsertRequest req){
        Order ord = orderRepo.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

        Payment p = payRepo.findByOrderId(orderId).orElseGet(Payment::new);
        p.setOrderId(orderId);
        p.setMethod((req.method()==null || req.method().isBlank()) ? "cod" : req.method().trim().toLowerCase());
        BigDecimal amount = req.amount()!=null ? req.amount() :
                (ord.getTotal()!=null ? ord.getTotal() : BigDecimal.ZERO);
        p.setAmount(amount);
        if (p.getStatus() == null) p.setStatus(PaymentStatus.UNPAID);
        if (req.txnRef()!=null && !req.txnRef().isBlank()) p.setTxnRef(req.txnRef().trim());

        payRepo.save(p);
        return toResp(p);
    }

    @Transactional
    public PaymentDTO updateStatusByOrder(Long orderId, PaymentStatusUpdate req){
        if (req == null || req.status()==null) throw new BadRequestException("Missing status");
        final String v = normUpper(req.status());
        final PaymentStatus target = "PAID".equals(v) ? PaymentStatus.PAID : PaymentStatus.UNPAID;

        Order ord = orderRepo.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

        // Nếu đơn đã hủy thì không cho thu tiền
        final String ordStatus = normUpper(ord.getStatus());
        if ("CANCELED".equals(ordStatus) && target == PaymentStatus.PAID) {
            throw new BadRequestException("Order was canceled; cannot mark as PAID");
        }

        Payment p = payRepo.findByOrderId(orderId).orElseGet(() -> {
            Payment n = new Payment();
            n.setOrderId(orderId);
            n.setMethod("cod");
            n.setAmount(ord.getTotal()!=null ? ord.getTotal() : BigDecimal.ZERO);
            n.setStatus(PaymentStatus.UNPAID);
            return n;
        });

        p.setStatus(target);
        payRepo.save(p);

        // Rule: khi PAID => tự chuyển đơn sang DELIVERED (nếu chưa)
        if (target == PaymentStatus.PAID && !"DELIVERED".equals(ordStatus)) {
            ord.setStatus("DELIVERED");
            orderRepo.save(ord);
        }

        return toResp(p);
    }
}
