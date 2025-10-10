package com.fonestore.staff_api.repository;
import com.fonestore.staff_api.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductId(Long productId);

    // ĐÚNG: sắp xếp theo field "id" của entity (map với sku_id ở DB)
    List<ProductVariant> findByProductIdOrderByIdAsc(Long productId);

    // ĐÚNG: lấy biến thể có id nhỏ nhất
    Optional<ProductVariant> findFirstByProductIdOrderByIdAsc(Long productId);
}
