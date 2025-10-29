package com.fonestore.staff_api.service.order;

import com.fonestore.staff_api.dto.order.OrderDetailDTO;
import com.fonestore.staff_api.dto.order.OrderItemDTO;
import com.fonestore.staff_api.dto.order.OrderItemDetailDTO;
import com.fonestore.staff_api.dto.order.OrderListDTO;
import com.fonestore.staff_api.repository.order.StaffOrderItemRepository;
import com.fonestore.staff_api.repository.order.StaffOrderRepository;
import com.fonestore.staff_api.repository.payment.StaffPaymentRepository;
import com.fonestore.user_api.entity.Order;
import com.fonestore.user_api.entity.OrderItem;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;


@Service
@RequiredArgsConstructor
public class StaffOrderService {

    private final StaffOrderRepository orderRepo;
    private final StaffOrderItemRepository itemRepo;
    private final StaffPaymentRepository paymentRepo;

    /* ===== Helpers ===== */

    private String extractField(String json, String key) {
        if (json == null) return null;
        int i = json.indexOf("\"" + key + "\"");
        if (i < 0) return null;
        int c = json.indexOf(':', i);
        int q1 = json.indexOf('"', c + 1);
        int q2 = json.indexOf('"', q1 + 1);
        if (q1 >= 0 && q2 > q1) return json.substring(q1 + 1, q2);
        return null;
    }
    private String extractName(String snapshot)  { return extractField(snapshot, "name");  }
    private String extractPhone(String snapshot) { return extractField(snapshot, "phone"); }

    private Instant startOfDay(String yyyyMMdd) {
        if (yyyyMMdd == null || yyyyMMdd.isBlank()) return null;
        return LocalDate.parse(yyyyMMdd).atStartOfDay(ZoneId.systemDefault()).toInstant();
    }
    private Instant endExclusive(String yyyyMMdd) {
        if (yyyyMMdd == null || yyyyMMdd.isBlank()) return null;
        return LocalDate.parse(yyyyMMdd).plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    private static BigDecimal bd(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private static Integer i(Integer v) { return v == null ? 0 : v; }

    /* ===== mapping ===== */

    // NOTE: OrderListDTO cần có thêm trường cuối: paymentStatus (String)
    private OrderListDTO toListDTO(Order o, String paymentStatus) {
        String name = extractName(o.getAddressSnapshot());
        return new OrderListDTO(
                o.getId(),
                o.getCode(),
                name,
                o.getStatus(),
                bd(o.getSubtotal()),
                bd(o.getShippingFee()),
                bd(o.getTotal()),
                o.getCreatedAt(),
                paymentStatus
        );
    }

    private OrderDetailDTO toDetailDTO(Order o, List<OrderItem> items) {
        String name  = extractName(o.getAddressSnapshot());
        String phone = extractPhone(o.getAddressSnapshot());

        List<OrderItemDTO> itemDTOs = items.stream().map(it -> {
            int qty = i(it.getQuantity());
            BigDecimal unit = bd(it.getUnitPrice());
            BigDecimal line = unit.multiply(BigDecimal.valueOf(Math.max(qty, 0)));
            return new OrderItemDTO(it.getId(), it.getSkuId(), qty, unit, line);
        }).toList();

        return new OrderDetailDTO(
                o.getId(), o.getCode(), o.getStatus(),
                name, phone, o.getAddressSnapshot(),
                itemDTOs,
                bd(o.getSubtotal()),
                bd(o.getDiscount()),
                bd(o.getShippingFee()),
                bd(o.getTotal()),
                o.getNote(), o.getCreatedAt(), o.getUpdatedAt()
        );
    }

    /* ===== Queries ===== */
    @Transactional(readOnly = true)
    public List<OrderListDTO> list(String status, String from, String to, String q) {
        String s = (status == null || status.isBlank()) ? null : status.trim().toUpperCase();
        String query = (q == null || q.isBlank()) ? null : q.trim();

        List<Order> orders = orderRepo.search(s, startOfDay(from), endExclusive(to), query);

        // Lấy id đơn
        List<Long> ids = orders.stream()
                .map(Order::getId)
                .filter(Objects::nonNull)
                .toList();

        // Lấy các orderId đã PAID
        Set<Long> paidIds = new HashSet<>();
        if (!ids.isEmpty()) {
            paidIds.addAll(paymentRepo.findPaidOrderIds(ids));
        }

        return orders.stream()
                .map(o -> toListDTO(o, paidIds.contains(o.getId()) ? "PAID" : "UNPAID"))
                .toList();
    }


    @Transactional(readOnly = true)
    public OrderDetailDTO detail(Long id) {
        Order o = orderRepo.findById(id).orElseThrow(NoSuchElementException::new);
        List<OrderItem> items = itemRepo.findByOrderIdOrderByIdAsc(id);
        return toDetailDTO(o, items);
    }

    /** Lấy chi tiết item có kèm skuCode + productName (dùng cho hóa đơn / UI bảng). */
    @Transactional(readOnly = true)
    public List<OrderItemDetailDTO> getOrderDetails(Long orderId) {
        return itemRepo.findDetailsByOrderId(orderId)
                .stream()
                .map(OrderItemDetailDTO::from)
                .toList();
    }

    /* ===== Commands ===== */

    @Transactional
    public OrderDetailDTO updateStatus(Long id, String newStatus, String note) {
        if (newStatus == null || newStatus.isBlank())
            throw new IllegalArgumentException("status is required");
        Order o = orderRepo.findById(id).orElseThrow(NoSuchElementException::new);
        o.setStatus(newStatus.trim().toUpperCase());
        if (note != null) o.setNote(note);
        o.setUpdatedAt(Instant.now());
        orderRepo.save(o);
        return detail(id);
    }

    /* ===== Export Invoice (XLSX) ===== */

    @Transactional(readOnly = true)
    public byte[] exportInvoiceXlsx(Long id) {
        Order o = orderRepo.findById(id).orElseThrow(NoSuchElementException::new);
        var items = getOrderDetails(id);

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sh = wb.createSheet("Invoice");
            int r = 0;

            CellStyle bold = wb.createCellStyle();
            Font f = wb.createFont(); f.setBold(true); bold.setFont(f);

            Row title = sh.createRow(r++); title.createCell(0).setCellValue("HÓA ĐƠN BÁN HÀNG"); title.getCell(0).setCellStyle(bold);

            sh.createRow(r++).createCell(0).setCellValue("Mã đơn: " + Optional.ofNullable(o.getCode()).orElse("OD-" + String.format("%06d", o.getId())));
            sh.createRow(r++).createCell(0).setCellValue("Khách: " + Optional.ofNullable(extractName(o.getAddressSnapshot())).orElse("-"));
            sh.createRow(r++).createCell(0).setCellValue("Ngày: " + Optional.ofNullable(o.getCreatedAt()).orElse(Instant.now()));

            r++;
            Row head = sh.createRow(r++);
            head.createCell(0).setCellValue("Sản phẩm"); head.getCell(0).setCellStyle(bold);
            head.createCell(1).setCellValue("SL");       head.getCell(1).setCellStyle(bold);
            head.createCell(2).setCellValue("Đơn giá");  head.getCell(2).setCellStyle(bold);
            head.createCell(3).setCellValue("Thành tiền"); head.getCell(3).setCellStyle(bold);

            BigDecimal sum = BigDecimal.ZERO;
            for (var it : items) {
                int qty = it.qty() == null ? 0 : it.qty();
                BigDecimal unit = it.unitPrice() == null ? BigDecimal.ZERO : it.unitPrice();
                BigDecimal line = unit.multiply(BigDecimal.valueOf(Math.max(qty, 0)));
                sum = sum.add(line);

                Row row = sh.createRow(r++);
                row.createCell(0).setCellValue(it.skuCode() + " — " + it.productName());
                row.createCell(1).setCellValue(qty);
                row.createCell(2).setCellValue(unit.doubleValue());
                row.createCell(3).setCellValue(line.doubleValue());
            }
            r++;

            sh.createRow(r++).createCell(2).setCellValue("Tạm tính:");
            sh.getRow(r-1).createCell(3).setCellValue(bd(o.getSubtotal()).max(sum).doubleValue());

            sh.createRow(r++).createCell(2).setCellValue("Giảm giá:");
            sh.getRow(r-1).createCell(3).setCellValue(bd(o.getDiscount()).doubleValue());

            sh.createRow(r++).createCell(2).setCellValue("Phí VC:");
            sh.getRow(r-1).createCell(3).setCellValue(bd(o.getShippingFee()).doubleValue());

            Row total = sh.createRow(r++);
            total.createCell(2).setCellValue("TỔNG CỘNG:"); total.getCell(2).setCellStyle(bold);
            BigDecimal calc = bd(o.getSubtotal()).max(sum)
                    .subtract(bd(o.getDiscount()))
                    .add(bd(o.getShippingFee()));
            total.createCell(3).setCellValue(bd(o.getTotal()).max(calc).doubleValue());
            total.getCell(3).setCellStyle(bold);

            for (int c = 0; c <= 3; c++) sh.autoSizeColumn(c);
            wb.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Export invoice failed", e);
        }
    }
}
