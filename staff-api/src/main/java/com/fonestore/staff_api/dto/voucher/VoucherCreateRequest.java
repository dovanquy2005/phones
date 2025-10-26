// dto/voucher/VoucherCreateRequest.java
package com.fonestore.staff_api.dto.voucher;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VoucherCreateRequest(
    @NotBlank String code,           // sẽ chuẩn hoá về UPPERCASE
    @NotBlank String type,           // percent | flat
    @NotNull  BigDecimal value,
    BigDecimal minOrder,
    Integer usageLimit,
    Integer perUserLimit,
    LocalDateTime startsAt,
    LocalDateTime endsAt,
    Boolean active
) {}
