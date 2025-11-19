package com.fonestore.staff_api.controller;

import com.fonestore.staff_api.dto.user.*;
import com.fonestore.staff_api.service.UserAuthService;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserAuthService service;
    public UserController(UserAuthService service) { this.service = service; }

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
            @RequestParam(defaultValue = "userId,desc") String sort // đổi mặc định
    ) {
        // tách "field,dir"
        String[] parts = sort.split(",");
        String reqField = parts.length > 0 ? parts[0].trim() : "userId";
        String dir = parts.length > 1 ? parts[1].trim() : "asc";

        // ánh xạ & whitelist để tránh lỗi "No property ..."
        String field = switch (reqField) {
            case "id", "userId" -> "userId";     // map "id" -> "userId"
            case "email"        -> "email";
            case "fullName"     -> "fullName";
            case "phone"        -> "phone";
            case "dob"          -> "dob";
            case "gender"       -> "gender";
            default             -> "userId";     // fallback an toàn
        };

        Sort s = Sort.by(field);
        if ("desc".equalsIgnoreCase(dir)) s = s.descending();

        Pageable p = PageRequest.of(Math.max(0, page), Math.max(1, size), s);
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

    // @PutMapping("/{id}/password")
    // public void changePassword(@PathVariable Long id, @RequestBody PasswordUpdateRequest r) {
    //     service.changePassword(id, r.oldPassword(), r.newPassword());
    // }

}

