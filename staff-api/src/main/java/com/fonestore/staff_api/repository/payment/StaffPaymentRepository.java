package com.fonestore.staff_api.repository.payment;

import com.fonestore.user_api.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StaffPaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);
    boolean existsByOrderId(Long orderId);

    // Lấy danh sách orderId đã PAID (dùng DISTINCT, không join sang Order)
    @Query("""
           select distinct p.orderId
           from Payment p
           where upper(p.status) = 'PAID' and p.orderId in :ids
           """)
    List<Long> findPaidOrderIds(@Param("ids") List<Long> ids);
}
