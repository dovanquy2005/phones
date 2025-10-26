// dto/voucher/VoucherUpdateRequest.java
package com.fonestore.staff_api.dto.voucher;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VoucherUpdateRequest(
    String code,
    String type,
    BigDecimal value,
    BigDecimal minOrder,
    Integer usageLimit,
    Integer perUserLimit,
    LocalDateTime startsAt,
    LocalDateTime endsAt,
    Boolean active
) {}
