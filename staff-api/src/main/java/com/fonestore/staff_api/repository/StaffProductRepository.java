package com.fonestore.staff_api.repository;
import com.fonestore.staff_api.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
public interface StaffProductRepository extends JpaRepository<Product, Long> { }
