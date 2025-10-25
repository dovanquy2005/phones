package com.fonestore.user_api.repository;

import com.fonestore.staff_api.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProductRepository extends JpaRepository<Product, Long> {
}
