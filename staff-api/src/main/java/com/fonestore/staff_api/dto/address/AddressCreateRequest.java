package com.fonestore.staff_api.dto.address;

public record AddressCreateRequest(
        String receiverName,
        String phone,
        String line1,
        String line2,
        String ward,
        String district,
        String province,
        String country,
        String postalCode,
        Boolean isDefault
) {}
