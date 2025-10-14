package com.fonestore.staff_api.controller;

import com.fonestore.staff_api.dto.user.*;
import com.fonestore.staff_api.service.UserService;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Qualifier;


@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService service;
    public UserController(@Qualifier("staffUserService") UserService service) { this.service = service; }

    @PostMapping(
        consumes = "application/json",
        produces = "application/json"
    )
    public UserResponse create(@RequestBody UserCreateRequest r) { return service.create(r); }

    @GetMapping
    public Page<UserResponse> list(
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String sort
    ) {
        Sort s = Sort.by(sort.split(",")[0]);
        if (sort.toLowerCase().endsWith(",desc")) s = s.descending();
        Pageable p = PageRequest.of(page, size, s);
        return service.list(role, p);
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @RequestBody UserUpdateRequest r) {
        return service.update(id, r);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) { service.delete(id); }

    @PostMapping("/{id}/2fa/enable")
    public TwoFAResponse enable2FA(@PathVariable Long id,
                                   @RequestParam(defaultValue = "PhoneStore") String issuer) {
        return service.enable2FA(id, issuer);
    }

    @PostMapping("/{id}/2fa/disable")
    public void disable2FA(@PathVariable Long id) { service.disable2FA(id); }
    
    @GetMapping("/{id}")
    public UserResponse getOne(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}/password")
    public void changePassword(@PathVariable Long id, @RequestBody PasswordUpdateRequest r) {
        service.changePassword(id, r.oldPassword(), r.newPassword());
    }

}

