package com.fonestore.staff_api.repository;

import com.fonestore.staff_api.entity.StockImei;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockImeiRepository extends JpaRepository<StockImei, Long> {}
