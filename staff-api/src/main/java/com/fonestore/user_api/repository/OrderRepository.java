package com.fonestore.user_api.repository;

import com.fonestore.user_api.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // List theo user, có sẵn thứ tự mới nhất
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Lấy chi tiết kèm items, dùng DISTINCT để tránh nhân dòng
    @Query("""
           select distinct o
           from Order o
           left join fetch o.items i
           where o.id = :id
           order by i.id asc
           """)
    Optional<Order> findByIdFetchItems(@Param("id") Long id);

    // (Tuỳ chọn) Bảo mật: xem chi tiết đơn phải đúng user
    @Query("""
           select distinct o
           from Order o
           left join fetch o.items i
           where o.id = :id and o.userId = :userId
           order by i.id asc
           """)
    Optional<Order> findByIdAndUserIdFetchItems(@Param("id") Long id,
                                                @Param("userId") Long userId);

    // (Tuỳ chọn) Nếu muốn luôn load items cả khi list để tránh N+1:
    @EntityGraph(attributePaths = "items")
    List<Order> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
