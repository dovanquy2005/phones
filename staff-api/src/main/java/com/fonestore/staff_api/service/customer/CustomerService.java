package com.fonestore.staff_api.service.customer;

import com.fonestore.staff_api.dto.customer.CustomerSummaryDTO;
import com.fonestore.staff_api.dto.user.UserResponse; // Dùng lại DTO này
import com.fonestore.staff_api.entity.User;
import com.fonestore.staff_api.exception.NotFoundException;
import com.fonestore.staff_api.repository.CustomerSummaryRepository;
import com.fonestore.staff_api.repository.UserRepository; // Import thêm
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CustomerService {

    private final CustomerSummaryRepository summaryRepo;
    private final UserRepository userRepo; // Thêm repo này

    // Cập nhật Constructor
    public CustomerService(CustomerSummaryRepository summaryRepo, UserRepository userRepo) {
        this.summaryRepo = summaryRepo;
        this.userRepo = userRepo;
    }

    // Hàm cũ giữ nguyên
    public List<CustomerSummaryDTO> list(String q){
        // ... (giữ nguyên code cũ của hàm list) ...
        String kw = (q == null || q.isBlank()) ? "" : "%" + q.trim() + "%";
        var rows = summaryRepo.findSummaries(kw);

        return rows.stream().map(r -> {
            Long id            = ((Number) r[0]).longValue();
            String name        = (String) r[1];
            String phone       = (String) r[2];
            BigDecimal spent   = (BigDecimal) r[3];
            Integer cnt        = ((Number) r[4]).intValue();
            Long lastOrderId   = (r[5] == null) ? null : ((Number) r[5]).longValue();
            LocalDateTime lastAt = (r[6] == null) ? null : ((Timestamp) r[6]).toLocalDateTime();

            String lastCode = (lastOrderId == null) ? null : String.format("OD-%06d", lastOrderId);

            return new CustomerSummaryDTO(id, name, phone, spent, cnt, lastCode, lastAt);
        }).toList();
    }

    // --- HÀM MỚI THÊM: Lấy chi tiết khách hàng ---
    public UserResponse getDetail(Long id) {
        User u = userRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Khách hàng không tồn tại"));
        
        // Map sang UserResponse (DTO đã có sẵn)
        return new UserResponse(
                u.getId(),
                u.getEmail(),
                u.getFullName(),
                u.getPhone(),
                u.getDob() != null ? u.getDob().toString() : null,
                u.getGender(),
                u.getRole(),
                u.getTwofaSecret() != null,
                u.getAddress()
        );
        // Lưu ý: Nếu UserResponse thiếu field 'address', bạn có thể cần update DTO UserResponse 
        // hoặc tạo DTO mới. Ở đây giả định UserResponse chưa có Address, ta sẽ sửa UserResponse ở bước dưới.
    }
}