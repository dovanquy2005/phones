package com.fonestore.staff_api.service.customer;

import com.fonestore.staff_api.dto.customer.CustomerSummaryDTO;
import com.fonestore.staff_api.repository.CustomerSummaryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CustomerService {

    private final CustomerSummaryRepository repo;

    public CustomerService(CustomerSummaryRepository repo) {
        this.repo = repo;
    }

    public List<CustomerSummaryDTO> list(String q){
        String kw = (q == null || q.isBlank()) ? "" : "%" + q.trim() + "%";
        var rows = repo.findSummaries(kw);

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
}
