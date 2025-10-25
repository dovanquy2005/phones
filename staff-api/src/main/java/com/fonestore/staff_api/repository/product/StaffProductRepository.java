package com.fonestore.staff_api.repository.product;

import com.fonestore.staff_api.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffProductRepository extends JpaRepository<Product, Long> {
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Long id);
}
