package com.fonestore.user_api.spec;

import com.fonestore.staff_api.entity.Product;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public class ProductSpecs {
  public static Specification<Product> nameContains(String q) {
    return (root, query, cb) -> (q == null || q.isBlank())
        ? cb.conjunction()
        : cb.like(cb.lower(root.get("name")), "%" + q.toLowerCase() + "%");
  }

  public static Specification<Product> brandInCsv(String csv) {
    if (csv == null || csv.isBlank()) return (r,q,cb) -> cb.conjunction();
    List<String> brands = Arrays.stream(csv.split(",")).map(String::trim).toList();
    return (root, query, cb) -> root.get("brand").in(brands);
  }

  public static Specification<Product> priceGte(BigDecimal min) {
    return (root, query, cb) -> (min == null) ? cb.conjunction()
        : cb.greaterThanOrEqualTo(root.get("minPrice"), min);
  }

  public static Specification<Product> priceLte(BigDecimal max) {
    return (root, query, cb) -> (max == null) ? cb.conjunction()
        : cb.lessThanOrEqualTo(root.get("minPrice"), max);
  }
}
