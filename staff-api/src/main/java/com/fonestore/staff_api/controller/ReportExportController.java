package com.fonestore.staff_api.controller;

import com.fonestore.staff_api.dto.report.DailyRevenueDTO;
import com.fonestore.staff_api.dto.report.MonthlyRevenueDTO;
import com.fonestore.staff_api.dto.report.TopProductDTO;
import com.fonestore.staff_api.service.report.ReportService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportExportController {

    private final ReportService svc;

    @GetMapping(
        value = "/export.xlsx",
        produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    )
    public ResponseEntity<byte[]> exportXlsx(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam int year,
            @RequestParam(name = "topLimit", defaultValue = "50") int topLimit
    ) throws Exception {

        List<DailyRevenueDTO> daily   = svc.daily(from, to);
        List<MonthlyRevenueDTO> monthly = svc.monthly(year);
        List<TopProductDTO> top      = svc.topProducts(from, to, topLimit);

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            DataFormat df = wb.createDataFormat();
            CellStyle money = wb.createCellStyle();
            money.setDataFormat(df.getFormat("#,##0")); // số nguyên; nếu cần thập phân dùng "#,##0.00"

            // ----- Sheet Daily -----
            Sheet s1 = wb.createSheet("Daily");
            Row h1 = s1.createRow(0);
            h1.createCell(0).setCellValue("Date");
            h1.createCell(1).setCellValue("Revenue");
            int r = 1;
            for (DailyRevenueDTO d : daily) {
                Row row = s1.createRow(r++);
                row.createCell(0).setCellValue(d.date().toString());
                Cell c = row.createCell(1);
                c.setCellValue(toDouble(d.revenue()));
                c.setCellStyle(money);
            }
            s1.autoSizeColumn(0); s1.autoSizeColumn(1);

            // ----- Sheet Monthly -----
            Sheet s2 = wb.createSheet("Monthly");
            Row h2 = s2.createRow(0);
            h2.createCell(0).setCellValue("Month");
            h2.createCell(1).setCellValue("Revenue");
            r = 1;
            for (MonthlyRevenueDTO m : monthly) {
                Row row = s2.createRow(r++);
                row.createCell(0).setCellValue(m.month());
                Cell c = row.createCell(1);
                c.setCellValue(toDouble(m.revenue()));
                c.setCellStyle(money);
            }
            s2.autoSizeColumn(0); s2.autoSizeColumn(1);

            // ----- Sheet TopProducts -----
            Sheet s3 = wb.createSheet("TopProducts");
            Row h3 = s3.createRow(0);
            h3.createCell(0).setCellValue("SKU/Name");
            h3.createCell(1).setCellValue("Qty");
            h3.createCell(2).setCellValue("Revenue");
            r = 1;
            for (TopProductDTO t : top) {
                Row row = s3.createRow(r++);
                row.createCell(0).setCellValue(t.name());
                row.createCell(1).setCellValue(t.qty() == null ? 0 : t.qty());
                Cell c = row.createCell(2);
                c.setCellValue(toDouble(t.revenue()));
                c.setCellStyle(money);
            }
            s3.autoSizeColumn(0); s3.autoSizeColumn(1); s3.autoSizeColumn(2);

            wb.write(baos);
            byte[] bytes = baos.toByteArray();

            String filename = String.format("reports_%d_%s_%s.xlsx", year, from, to);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
            headers.setCacheControl(CacheControl.noCache().getHeaderValue());

            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
        }
    }

    private static double toDouble(BigDecimal bd){
        return (bd == null) ? 0d : bd.doubleValue();
    }
}
