package com.fonestore.staff_api.service;

import com.fonestore.staff_api.dto.staff.StaffCreateRequest;
import com.fonestore.staff_api.dto.staff.StaffResponse;
import com.fonestore.staff_api.entity.StaffProfile;
import com.fonestore.staff_api.entity.User;
import com.fonestore.staff_api.exception.BadRequestException;
import com.fonestore.staff_api.exception.NotFoundException;
import com.fonestore.staff_api.repository.StaffProfileRepository;
import com.fonestore.staff_api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;



@Service("staffStaffService")
public class StaffService {

    private final UserRepository userRepo;
    private final StaffProfileRepository staffRepo;

    public StaffService(UserRepository userRepo, StaffProfileRepository staffRepo) {
        this.userRepo = userRepo; this.staffRepo = staffRepo;
    }

    @Transactional
    public StaffResponse create(StaffCreateRequest r) {
        User user = userRepo.findById(r.userId()).orElseThrow(() -> new NotFoundException("User not found"));
        if (!"staff".equalsIgnoreCase(user.getRole())) {
            throw new BadRequestException("User role must be 'staff' before creating staff profile");
        }
        if (staffRepo.existsByUser(user)) throw new BadRequestException("Staff profile already exists");

        StaffProfile sp = new StaffProfile();
        sp.setUser(user);
        sp.setPosition(r.position());
        sp.setPhone(r.phone());
        sp.setNote(r.note());
        sp = staffRepo.save(sp);

        return new StaffResponse(sp.getId(), user.getId(), user.getEmail(), sp.getPosition(), sp.getPhone(), sp.getNote());
    }

    @Transactional(readOnly = true)
    public StaffResponse getByUserId(Long userId) {
        User user = userRepo.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        StaffProfile sp = staffRepo.findByUser(user).orElseThrow(() -> new NotFoundException("Staff profile not found"));
        return new StaffResponse(sp.getId(), user.getId(), user.getEmail(), sp.getPosition(), sp.getPhone(), sp.getNote());
    }

    @Transactional
    public void deleteByUserId(Long userId) {
        User user = userRepo.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        StaffProfile sp = staffRepo.findByUser(user).orElseThrow(() -> new NotFoundException("Staff profile not found"));
        staffRepo.delete(sp);
    }

    @Transactional(readOnly = true)
    public Page<StaffResponse> list(Pageable pageable) {
        return staffRepo.findAll(pageable)
                .map(sp -> new StaffResponse(
                        sp.getId(),
                        sp.getUser().getId(),
                        sp.getUser().getEmail(),
                        sp.getPosition(),
                        sp.getPhone(),
                        sp.getNote()
                ));
    }
}
