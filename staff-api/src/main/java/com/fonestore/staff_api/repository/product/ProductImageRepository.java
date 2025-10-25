package com.fonestore.staff_api.repository.product;
import com.fonestore.staff_api.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    List<ProductImage> findByProductIdOrderBySortOrderAsc(Long productId);
    
}
