package com.fonestore.user_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fonestore.user_api.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {}
