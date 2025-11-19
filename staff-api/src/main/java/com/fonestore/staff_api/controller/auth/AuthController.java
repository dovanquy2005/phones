package com.fonestore.staff_api.controller.auth;

import com.fonestore.staff_api.config.JwtUtil;
import com.fonestore.staff_api.dto.auth.LoginRequest;
import com.fonestore.staff_api.dto.auth.LoginResponse;
import com.fonestore.staff_api.entity.User;
import com.fonestore.staff_api.exception.BadRequestException;
import com.fonestore.staff_api.repository.UserRepository;
import com.fonestore.staff_api.service.StaffAuthService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

/**
 * AuthController: buyer login / register / me
 * - loginBuyer: tạo token bằng jwtUtil.generateTokenForUser(...) để đảm bảo "userId" và "uid" có trong payload
 * - me: cố gắng lấy userId từ principal hoặc details; fallback tìm user bằng email nếu cần
 */
@RestController
@RequestMapping("/api/auth")
// @CrossOrigin(origins = "*") // <--- THÊM DÒNG NÀY (Cho phép mọi nơi gọi vào)
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

        // tạo token: dùng helper để đảm bảo userId & uid có trong claims
        // role mặc định "user" (thay đổi nếu bạn lưu role ở entity)
        String role = u.getRole() != null && !u.getRole().isBlank() ? u.getRole() : "user";
        String token = jwtUtil.generateTokenForUser(u.getEmail(), u.getId(), role);
        
        Map<String,Object> resp = new HashMap<>();
        resp.put("token", token);
        resp.put("user", Map.of(
                "userId",  u.getId(),
                "email",   u.getEmail(),
                "fullName", u.getFullName()
        ));

        return ResponseEntity.ok(resp);
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
        // u.setRole("user");
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
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthenticated"));
        }

        Long uid = null;
        String email = null;

        Object principal = auth.getPrincipal();
        if (principal instanceof Number) {
            uid = ((Number) principal).longValue();
        } else if (principal instanceof String) {
            String p = (String) principal;
            if (p.contains("@")) {
                email = p;
            } else {
                try { uid = Long.valueOf(p); } catch (Exception ignored) {}
            }
        } else if (principal instanceof Map) {
            // in case principal is a Map set by the filter
            Object idObj = ((Map<?,?>) principal).get("id");
            if (idObj instanceof Number) uid = ((Number) idObj).longValue();
            else if (idObj instanceof String) {
                try { uid = Long.valueOf((String) idObj); } catch (Exception ignored) {}
            }
            if (email == null) {
                Object em = ((Map<?,?>) principal).get("email");
                if (em != null) email = String.valueOf(em);
            }
        } else {
            // add other principal types if you use a CustomUserPrincipal class
            // e.g. if (principal instanceof CustomUserPrincipal) { uid = ((CustomUserPrincipal)principal).getId(); ... }
        }

        // fallback: try details
        Object details = auth.getDetails();
        if (uid == null && details instanceof Map) {
            Map<?,?> m = (Map<?,?>) details;
            Object v = m.get("userId");
            if (v == null) v = m.get("uid");
            if (v == null) v = m.get("id");
            if (v instanceof Number) uid = ((Number) v).longValue();
            else if (v instanceof String) {
                try { uid = Long.valueOf((String) v); } catch (Exception ignored) {}
            }
            if (email == null) {
                Object em = m.get("email");
                if (em == null) em = m.get("sub");
                if (em != null) email = String.valueOf(em);
            }
        }

        if (uid == null && email == null) {
            // cannot determine user identity
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Cannot determine user identity"));
        }

        User u = null;
        if (uid != null) {
            u = userRepo.findById(uid).orElse(null);
        } else if (email != null) {
            u = userRepo.findByEmail(email).orElse(null);
        }

        // build response using HashMap so null values do NOT throw NPE
        Map<String, Object> resp = new HashMap<>();
        // prefer DB email if we have a user
        String outEmail = (u != null && u.getEmail() != null) ? u.getEmail() : email;
        resp.put("email", outEmail); // may be null but HashMap allows it; JSON serializer will include null
        String name = (u != null && u.getFullName() != null) ? u.getFullName() : (outEmail != null ? outEmail : "unknown");
        resp.put("name", name);

        // role: try to map authorities to a readable string
        String role = auth.getAuthorities().stream().findFirst().map(Object::toString).orElse("user");
        resp.put("role", role);

        // userId: prefer DB id if present, else uid (could be null)
        Long outUserId = (u != null && u.getId() != null) ? u.getId() : uid;
        resp.put("userId", outUserId);

        // If user not found, maybe return 404 or still 200 with partial info — choose policy you want:
        if (u == null) {
            // Option A: return 404
            // return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
            // Option B: return 200 with available info (below we return 200)
        }

        return ResponseEntity.ok(resp);
    }

}
