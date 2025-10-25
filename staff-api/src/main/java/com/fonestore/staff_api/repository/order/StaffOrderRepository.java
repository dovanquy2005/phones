// staff_api/repository/order/StaffOrderRepository.java
package com.fonestore.staff_api.repository.order;

import com.fonestore.user_api.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface StaffOrderRepository extends JpaRepository<Order, Long> {

      @Query("""
      select o from Order o
      where (:status is null or o.status = :status)
        and (:from  is null or o.createdAt >= :from)
        and (:to    is null or o.createdAt <  :to)
        and (
            :q is null
          or cast(o.id as string) like concat('%', :q, '%')
          or lower(cast(o.addressSnapshot as string)) like lower(concat('%', :q, '%'))
        )
      order by o.createdAt desc
      """)
      List<Order> search(@Param("status") String status,
                        @Param("from") Instant from,
                        @Param("to") Instant to,
                        @Param("q") String q);

}