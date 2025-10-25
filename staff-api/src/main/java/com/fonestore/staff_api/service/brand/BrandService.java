package com.fonestore.staff_api.service.brand;

import com.fonestore.staff_api.dto.brand.BrandDTO;
import com.fonestore.staff_api.dto.brand.CreateBrandRequest;
import com.fonestore.staff_api.dto.brand.UpdateBrandRequest;
import com.fonestore.staff_api.entity.Brand;
import com.fonestore.staff_api.repository.brand.BrandRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;

    private static String norm(String s) {
        return s == null ? null : s.trim();
    }

    public List<BrandDTO> listAll() {
        return brandRepository.findAll().stream()
                .map(b -> new BrandDTO(b.getId(), b.getName()))
                .toList();
    }

    @Transactional
    public BrandDTO create(CreateBrandRequest req) {
        String name = norm(req.getName());
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên thương hiệu không được trống");
        }
        if (brandRepository.existsByNameIgnoreCase(name)) {
            // sẽ được advice map thành 409 Conflict
            throw new DataIntegrityViolationException("Brand name already exists");
        }
        Brand b = new Brand();
        b.setName(name);
        b = brandRepository.save(b);
        return new BrandDTO(b.getId(), b.getName());
    }

    @Transactional
    public BrandDTO update(Long id, UpdateBrandRequest req) {
        Brand b = brandRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Brand not found"));
        String name = norm(req.getName());
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên thương hiệu không được trống");
        }
        // nếu đổi tên sang tên đã tồn tại (khác id hiện tại) → 409
        brandRepository.findByNameIgnoreCase(name)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(x -> { throw new DataIntegrityViolationException("Brand name already exists"); });

        b.setName(name);
        brandRepository.save(b);
        return new BrandDTO(b.getId(), b.getName());
    }

    @Transactional
    public void delete(Long id) {
        if (!brandRepository.existsById(id)) return;
        brandRepository.deleteById(id);
    }
}
