// user_api/controller/PublicApiController.java
package com.fonestore.user_api.controller;

import com.fonestore.user_api.dto.*;
import com.fonestore.user_api.service.ProductService;
import com.fonestore.user_api.service.OrderService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/public")
public class PublicApiController {

  private final ProductService productService;
  private final OrderService orderService;

  // ✅ Constructor injection + @Qualifier chọn đúng bean theo tên
  public PublicApiController(@Qualifier("userProductService") ProductService productService,
      @Qualifier("userOrderService")   OrderService orderService) {
    this.productService = productService;
    this.orderService   = orderService;
  }

  @GetMapping("/products")
  public List<ProductListDTO> listProducts() {
    return productService.getAllActive();
  }

  @GetMapping("/products/{id}")
  public ProductDetailDTO get(@PathVariable Long id) {
    return productService.getById(id);
  }

  @PostMapping("/orders")
  public OrderResponse create(@RequestBody @Valid CreateOrderRequest req) {
    return orderService.create(req);
  }
}
