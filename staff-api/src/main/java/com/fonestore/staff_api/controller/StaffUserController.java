package com.fonestore.staff_api.controller;

import com.fonestore.staff_api.dto.staff.*;
import com.fonestore.staff_api.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff/users")
@RequiredArgsConstructor
public class StaffUserController {

    private final StaffService staffService;

    @GetMapping
    public Page<StaffResponse> list(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size){
        return staffService.list(PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }

    @GetMapping("/{id}")
    public StaffResponse get(@PathVariable("id") Long id){
        return staffService.get(id);
    }

    @PostMapping
    public ResponseEntity<StaffResponse> create(@Valid @RequestBody StaffCreateRequest r){
        return ResponseEntity.ok(staffService.create(r));
    }

    @PatchMapping("/{id}")
    public StaffResponse patch(@PathVariable("id") Long id, @RequestBody StaffUpdateRequest r){
        return staffService.update(id, r);
    }

    @PutMapping("/{id}")
    public StaffResponse put(@PathVariable("id") Long id, @Valid @RequestBody StaffCreateRequest r){
        // replace: map sang update
        var u = new StaffUpdateRequest(r.email(), r.fullName(), r.role(), r.password(), r.position(), r.phone(), r.isActive());
        return staffService.update(id, u);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> delete(@PathVariable("id") Long id){
        staffService.delete(id);
        return ResponseEntity.noContent().build(); // 204, không có body
    }
}
