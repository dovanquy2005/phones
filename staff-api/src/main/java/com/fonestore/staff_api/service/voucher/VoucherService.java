package com.fonestore.staff_api.service.voucher;

import com.fonestore.staff_api.dto.voucher.*;
import com.fonestore.staff_api.entity.Voucher;
import com.fonestore.staff_api.exception.BadRequestException;
import com.fonestore.staff_api.exception.NotFoundException;
import com.fonestore.staff_api.repository.voucher.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class VoucherService {

    private final VoucherRepository repo;

    /* ---------- Helpers ---------- */
    private static String normCode(String s) {
        if (s == null) return null;
        // bỏ khoảng trắng 2 đầu và giữa (tuỳ policy, đang remove toàn bộ)
        String t = s.trim().replaceAll("\\s+", "");
        if (t.isEmpty()) throw new BadRequestException("Code không được rỗng");
        return t.toUpperCase(Locale.ROOT);
    }

    private static String normType(String t) {
        if (t == null) return null;
        String v = t.trim().toLowerCase(Locale.ROOT);
        if (!v.equals("percent") && !v.equals("flat"))
            throw new BadRequestException("type phải là 'percent' hoặc 'flat'");
        return v;
    }

    private static void validateBusiness(String type, BigDecimal value,
                                         BigDecimal minOrder,
                                         LocalDateTime startsAt, LocalDateTime endsAt) {
        if (value == null) throw new BadRequestException("value không được null");

        if ("percent".equals(type)) {
            if (value.compareTo(BigDecimal.ONE) < 0 || value.compareTo(new BigDecimal("100")) > 0)
                throw new BadRequestException("value (percent) phải 1..100");
        } else { // flat
            if (value.compareTo(BigDecimal.ZERO) <= 0)
                throw new BadRequestException("value (flat) phải > 0");
        }
        if (minOrder != null && minOrder.compareTo(BigDecimal.ZERO) < 0)
            throw new BadRequestException("minOrder không được âm");
        if (startsAt != null && endsAt != null && !endsAt.isAfter(startsAt))
            throw new BadRequestException("endsAt phải sau startsAt");
    }

    private static VoucherResponse toRes(Voucher v) {
        return new VoucherResponse(
            v.getVoucherId(), v.getCode(), v.getType(), v.getValue(), v.getMinOrder(),
            v.getUsageLimit(), v.getPerUserLimit(), v.getStartsAt(), v.getEndsAt(),
            v.isActive(), v.getCreatedAt()
        );
    }

    /* ---------- CREATE ---------- */
    @Transactional
    public VoucherResponse create(VoucherCreateRequest r) {
        final String code = normCode(r.code());
        final String type = normType(r.type());
        final BigDecimal value = r.value();
        final BigDecimal minOrder = r.minOrder();
        final LocalDateTime startsAt = r.startsAt();
        final LocalDateTime endsAt = r.endsAt();
        final Boolean active = r.active() == null ? Boolean.TRUE : r.active();

        // nếu đã có method existsByCodeNorm thì dùng cái đó; ở repo hiện có existsExact(code)
        if (repo.existsExact(code)) throw new BadRequestException("Code đã tồn tại");

        validateBusiness(type, value, minOrder, startsAt, endsAt);

        Voucher v = new Voucher();
        v.setCode(code);                // @PrePersist/@PreUpdate vẫn OK, nhưng set thẳng cho chắc
        v.setCodeNorm(code);
        v.setType(type);
        v.setValue(value);
        // Nếu schema NOT NULL, đảm bảo mặc định:
        v.setMinOrder(minOrder != null ? minOrder : BigDecimal.ZERO);
        v.setUsageLimit(r.usageLimit() != null ? r.usageLimit() : 0);
        v.setPerUserLimit(r.perUserLimit() != null ? r.perUserLimit() : 0);
        v.setStartsAt(startsAt);
        v.setEndsAt(endsAt);
        v.setActive(active);
        v.setCreatedAt(LocalDateTime.now());

        try {
            v = repo.save(v);
        } catch (DataIntegrityViolationException e) {
            // phân biệt lỗi unique code_norm hay lỗi khác (tuỳ log của bạn)
            String msg = e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage();
            if (msg != null && msg.toLowerCase(Locale.ROOT).contains("code_norm"))
                throw new BadRequestException("Code đã tồn tại");
            throw new BadRequestException("Dữ liệu không hợp lệ");
        }
        return toRes(v);
    }

    /* ---------- LIST ---------- */
    @Transactional(readOnly = true)
    public Page<VoucherResponse> list(Pageable pageable) {
        return repo.findAll(pageable).map(VoucherService::toRes);
    }

    /* ---------- GET ---------- */
    @Transactional(readOnly = true)
    public VoucherResponse get(Long id) {
        Voucher v = repo.findById(id).orElseThrow(() -> new NotFoundException("Voucher không tồn tại"));
        return toRes(v);
    }

    /* ---------- UPDATE ---------- */
    @Transactional
    public VoucherResponse update(Long id, VoucherUpdateRequest r) {
        Voucher v = repo.findById(id).orElseThrow(() -> new NotFoundException("Voucher không tồn tại"));

        if (r.code() != null && !r.code().isBlank()) {
            String newCode = normCode(r.code());
            // chỉ check trùng khi thực sự đổi
            // so sánh với code_norm hiện tại để chính xác
            if (!Objects.equals(newCode, v.getCodeNorm()) && repo.existsExact(newCode))
                throw new BadRequestException("Code đã tồn tại");
            v.setCode(newCode);
            v.setCodeNorm(newCode);
        }
        if (r.type() != null) v.setType(normType(r.type()));
        if (r.value() != null) v.setValue(r.value());
        if (r.minOrder() != null) v.setMinOrder(r.minOrder());
        if (r.usageLimit() != null) v.setUsageLimit(r.usageLimit());
        if (r.perUserLimit() != null) v.setPerUserLimit(r.perUserLimit());
        if (r.startsAt() != null) v.setStartsAt(r.startsAt());
        if (r.endsAt() != null) v.setEndsAt(r.endsAt());
        if (r.active() != null) v.setActive(r.active());

        validateBusiness(v.getType(), v.getValue(), v.getMinOrder(), v.getStartsAt(), v.getEndsAt());
        return toRes(v);
    }

    /* ---------- DELETE ---------- */
    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) throw new NotFoundException("Voucher không tồn tại");
        repo.deleteById(id);
    }
}
