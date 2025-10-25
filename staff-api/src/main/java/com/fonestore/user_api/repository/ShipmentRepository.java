package com.fonestore.user_api.repository;

import com.fonestore.user_api.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    List<Shipment> findByOrderIdOrderByCreatedAtAsc(Long orderId);
}
