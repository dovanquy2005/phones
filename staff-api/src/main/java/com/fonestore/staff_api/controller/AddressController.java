package com.fonestore.staff_api.controller;

import com.fonestore.staff_api.dto.address.*;
import com.fonestore.staff_api.service.AddressService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/addresses")
public class AddressController {
    private final AddressService service;
    public AddressController(@Qualifier("staffAddressService") AddressService service) { this.service = service; }

    @GetMapping public List<AddressResponse> list(@PathVariable Long userId) { return service.list(userId); }

    @PostMapping public AddressResponse create(@PathVariable Long userId, @RequestBody AddressCreateRequest r) {
        return service.add(userId, r);
    }

    @PutMapping("/{addressId}")
    public AddressResponse update(@PathVariable Long userId,
                                  @PathVariable Long addressId,
                                  @RequestBody AddressUpdateRequest r) {
        return service.update(userId, addressId, r);
    }

    @PatchMapping("/{addressId}/default")
    public void setDefault(@PathVariable Long userId, @PathVariable Long addressId) {
        service.setDefault(userId, addressId);
    }

    @DeleteMapping("/{addressId}")
    public void delete(@PathVariable Long userId, @PathVariable Long addressId) { service.delete(userId, addressId); }
}
