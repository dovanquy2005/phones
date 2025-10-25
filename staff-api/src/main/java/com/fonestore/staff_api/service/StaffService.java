package com.fonestore.staff_api.service;

import com.fonestore.staff_api.dto.staff.StaffCreateRequest;
import com.fonestore.staff_api.dto.staff.StaffResponse;
import com.fonestore.staff_api.entity.Staff;
import com.fonestore.staff_api.entity.User;
import com.fonestore.staff_api.exception.BadRequestException;
import com.fonestore.staff_api.exception.NotFoundException;
import com.fonestore.staff_api.repository.StaffRepository;
import com.fonestore.staff_api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service("staffStaffService")
public class StaffService {

    private final UserRepository userRepo;
    private final StaffRepository staffRepo;

    public StaffService(UserRepository userRepo, StaffRepository staffRepo) {
        this.userRepo = userRepo;
        this.staffRepo = staffRepo;
    }

    @Transactional
    public StaffResponse create(StaffCreateRequest r) {
        User user = userRepo.findById(r.userId()).orElseThrow(() -> new NotFoundException("User not found"));
        if (staffRepo.existsByEmail(user.getEmail())) throw new BadRequestException("Staff already exists");

        Staff s = new Staff();
        s.setEmail(user.getEmail());
        s.setPasswordHash(user.getPasswordHash());
        s.setFullName(user.getFullName());
        s.setRole("staff");
        s.setPosition(r.position());
        s = staffRepo.save(s);

        return new StaffResponse(s.getStaffId(), user.getId(), s.getEmail(), s.getPosition(), null, null);
    }

    @Transactional(readOnly = true)
    public StaffResponse getByUserId(Long userId) {
        User user = userRepo.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        Staff s = staffRepo.findByEmail(user.getEmail()).orElseThrow(() -> new NotFoundException("Staff profile not found"));
        return new StaffResponse(s.getStaffId(), user.getId(), s.getEmail(), s.getPosition(), null, null);
    }

    @Transactional
    public void deleteByUserId(Long userId) {
        User user = userRepo.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        Staff s = staffRepo.findByEmail(user.getEmail()).orElseThrow(() -> new NotFoundException("Staff profile not found"));
        staffRepo.delete(s);
    }

    @Transactional(readOnly = true)
    public Page<StaffResponse> list(Pageable pageable) {
        return staffRepo.findAll(pageable)
                .map(s -> new StaffResponse(s.getStaffId(), null, s.getEmail(), s.getPosition(), null, null));
    }
}
