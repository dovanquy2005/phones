package com.fonestore.staff_api.repository;

import com.fonestore.staff_api.entity.User;
import com.fonestore.staff_api.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {
    List<UserAddress> findByUser(User user);
    Optional<UserAddress> findByIdAndUser(Long id, User user);
    List<UserAddress> findByUserAndIsDefault(User user, boolean isDefault);
}
