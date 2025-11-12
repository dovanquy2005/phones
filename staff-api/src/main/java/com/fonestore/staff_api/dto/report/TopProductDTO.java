package com.fonestore.staff_api.dto.report;

import java.math.BigDecimal;

public record TopProductDTO(Long productId, String name, Long qty, BigDecimal revenue) {}
