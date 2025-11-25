package com.fonestore.staff_api.service;

import com.fonestore.staff_api.config.JwtUtil;
import com.fonestore.staff_api.dto.auth.LoginResponse;
import com.fonestore.staff_api.entity.Staff;
import com.fonestore.staff_api.exception.BadRequestException;
import com.fonestore.staff_api.repository.staff.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class StaffAuthService {

    private static final Pattern SHA256_HEX = Pattern.compile("(?i)^[0-9a-f]{64}$");
    private static final String[] BCRYPT_PREFIX = {"$2a$", "$2b$", "$2y$"};

    private final StaffRepository repo;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder; // BCryptPasswordEncoder bean

    @Transactional // cần ghi để auto-upgrade SHA256 -> bcrypt
    public LoginResponse login(String email, String rawPassword) {
        if (email == null || rawPassword == null || rawPassword.isBlank())
            throw new BadRequestException("Invalid email or password");

        Staff staff = repo.findByEmail(email.trim())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        // chặn tài khoản khóa
        if (Boolean.FALSE.equals(staff.isActive())) {
            throw new BadRequestException("Tài khoản đang bị khóa");
        }

        String stored = staff.getPasswordHash();
        if (stored == null || stored.isBlank())
            throw new BadRequestException("Invalid email or password");
        stored = stored.trim();

        boolean ok = false;

        // 1) ƯU TIÊN: bcrypt
        if (isBcrypt(stored)) {
            ok = passwordEncoder.matches(rawPassword, stored);
        }
        // 2) Hỗ trợ tài khoản CŨ: SHA-256 hex (không salt) -> cho login + nâng cấp sang bcrypt
        else if (isSha256Hex(stored)) {
            if (sha256(rawPassword).equalsIgnoreCase(stored)) {
                ok = true;
                staff.setPasswordHash(passwordEncoder.encode(rawPassword)); // upgrade
                repo.save(staff);
            }
        }

        if (!ok) throw new BadRequestException("Invalid email or password");

        String roleLower = staff.getRole() == null ? "staff" : staff.getRole().toLowerCase();
        String token = jwtUtil.generateToken(
                staff.getEmail(),
                Map.of(
                        "uid", staff.getStaffId(),
                        "role", roleLower,
                        "authorities", List.of(roleLower),
                        "aud", "staff-panel",
                        "email", staff.getEmail(),
                        "name", staff.getFullName() == null ? "" : staff.getFullName()
                )
        );

        return new LoginResponse(staff.getStaffId(), staff.getEmail(), roleLower, token);
    }

    // ===== helpers =====
    private static boolean isBcrypt(String s) {
        for (String p : BCRYPT_PREFIX) if (s.startsWith(p)) return true;
        return false;
    }
    private static boolean isSha256Hex(String s) { return SHA256_HEX.matcher(s).matches(); }

    private static String sha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
