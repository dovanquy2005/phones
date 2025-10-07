package com.fonestore.staff_api.service;

import com.fonestore.staff_api.dto.CategoryDTO;
import com.fonestore.staff_api.repo.CategoryQueryRepository;
import com.fonestore.staff_api.repo.proj.CategoryRow;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {
    private final CategoryQueryRepository repo;

    public CategoryService(CategoryQueryRepository repo) {
        this.repo = repo;
    }

    public List<CategoryRow> listSimple() {
        return repo.findAllSimple();
    }

    public List<CategoryDTO> list() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'list'");
    }
}
