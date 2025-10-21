package com.minimart.api.repository;

import com.minimart.api.model.Product;
import com.minimart.api.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    // Find products by category
    List<Product> findByCategory(Category category);

    // Find products by category ID
    List<Product> findByCategoryId(Integer categoryId);

    // Find products by name containing (case insensitive)
    List<Product> findByNameContainingIgnoreCase(String keyword);

    // Search products by name or detail
    @Query("SELECT p FROM Product p WHERE " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.detail) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Product> searchProducts(@Param("keyword") String keyword);

    // Find products with low stock (using join with Stock table)
    @Query("SELECT p FROM Product p JOIN p.stock s WHERE s.qty <= :threshold")
    List<Product> findByStockLessThanEqual(@Param("threshold") Integer threshold);

    // Find all products ordered by name
    List<Product> findAllByOrderByNameAsc();

    // Find all products ordered by price
    List<Product> findAllByOrderByPriceAsc();
    List<Product> findAllByOrderByPriceDesc();

    // Find all products ordered by creation date
    List<Product> findAllByOrderByCreatedAtDesc();

    // Find products by category ordered by name
    List<Product> findByCategoryIdOrderByNameAsc(Integer categoryId);

    // Count products by category
    long countByCategory(Category category);
    long countByCategoryId(Integer categoryId);

    // Check if product name exists
    boolean existsByName(String name);
}