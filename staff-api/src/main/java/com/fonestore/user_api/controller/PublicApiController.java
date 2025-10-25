package com.fonestore.user_api.controller;


import com.fonestore.staff_api.dto.product.ProductDetailDTO;
import com.fonestore.staff_api.dto.product.ProductListDTO;
import com.fonestore.staff_api.service.product.ProductService;
import com.fonestore.user_api.dto.order.CreateOrderRequest;
import com.fonestore.user_api.dto.order.OrderResponse;
import com.fonestore.user_api.service.OrderService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class PublicApiController {

  private final ProductService productService;
  private final OrderService orderService;

  public PublicApiController(ProductService productService,
                             OrderService orderService) {
    this.productService = productService;
    this.orderService   = orderService;
  }

  // --- healthcheck nhanh ---
  @GetMapping("/ping")
  public String ping() { return "public-ok"; }

  // --- products ---
  @GetMapping("/products")
  public List<ProductListDTO> listProducts() {
    return productService.listAll();
  }

  @GetMapping("/products/{id}")
  public ProductDetailDTO get(@PathVariable Long id) {
    return productService.getDetail(id);
  }

  // --- orders ---
  @PostMapping("/orders")
  public OrderResponse create(@RequestBody @Valid CreateOrderRequest req) {
    return orderService.create(req);
  }

  // Lấy danh sách đơn theo userId (phục vụ trang lịch sử mua hàng)
  // GET /api/public/orders?userId=14
  @GetMapping(value = "/orders", params = "userId")
  public List<OrderResponse> listByUser(@RequestParam Long userId) {
    return orderService.findByUserId(userId);
  }

  // Fallback: thiếu userId -> tránh rơi vào "No static resource ..."
  @GetMapping("/orders")
  public ResponseEntity<?> requireUserId() {
    return ResponseEntity.badRequest().body(Map.of("error","Missing 'userId'"));
  }
  
  // Chi tiết 1 đơn
  // GET /api/public/orders/7
  @GetMapping("/orders/{orderId}")
  public OrderResponse getOne(@PathVariable Long orderId) {
    return orderService.getById(orderId);
  }
}
