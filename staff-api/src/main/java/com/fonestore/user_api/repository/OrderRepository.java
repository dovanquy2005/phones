package com.fonestore.user_api.repository;

import com.fonestore.user_api.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // Phân trang theo user, mới nhất trước
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // Danh sách theo user, mới nhất trước
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Lấy chi tiết kèm items (tránh N+1)
    @EntityGraph(attributePaths = "items")
    Optional<Order> findById(Long id);

    // (Tuỳ chọn) Nếu muốn join fetch rõ ràng:
    @Query("""
           select distinct o
           from Order o
           left join fetch o.items i
           where o.id = :id
           """)
    Optional<Order> findByIdFetchItems(@Param("id") Long id);

    // (Tuỳ chọn) Bảo mật: chỉ lấy đơn thuộc đúng user
    @Query("""
           select distinct o
           from Order o
           left join fetch o.items i
           where o.id = :id and o.userId = :userId
           """)
    Optional<Order> findByIdAndUserIdFetchItems(@Param("id") Long id,
                                                @Param("userId") Long userId);
}
