package com.fonestore.staff_api.service;

import com.fonestore.staff_api.config.JwtUtil;
import com.fonestore.staff_api.dto.auth.LoginResponse;
import com.fonestore.staff_api.dto.user.TwoFAResponse;
import com.fonestore.staff_api.dto.user.UserCreateRequest;
import com.fonestore.staff_api.dto.user.UserResponse;
import com.fonestore.staff_api.dto.user.UserUpdateRequest;
import com.fonestore.staff_api.entity.User;
import com.fonestore.staff_api.exception.BadRequestException;
import com.fonestore.staff_api.exception.NotFoundException;
import com.fonestore.staff_api.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;

@Service("staffUserService")
public class UserAuthService {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepo;
    private static final SecureRandom RNG = new SecureRandom();

    public UserAuthService(UserRepository userRepo, JwtUtil jwtUtil) {
        this.userRepo = userRepo;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public UserResponse create(UserCreateRequest r) {
        userRepo.findByEmail(r.email()).ifPresent(u -> { throw new BadRequestException("Email already exists"); });

        User u = new User();
        u.setEmail(r.email());
        u.setPasswordHash(hashPassword(r.password()));
        u.setFullName(r.fullName());
        u.setPhone(r.phone());
        u.setDob(parseDob(r.dob()));
        u.setGender(r.gender());
        u.setAddress(r.address());
        u.setRole("user");
        u = userRepo.save(u);

        return toResp(u);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> list(String roleIgnored, Pageable pageable) {
        Page<User> page = userRepo.findAll(pageable);
        return page.map(this::toResp);
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest r) {
        User u = userRepo.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
        if (r.fullName() != null) u.setFullName(r.fullName());
        if (r.phone() != null) u.setPhone(r.phone());
        if (r.dob() != null) u.setDob(parseDob(r.dob()));
        if (r.gender() != null) u.setGender(r.gender());
        if (r.address() != null) u.setAddress(r.address());
        
        u = userRepo.save(u);
        return toResp(u);
    }

    @Transactional
    public void delete(Long id) {
        if (!userRepo.existsById(id)) throw new NotFoundException("User not found");
        userRepo.deleteById(id);
    }

    @Transactional
    public TwoFAResponse enable2FA(Long userId, String issuer) {
        User u = userRepo.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        String secret = generateBase32Secret(16);
        u.setTwofaSecret(secret);
        userRepo.save(u);

        String label = u.getEmail();
        String otpauth = "otpauth://totp/" + urlEncode(issuer) + ":" + urlEncode(label)
                + "?secret=" + secret + "&issuer=" + urlEncode(issuer) + "&algorithm=SHA1&digits=6&period=30";
        return new TwoFAResponse(u.getId(), secret, otpauth);
    }

    @Transactional
    public void disable2FA(Long userId) {
        User u = userRepo.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        u.setTwofaSecret(null);
        userRepo.save(u);
    }

    private String hashPassword(String plain) {
        if (plain == null) throw new BadRequestException("Password is required");
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest(plain.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private LocalDate parseDob(String dobStr) {
        if (dobStr == null || dobStr.isBlank()) return null;
        try { return LocalDate.parse(dobStr); }
        catch (DateTimeParseException e) { throw new BadRequestException("dob must be yyyy-MM-dd"); }
    }

    private String generateBase32Secret(int len) {
        final char[] alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append(alphabet[RNG.nextInt(alphabet.length)]);
        return sb.toString();
    }

    private String urlEncode(String s) {
        return java.net.URLEncoder.encode(s == null ? "" : s, java.nio.charset.StandardCharsets.UTF_8);
    }

    private UserResponse toResp(User u) {
        return new UserResponse(
                u.getId(),
                u.getEmail(),
                u.getFullName(),
                u.getPhone(),
                u.getDob() != null ? u.getDob().toString() : null,
                u.getGender(),
                u.getRole(),
                u.getTwofaSecret() != null,
                u.getAddress()
        );
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        User u = userRepo.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
        return toResp(u);
    }

    // @Transactional
    // public void changePassword(Long id, String oldPassword, String newPassword) {
    //     User u = userRepo.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
    //     String oldHash = hashPassword(oldPassword);
    //     if (!oldHash.equals(u.getPasswordHash())) {
    //         throw new BadRequestException("Old password is incorrect");
    //     }
    //     u.setPasswordHash(hashPassword(newPassword));
    //     userRepo.save(u);
    // }

    @Transactional(readOnly = true)
    public com.fonestore.staff_api.dto.auth.LoginResponse login(String email, String password) {
        User u = userRepo.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        String inputHash = hashPassword(password);
        if (!inputHash.equals(u.getPasswordHash())) {
            throw new BadRequestException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(
                u.getEmail(),
                java.util.Map.of("uid", u.getId(), "role", "user", "aud", "buyer")
        );
        return new LoginResponse(u.getId(), u.getEmail(), "user", u.getTwofaSecret() != null, token);
    }
}
