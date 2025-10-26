
// repository/staff/StaffRepository.java
package com.fonestore.staff_api.repository.staff;

import com.fonestore.staff_api.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StaffRepository extends JpaRepository<Staff, Long> {
    boolean existsByEmail(String email);          // <- service đang gọi
    Optional<Staff> findByEmail(String email);
}
