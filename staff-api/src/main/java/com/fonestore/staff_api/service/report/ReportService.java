package com.fonestore.staff_api.service.report;

import com.fonestore.staff_api.dto.report.DailyRevenueDTO;
import com.fonestore.staff_api.dto.report.MonthlyRevenueDTO;
import com.fonestore.staff_api.dto.report.TopProductDTO;
import com.fonestore.staff_api.repository.report.ReportRepository;

import java.util.Collections;   
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository repo;

    /* ================= Helpers ================= */

    private static Timestamp atStart(LocalDate d) {
        return Timestamp.valueOf(d.atStartOfDay());
    }

    private static Timestamp dayAfter(LocalDate d) {
        return Timestamp.valueOf(d.plusDays(1).atStartOfDay());
    }

    private static BigDecimal toDecimal(Object o){
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal b) return b;
        if (o instanceof Number n)      return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(o.toString());
    }

    @FunctionalInterface
    private interface SupplierX<T> { T get() throws Exception; }

    private static <T> T safe(SupplierX<T> s, T fallback) {
        try { return s.get(); } catch (Exception e) { return fallback; }
    }

    /* =============== APIs =============== */

    public List<DailyRevenueDTO> daily(LocalDate from, LocalDate to){
        Timestamp f = atStart(from);
        Timestamp t = dayAfter(to);

        List<Object[]> rows = safe(
            () -> repo.dailyRevenue(f, t),
            Collections.<Object[]>emptyList()
        );

        List<DailyRevenueDTO> out = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            LocalDate d = (r[0] instanceof java.sql.Date d0)
                    ? d0.toLocalDate()
                    : ((Timestamp) r[0]).toLocalDateTime().toLocalDate();
            BigDecimal rev = toDecimal(r[1]);
            out.add(new DailyRevenueDTO(d, rev));
        }
        return out;
    }

    public List<MonthlyRevenueDTO> monthly(int year){
        LocalDateTime start = LocalDateTime.of(year, 1, 1, 0, 0);
        LocalDateTime end   = LocalDateTime.of(year + 1, 1, 1, 0, 0);

        List<Object[]> rows = safe(
            () -> repo.monthlyRevenue(Timestamp.valueOf(start), Timestamp.valueOf(end)),
            Collections.<Object[]>emptyList()
        );

        List<MonthlyRevenueDTO> out = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            int month = ((Number) r[0]).intValue();
            BigDecimal rev = toDecimal(r[1]);
            out.add(new MonthlyRevenueDTO(month, rev));
        }
        return out;
    }


    public List<TopProductDTO> topProducts(LocalDate from, LocalDate to, int limit){
        Timestamp f = Timestamp.valueOf(from.atStartOfDay());
        Timestamp t = Timestamp.valueOf(to.plusDays(1).atStartOfDay()); // [from, to] inclusive

        List<Object[]> rows = safe(
            () -> repo.topSkuRaw(f, t),
            Collections.<Object[]>emptyList()
        );

        int k = Math.max(1, limit);
        if (rows.size() > k) rows = rows.subList(0, k);

        List<TopProductDTO> out = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            Long skuId      = r[0] == null ? 0L : ((Number) r[0]).longValue();
            Long qty        = r[1] == null ? 0L : ((Number) r[1]).longValue();
            BigDecimal rev  = (r[2] instanceof BigDecimal bd) ? bd
                            : new BigDecimal(String.valueOf(r[2]));
            String name     = "SKU " + skuId;
            out.add(new TopProductDTO(skuId, name, qty, rev));
        }
        return out;
    }

}
