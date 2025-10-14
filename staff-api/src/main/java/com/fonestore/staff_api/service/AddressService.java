package com.fonestore.staff_api.service;

import com.fonestore.staff_api.dto.address.*;
import com.fonestore.staff_api.entity.User;
import com.fonestore.staff_api.entity.UserAddress;
import com.fonestore.staff_api.exception.NotFoundException;
import com.fonestore.staff_api.repository.UserAddressRepository;
import com.fonestore.staff_api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("staffAddressService")
public class AddressService {
    private final UserRepository userRepo;
    private final UserAddressRepository addressRepo;

    public AddressService(UserRepository userRepo, UserAddressRepository addressRepo) {
        this.userRepo = userRepo;
        this.addressRepo = addressRepo;
    }

    @Transactional
    public AddressResponse add(Long userId, AddressCreateRequest r) {
        User user = userRepo.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));

        UserAddress a = new UserAddress();
        a.setUser(user);
        a.setReceiverName(r.receiverName());
        a.setPhone(r.phone());
        a.setLine1(r.line1());
        a.setLine2(r.line2());
        a.setWard(r.ward());
        a.setDistrict(r.district());
        a.setProvince(r.province());
        a.setCountry(r.country());
        a.setPostalCode(r.postalCode());
        a.setDefault(Boolean.TRUE.equals(r.isDefault()));
        a = addressRepo.save(a);

        if (a.isDefault()) unsetOthersDefault(user, a.getId());
        return toResp(a);
    }

    @Transactional
    public AddressResponse update(Long userId, Long addressId, AddressUpdateRequest r) {
        User user = userRepo.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        UserAddress a = addressRepo.findByIdAndUser(addressId, user)
                .orElseThrow(() -> new NotFoundException("Address not found"));

        if (r.receiverName() != null) a.setReceiverName(r.receiverName());
        if (r.phone() != null) a.setPhone(r.phone());
        if (r.line1() != null) a.setLine1(r.line1());
        if (r.line2() != null) a.setLine2(r.line2());
        if (r.ward() != null) a.setWard(r.ward());
        if (r.district() != null) a.setDistrict(r.district());
        if (r.province() != null) a.setProvince(r.province());
        if (r.country() != null) a.setCountry(r.country());
        if (r.postalCode() != null) a.setPostalCode(r.postalCode());
        if (r.isDefault() != null) a.setDefault(r.isDefault());

        a = addressRepo.save(a);
        if (a.isDefault()) unsetOthersDefault(user, a.getId());
        return toResp(a);
    }

    @Transactional
    public void setDefault(Long userId, Long addressId) {
        User user = userRepo.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        UserAddress a = addressRepo.findByIdAndUser(addressId, user)
                .orElseThrow(() -> new NotFoundException("Address not found"));
        if (!a.isDefault()) { a.setDefault(true); addressRepo.save(a); }
        unsetOthersDefault(user, a.getId());
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> list(Long userId) {
        User user = userRepo.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        return addressRepo.findByUser(user).stream().map(this::toResp).toList();
    }

    @Transactional
    public void delete(Long userId, Long addressId) {
        User user = userRepo.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        UserAddress a = addressRepo.findByIdAndUser(addressId, user)
                .orElseThrow(() -> new NotFoundException("Address not found"));
        addressRepo.delete(a);
    }

    private void unsetOthersDefault(User user, Long keepId) {
        List<UserAddress> defaults = addressRepo.findByUserAndIsDefault(user, true);
        for (UserAddress x : defaults) {
            if (!x.getId().equals(keepId)) { x.setDefault(false); addressRepo.save(x); }
        }
    }

    private AddressResponse toResp(UserAddress a) {
        return new AddressResponse(
                a.getId(), a.getReceiverName(), a.getPhone(),
                a.getLine1(), a.getLine2(), a.getWard(), a.getDistrict(),
                a.getProvince(), a.getCountry(), a.getPostalCode(), a.isDefault()
        );
    }
}
