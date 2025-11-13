package com.fonestore.user_api.repository.order;

import com.fonestore.user_api.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType; // dùng jakarta vì project đang dùng jakarta.*

import java.util.List;
import java.util.Optional;

public interface UserOrderRepository extends JpaRepository<Order, Long> {

    // ======= Paging / Non-paging standard methods =======

    // Phân trang theo user, mới nhất trước
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // Danh sách theo user (không phân trang), mới nhất trước
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Danh sách theo user + status (không phân trang) — TÊN CHUẨN
    List<Order> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);

    /* Nếu bạn cần tương thích ngược với tên cũ (không bắt buộc),
       bạn có thể bật method có @Query này — comment/ bỏ nếu không cần.
    @Query("""
           select o
           from Order o
           where o.userId = :userId
             and (:status is null or o.status = :status)
           order by o.createdAt desc
           """)
    List<Order> findByUserIdAndStatusOrderByCreatedAtDescNoPagenation(
            @Param("userId") Long userId,
            @Param("status") String status);
    */

    // ======= Detail / fetch helpers =======

    @EntityGraph(attributePaths = "items")
    Optional<Order> findById(Long id);

    @Query("""
           select distinct o
           from Order o
           left join fetch o.items i
           where o.id = :id
           """)
    Optional<Order> findByIdFetchItems(@Param("id") Long id);

    @Query("""
           select distinct o
           from Order o
           left join fetch o.items i
           where o.id = :id and o.userId = :userId
           """)
    Optional<Order> findByIdAndUserIdFetchItems(@Param("id") Long id,
                                                @Param("userId") Long userId);

    Optional<Order> findByUserIdAndStatus(Long userId, String status);

    // ======= Concurrency / utility methods =======

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "items")
    @Query("select o from Order o where o.userId = :userId and o.status = :status")
    Optional<Order> findByUserIdAndStatusForUpdateWithItems(@Param("userId") Long userId,
                                                            @Param("status") String status);

    boolean existsByUserIdAndStatus(Long userId, String status);

    Optional<Order> findTopByUserIdAndStatusOrderByUpdatedAtDesc(Long userId, String status);

    List<Order> findByUserIdAndStatusInOrderByCreatedAtDesc(Long userId, List<String> statuses);
}
