package com.fonestore.staff_api.controller.auth;

import com.fonestore.staff_api.config.JwtUtil;
import com.fonestore.staff_api.dto.auth.LoginRequest;
import com.fonestore.staff_api.dto.auth.LoginResponse;
import com.fonestore.staff_api.entity.User;
import com.fonestore.staff_api.exception.BadRequestException;
import com.fonestore.staff_api.repository.UserRepository;
import com.fonestore.staff_api.service.StaffAuthService;  // <— dùng service riêng cho staff
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final StaffAuthService staffAuthService;   // <— thay vì UserService
    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    public AuthController(
            StaffAuthService staffAuthService,
            UserRepository userRepo,
            PasswordEncoder encoder,
            JwtUtil jwtUtil
    ) {
        this.staffAuthService = staffAuthService;
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
    }

    /* ====================== BUYER LOGIN ====================== */
    @PostMapping("/login")
    public ResponseEntity<?> loginBuyer(@RequestBody Map<String, String> req) {
        String email = (req.get("email") + "").trim().toLowerCase();
        String password = req.get("password");

        User u = userRepo.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        // Ưu tiên BCrypt, fallback SHA-256 (nâng cấp lên BCrypt nếu match)
        boolean ok = encoder.matches(password, u.getPasswordHash());
        if (!ok) {
            try {
                var md = java.security.MessageDigest.getInstance("SHA-256");
                var hex = java.util.HexFormat.of()
                        .formatHex(md.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                ok = hex.equalsIgnoreCase(u.getPasswordHash());
                if (ok) {
                    u.setPasswordHash(encoder.encode(password));
                    userRepo.save(u);
                }
            } catch (Exception ignored) {}
        }
        if (!ok) throw new BadRequestException("Invalid email or password");

        var claims = new HashMap<String, Object>();
        claims.put("uid", u.getId());
        claims.put("role", "user");
        claims.put("aud",  "buyer");

        String token = jwtUtil.generateToken(u.getEmail(), claims);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "user", Map.of(
                        "userId",  u.getId(),
                        "email",   u.getEmail(),
                        "fullName", u.getFullName()
                )
        ));
    }

    /* ====================== STAFF LOGIN (manager/staff) ====================== */
    @PostMapping("/staff/login")  // <— đúng: không lặp /api/auth
    public LoginResponse staffLogin(@RequestBody LoginRequest r) {
        return staffAuthService.login(r.email(), r.password());
    }

    /* ====================== REGISTER BUYER ====================== */
    public record RegisterRequest(
            String email, String password, String fullName,
            String phone, String dob, String gender) {}
    public record Msg(String message) {}

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepo.findByEmail(email).isPresent()) {
            return ResponseEntity.badRequest().body(new Msg("Email đã được sử dụng"));
        }

        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(req.password())); // BCrypt
        u.setFullName(req.fullName());
        u.setPhone(req.phone());
        if (req.dob() != null && !req.dob().isBlank()) {
            u.setDob(LocalDate.parse(req.dob())); // yyyy-MM-dd
        }
        u.setGender(req.gender());

        userRepo.save(u);

        return ResponseEntity.status(201).body(Map.of(
                "userId", u.getId(),
                "email",  u.getEmail()
        ));
    }

    /* ====================== AUTH ME (JWT) ====================== */
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        String email = String.valueOf(auth.getPrincipal());
        var u = userRepo.findByEmail(email).orElse(null);
        String name = (u != null && u.getFullName()!=null) ? u.getFullName() : email;
        String role = auth.getAuthorities().stream().findFirst().map(Object::toString).orElse("user");

        return ResponseEntity.ok(Map.of(
            "email", email,
            "name", name,
            "role", role,
            "userId", u != null ? u.getId() : null
        ));
    }
}
