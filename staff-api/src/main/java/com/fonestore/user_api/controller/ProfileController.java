package com.fonestore.user_api.controller;

import com.fonestore.staff_api.entity.User;
import com.fonestore.staff_api.repository.UserRepository;
import com.fonestore.user_api.dto.profile.UpdateProfileRequest;
import com.fonestore.user_api.dto.profile.UserProfileDTO;
import com.fonestore.user_api.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/profile") // Endpoint này được bảo vệ bởi UserSecurityConfig
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository userRepo;
    private final SecurityUtil securityUtil;

    // Helper: Map Entity -> DTO
    private UserProfileDTO toDTO(User u) {
        return new UserProfileDTO(
                u.getId(),
                u.getEmail(),
                u.getFullName(),
                u.getPhone(),
                u.getDob(),
                u.getGender(),
                u.getAddress()
        );
    }

    // 1. Xem hồ sơ
    @GetMapping
    public ResponseEntity<UserProfileDTO> getMyProfile() {
        Long userId = securityUtil.resolveUserId(null); // Lấy ID từ token
        if (userId == null) return ResponseEntity.status(401).build();

        User u = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(toDTO(u));
    }

    // 2. Cập nhật hồ sơ
    @PutMapping
    public ResponseEntity<UserProfileDTO> updateMyProfile(@RequestBody UpdateProfileRequest req) {
        Long userId = securityUtil.resolveUserId(null);
        if (userId == null) return ResponseEntity.status(401).build();

        User u = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Cập nhật từng trường nếu có gửi lên
        if (req.fullName() != null) u.setFullName(req.fullName().trim());
        if (req.phone() != null)    u.setPhone(req.phone().trim());
        if (req.dob() != null)      u.setDob(req.dob());
        if (req.gender() != null)   u.setGender(req.gender());
        if (req.address() != null)  u.setAddress(req.address().trim());

        userRepo.save(u);

        return ResponseEntity.ok(toDTO(u));
    }
}