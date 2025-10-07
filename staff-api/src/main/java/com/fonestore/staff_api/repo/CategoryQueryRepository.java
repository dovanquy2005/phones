package com.fonestore.staff_api.repo;

import com.fonestore.staff_api.entity.Category;
import com.fonestore.staff_api.repo.proj.CategoryRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CategoryQueryRepository extends JpaRepository<Category, Long> {

    @Query(value = """
            SELECT 
              c.cat_id   AS catId,
              c.name     AS name,
              c.parent_id AS parentId
            FROM categories c
            ORDER BY c.name
            """, nativeQuery = true)
    List<CategoryRow> findAllSimple();
}
