package com.fonestore.staff_api.repository;

import java.math.BigDecimal;
import java.util.List;

import com.fonestore.staff_api.entity.Variant;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VariantRepository extends JpaRepository<Variant, Long> {

    List<Variant> findByProduct_ProductId(Long productId);

    @Query(value = "SELECT MIN(list_price) FROM product_variants WHERE product_id = :pid", nativeQuery = true)
    BigDecimal findMinVariantPrice(@Param("pid") Long productId);
}
