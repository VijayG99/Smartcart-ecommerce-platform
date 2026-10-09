package com.smartcart.util;

import com.smartcart.dto.ProductSearchCriteria;
import com.smartcart.entity.Inventory;
import com.smartcart.entity.Product;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/** Builds the dynamic WHERE clause for product search from optional filters. */
public final class ProductSpecifications {
    private ProductSpecifications() {}

    public static Specification<Product> fromCriteria(ProductSearchCriteria c) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.isTrue(root.get("active")));
            if (c.q() != null && !c.q().isBlank()) {
                String like = "%" + c.q().trim().toLowerCase() + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("name")), like), cb.like(cb.lower(root.get("description")), like)));
            }
            if (c.categoryId() != null) p.add(cb.equal(root.get("category").get("id"), c.categoryId()));
            if (c.minPrice() != null) p.add(cb.greaterThanOrEqualTo(root.get("price"), c.minPrice()));
            if (c.maxPrice() != null) p.add(cb.lessThanOrEqualTo(root.get("price"), c.maxPrice()));
            if (Boolean.TRUE.equals(c.inStock())) p.add(cb.exists(inStockSubquery(root, query, cb)));
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    /** Matches products whose name or description contains ANY of the keywords (used by smart search). */
    public static Specification<Product> anyKeyword(List<String> keywords) {
        return (root, query, cb) -> cb.or(keywords.stream().map(k -> {
            String like = "%" + k.toLowerCase() + "%";
            return cb.or(cb.like(cb.lower(root.get("name")), like), cb.like(cb.lower(root.get("description")), like));
        }).toArray(Predicate[]::new));
    }

    private static Subquery<Long> inStockSubquery(Root<Product> root, jakarta.persistence.criteria.CriteriaQuery<?> query,
                                                  jakarta.persistence.criteria.CriteriaBuilder cb) {
        Subquery<Long> sq = query.subquery(Long.class);
        Root<Inventory> inv = sq.from(Inventory.class);
        sq.select(inv.get("id")).where(cb.equal(inv.get("product"), root), cb.greaterThan(inv.get("quantity"), 0));
        return sq;
    }
}
