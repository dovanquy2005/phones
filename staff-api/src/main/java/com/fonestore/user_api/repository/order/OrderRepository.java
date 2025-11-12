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

public interface OrderRepository extends JpaRepository<Order, Long> {

    // ========= Các method đang có (giữ nguyên) =========

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

    // Tìm 1 đơn theo user + status (đang dùng cho giỏ DRAFT)
    Optional<Order> findByUserIdAndStatus(Long userId, String status);



    // ========= Thêm mới (không đụng code cũ) =========

    // 1) Khoá bi quan khi thao tác giỏ hàng để tránh race-condition (add/update cùng lúc)
       @Lock(LockModeType.PESSIMISTIC_WRITE)
       @EntityGraph(attributePaths = "items")
       @Query("select o from Order o where o.userId = :userId and o.status = :status")
       Optional<Order> findByUserIdAndStatusForUpdateWithItems(@Param("userId") Long userId,
                                                               @Param("status") String status);


    // 2) Kiểm tra tồn tại giỏ DRAFT nhanh gọn
    boolean existsByUserIdAndStatus(Long userId, String status);

    // 3) Lấy giỏ DRAFT gần nhất nếu có nhiều (phòng TH dữ liệu cũ chưa clean)
    Optional<Order> findTopByUserIdAndStatusOrderByUpdatedAtDesc(Long userId, String status);

    // 4) Lấy danh sách đơn theo nhiều trạng thái (hữu ích cho lịch sử đơn hàng user)
    List<Order> findByUserIdAndStatusInOrderByCreatedAtDesc(Long userId, List<String> statuses);
}
