package com.fonestore.staff_api.controller.order;

import com.fonestore.staff_api.dto.order.*;
import com.fonestore.staff_api.service.order.StaffOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders") // staff FE gọi /api/orders
@RequiredArgsConstructor
public class StaffOrderController {

    private final StaffOrderService service;

    @GetMapping
    public List<OrderListDTO> list(@RequestParam(required = false) String status,
                                   @RequestParam(required = false) String from,
                                   @RequestParam(required = false) String to,
                                   @RequestParam(required = false) String q) {
        return service.list(status, from, to, q);
    }
    @GetMapping("/{id}/items")
    public ResponseEntity<List<OrderItemDetailDTO>> getItems(@PathVariable Long id) {
        return ResponseEntity.ok(service.getOrderDetails(id));
    }


    @GetMapping("/{id}")
    public OrderDetailDTO detail(@PathVariable Long id) {
        return service.detail(id);
    }

    @PatchMapping("/{id}/status")
    public OrderDetailDTO updateStatus(@PathVariable Long id,
                                       @RequestBody UpdateStatusRequest req) {
        return service.updateStatus(id, req.status(), req.note());
    }

    @GetMapping("/{id}/invoice.xlsx")
    public ResponseEntity<byte[]> export(@PathVariable Long id) {
        byte[] data = service.exportInvoiceXlsx(id);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoice-" + id + ".xlsx")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(data);
    }
}
