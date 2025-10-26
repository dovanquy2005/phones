package com.fonestore.staff_api.service;

import com.fonestore.staff_api.dto.staff.*;
import com.fonestore.staff_api.entity.Staff;
import com.fonestore.staff_api.exception.BadRequestException;
import com.fonestore.staff_api.exception.NotFoundException;
import com.fonestore.staff_api.repository.staff.StaffRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Locale;
import java.util.Objects;

@Service("staffStaffService")
public class StaffService {

    private final StaffRepository staffRepo;
    private final PasswordEncoder encoder;

    public StaffService(StaffRepository staffRepo, PasswordEncoder encoder) {
        this.staffRepo = staffRepo;
        this.encoder = encoder;
    }

    private static String normEmail(String email){
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
    private static String normRole(String role){
        if (role == null || role.isBlank()) return "staff";
        String r = role.trim().toLowerCase(Locale.ROOT);
        if (!r.equals("staff") && !r.equals("manager"))
            throw new BadRequestException("role phải là 'staff' hoặc 'manager'");
        return r;
    }
    private static String normPhoneVN(String raw){
        if (raw == null) return null;
        String s = raw.replaceAll("[^\\d+]", "").trim();
        if (s.startsWith("+84")) s = "0" + s.substring(3);
        if (s.length() >= 11 && s.startsWith("84")) s = "0" + s.substring(2);
        return s;
    }
    private static boolean normActive(Boolean v){
        return v == null || Boolean.TRUE.equals(v);
    }
    private static StaffResponse toRes(Staff s){
        return new StaffResponse(
                s.getStaffId(),
                s.getEmail(),
                s.getFullName(),
                s.getRole(),
                s.getPosition(),
                s.getPhone(),
                s.isActive(),
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }

    /* ===== CREATE ===== */
    @Transactional
    public StaffResponse create(StaffCreateRequest r) {
        String email = normEmail(r.email());
        if (email == null || email.isBlank()) throw new BadRequestException("Email là bắt buộc");
        if (staffRepo.existsByEmail(email)) throw new BadRequestException("Email đã tồn tại");

        Staff s = new Staff();
        s.setEmail(email);
        s.setFullName(r.fullName().trim());
        s.setRole(normRole(r.role()));
        s.setPosition(r.position());
        s.setPhone(normPhoneVN(r.phone()));
        s.setActive(normActive(r.isActive()));

        if (r.password() == null || r.password().length() < 6)
            throw new BadRequestException("password tối thiểu 6 ký tự");
        s.setPasswordHash(encoder.encode(r.password()));

        try {
            s = staffRepo.save(s);
        } catch (DataIntegrityViolationException ex) {
            throw new BadRequestException("Email đã tồn tại");
        }
        return toRes(s);
    }

    /* ===== LIST (PAGE) ===== */
    @Transactional(readOnly = true)
    public Page<StaffResponse> list(Pageable pageable) {
        return staffRepo.findAll(pageable).map(StaffService::toRes);
    }

    /* ===== GET ===== */
    @Transactional(readOnly = true)
    public StaffResponse get(Long staffId){
        var s = staffRepo.findById(staffId)
                .orElseThrow(() -> new NotFoundException("Staff không tồn tại"));
        return toRes(s);
    }

    /* ===== UPDATE (PATCH/PUT) ===== */
    @Transactional
    public StaffResponse update(Long staffId, StaffUpdateRequest r){
        var s = staffRepo.findById(staffId)
                .orElseThrow(() -> new NotFoundException("Staff không tồn tại"));

        if (r.email()!=null && !r.email().isBlank()){
            String newEmail = normEmail(r.email());
            if (!Objects.equals(newEmail, s.getEmail()) && staffRepo.existsByEmail(newEmail))
                throw new BadRequestException("Email đã tồn tại");
            s.setEmail(newEmail);
        }
        if (r.fullName()!=null && !r.fullName().isBlank())
            s.setFullName(r.fullName().trim());

        if (r.role()!=null && !r.role().isBlank())
            s.setRole(normRole(r.role()));

        if (r.position()!=null)
            s.setPosition(r.position());

        if (r.phone()!=null)
            s.setPhone(normPhoneVN(r.phone()));

        if (r.isActive()!=null)
            s.setActive(r.isActive());

        if (r.password()!=null && !r.password().isBlank())
            s.setPasswordHash(encoder.encode(r.password()));

        return toRes(s);
    }

    /* ===== DELETE ===== */
    @Transactional
    public void delete(Long staffId){
        if (!staffRepo.existsById(staffId))
            throw new NotFoundException("Staff không tồn tại");
        staffRepo.deleteById(staffId);
    }
}
