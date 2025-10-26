package com.fonestore.staff_api.service;

import com.fonestore.staff_api.config.JwtUtil;
import com.fonestore.staff_api.dto.auth.LoginResponse;
import com.fonestore.staff_api.entity.Staff;
import com.fonestore.staff_api.exception.BadRequestException;
import com.fonestore.staff_api.repository.staff.StaffRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

@Service
public class StaffAuthService {

    private final StaffRepository repo;
    private final JwtUtil jwtUtil;

    public StaffAuthService(StaffRepository repo, JwtUtil jwtUtil) {
        this.repo = repo;
        this.jwtUtil = jwtUtil;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(String email, String rawPassword) {
        Staff staff = repo.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        // ✅ Hash & so sánh
        String inputHash = sha256(rawPassword);
        if (!inputHash.equals(staff.getPasswordHash())) {
            throw new BadRequestException("Invalid email or password");
        }

        // ✅ Sinh token với role tương ứng ("staff" / "manager")
        String token = jwtUtil.generateToken(
                staff.getEmail(),
                Map.of(
                        "uid", staff.getStaffId(),
                        "role", staff.getRole(),  // "staff" hoặc "manager"
                        "aud", "staff-panel"
                )
        );

        return new LoginResponse(staff.getStaffId(), staff.getEmail(), staff.getRole(), false, token);
    }

    private String sha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
