package com.fonestore.user_api.controller;

import com.fonestore.user_api.dto.order.PagedOrderResponse;
import com.fonestore.user_api.dto.order.UserOrderDetailDTO; // dùng DTO của user_api
import com.fonestore.user_api.service.OrderService;
import com.fonestore.user_api.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/orders") 
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final SecurityUtil securityUtil;

    /**
     * GET /api/orders?status=...&userIdParam=...
     * (No pagination — trả về toàn bộ danh sách orders của user, filter bằng status nếu có)
     */
    @GetMapping
    public ResponseEntity<List<PagedOrderResponse>> listOrders(@RequestParam(required = false) String status,
                                                               @RequestParam(required = false) Long userIdParam) {
        Long uid = securityUtil.resolveUserId(userIdParam);
        if (uid == null) return ResponseEntity.status(401).build();
        var res = orderService.listOrdersForUser(uid, status);
        return ResponseEntity.ok(res);
    }

    /**
     * Backward-compatible: nếu client vẫn gửi page & size, xử lý ở đây (ignored),
     * và delegate về cùng hành vi không phân trang.
     * Mapping này chỉ match khi cả page và size đều có trong query params.
     */
    @GetMapping(params = {"page", "size"})
    public ResponseEntity<List<PagedOrderResponse>> listOrdersPagedIgnored(
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long userIdParam) {
        // ignore page/size, reuse non-paging implementation
        return listOrders(status, userIdParam);
    }

    /**
     * GET /api/orders/{id}
     * Trả về OrderDetailDTO từ user_api (không phải staff_api)
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserOrderDetailDTO> getOrder(@PathVariable Long id,
                                                   @RequestParam(required = false) Long userIdParam) {
        Long uid = securityUtil.resolveUserId(userIdParam);
        if (uid == null) return ResponseEntity.status(401).build();
        var dto = orderService.getOrderDetail(uid, id);
        if (dto == null) return ResponseEntity.status(404).build();
        return ResponseEntity.ok(dto);
    }
}
