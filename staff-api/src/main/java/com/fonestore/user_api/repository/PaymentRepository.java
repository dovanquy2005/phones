package com.fonestore.user_api.repository;

import com.fonestore.user_api.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByOrderIdOrderByCreatedAtAsc(Long orderId);
}
