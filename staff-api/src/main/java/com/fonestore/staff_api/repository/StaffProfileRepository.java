package com.fonestore.staff_api.repository;

import com.fonestore.staff_api.entity.StaffProfile;
import com.fonestore.staff_api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StaffProfileRepository extends JpaRepository<StaffProfile, Long> {
    Optional<StaffProfile> findByUser(User user);
    boolean existsByUser(User user);
}
