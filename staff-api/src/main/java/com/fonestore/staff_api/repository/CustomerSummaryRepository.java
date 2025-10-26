package com.fonestore.staff_api.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import com.fonestore.staff_api.entity.User; // entity User hiện có của bạn

import java.util.List;

public interface CustomerSummaryRepository extends JpaRepository<User, Long> {

    @Query(value = """
        SELECT 
          u.user_id                               AS userId,
          u.full_name                             AS fullName,
          u.phone                                 AS phone,
          ISNULL(SUM(o.total), 0)                 AS totalSpent,
          COUNT(o.order_id)                       AS ordersCount,
          lastOrd.order_id                        AS lastOrderId,
          lastOrd.created_at                      AS lastOrderAt
        FROM [phone_store].[dbo].[users] u
        LEFT JOIN [phone_store].[dbo].[orders] o
               ON o.user_id = u.user_id
        OUTER APPLY (
          SELECT TOP 1 o2.order_id, o2.created_at
          FROM [phone_store].[dbo].[orders] o2
          WHERE o2.user_id = u.user_id
          ORDER BY o2.created_at DESC
        ) AS lastOrd
        WHERE (:kw IS NULL OR :kw = '' 
               OR u.full_name LIKE :kw OR u.phone LIKE :kw)
        GROUP BY u.user_id, u.full_name, u.phone, lastOrd.order_id, lastOrd.created_at
        ORDER BY u.user_id DESC
        """, nativeQuery = true)
    List<Object[]> findSummaries(@Param("kw") String likeKeyword);
}
