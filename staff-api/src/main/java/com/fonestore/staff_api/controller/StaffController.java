package com.fonestore.staff_api.controller;


import com.fonestore.staff_api.dto.staff.StaffCreateRequest;
import com.fonestore.staff_api.dto.staff.StaffResponse;

import com.fonestore.staff_api.service.StaffService;

import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;



import org.springframework.data.domain.Page;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/api/staff")
public class StaffController {
    private final StaffService service;
    public StaffController(StaffService service) { this.service = service; }

    @PostMapping public StaffResponse create(@RequestBody StaffCreateRequest r) { return service.create(r); }

    @GetMapping("/by-user/{userId}") public StaffResponse getByUser(@PathVariable Long userId) {
        return service.getByUserId(userId);
    }

    @DeleteMapping("/by-user/{userId}") public void deleteByUser(@PathVariable Long userId) {
        service.deleteByUserId(userId);
    }

    @GetMapping("/profiles")
    public Page<StaffResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "staffId,desc") String sort // map theo DTO field
    ) {
        Sort s = Sort.by(sort.split(",")[0]);
        if (sort.toLowerCase().endsWith(",desc")) s = s.descending();
        Pageable p = PageRequest.of(page, size, s);
        return service.list(p);
    }
}
