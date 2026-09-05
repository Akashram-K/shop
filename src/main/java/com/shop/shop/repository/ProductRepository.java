package com.shop.shop.repository;

import com.shop.shop.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByActiveTrueOrderByCreatedAtDesc();

    List<Product> findByActiveTrueAndIsFeaturedTrueOrderByCreatedAtDesc();

    List<Product> findByCategoryIdAndActiveTrueOrderByCreatedAtDesc(Long categoryId);

    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.category.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.karatOrPurity) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Product> searchActiveProducts(@Param("query") String query);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.category.id = :categoryId AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.karatOrPurity) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Product> searchActiveProductsByCategory(@Param("categoryId") Long categoryId, @Param("query") String query);

    List<Product> findAllByOrderByCreatedAtDesc();

    List<Product> findByStockQuantityLessThanEqualAndActiveTrue(Integer threshold);

    long countByActiveTrue();
}
