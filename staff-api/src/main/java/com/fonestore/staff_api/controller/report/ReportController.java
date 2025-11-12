package com.fonestore.staff_api.controller.report;

import com.fonestore.staff_api.dto.report.DailyRevenueDTO;
import com.fonestore.staff_api.dto.report.MonthlyRevenueDTO;
import com.fonestore.staff_api.dto.report.TopProductDTO;
import com.fonestore.staff_api.service.report.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;



// com.fonestore.staff_api.controller.ReportController
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {
  private final ReportService svc;

  @GetMapping("/revenue/daily")
  public List<DailyRevenueDTO> daily(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to){
    return svc.daily(from, to);
  }

  @GetMapping("/revenue/monthly")
  public List<MonthlyRevenueDTO> monthly(@RequestParam int year){
    return svc.monthly(year);
  }

  @GetMapping("/top-products")
  public List<TopProductDTO> topProducts(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(defaultValue="10") int limit){
    return svc.topProducts(from, to, limit);
  }
}

