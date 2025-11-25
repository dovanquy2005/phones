package com.fonestore.user_api.repository.voucher;

import com.fonestore.user_api.entity.VoucherUsage;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserVoucherUsageRepository extends JpaRepository<VoucherUsage, Long> {
    long countByVoucherId(Long voucherId);
    long countByVoucherIdAndUserId(Long voucherId, Long userId);
    // Thêm dòng này để tìm Voucher theo ID đơn hàng
    Optional<VoucherUsage> findByOrderId(Long orderId);
}
