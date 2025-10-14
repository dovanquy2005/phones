package com.fonestore.staff_api.controller;

import com.fonestore.staff_api.dto.auth.LoginRequest;
import com.fonestore.staff_api.dto.auth.LoginResponse;
import com.fonestore.staff_api.service.UserService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Qualifier;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    public AuthController(@Qualifier("staffUserService") UserService userService) { this.userService = userService; }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest r) {
        return userService.login(r.email(), r.password());
    }
}
