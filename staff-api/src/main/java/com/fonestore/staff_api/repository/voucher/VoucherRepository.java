package com.fonestore.staff_api.repository.voucher;

import com.fonestore.staff_api.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import org.springframework.data.repository.query.Param;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {
  boolean existsByCode(String code); // vẫn giữ

@Query("select (count(v) > 0) from Voucher v where v.code = :code")
boolean existsExact(@Param("code") String code);

  Optional<Voucher> findByCode(String code);
}