package com.fonestore.staff_api.web;

import com.fonestore.staff_api.dto.CategoryDTO;
import com.fonestore.staff_api.service.CategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService service;
    public CategoryController(CategoryService service){ this.service = service; }

    @GetMapping
    public List<CategoryDTO> list(){ return service.list(); }
}
