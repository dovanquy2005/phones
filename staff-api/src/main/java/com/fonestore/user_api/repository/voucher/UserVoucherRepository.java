package com.fonestore.user_api.repository.voucher;

import com.fonestore.staff_api.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserVoucherRepository extends JpaRepository<Voucher, Long> {
    Optional<Voucher> findByCodeNorm(String codeNorm);
    Optional<Voucher> findByCode(String code);
}
