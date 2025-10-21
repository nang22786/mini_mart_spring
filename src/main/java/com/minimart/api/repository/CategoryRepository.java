package com.minimart.api.repository;

import com.minimart.api.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    
    // Find category by name (exact match)
    Optional<Category> findByName(String name);
    
    // Check if category name exists
    boolean existsByName(String name);
    
    // Find categories by name containing (case insensitive search)
    List<Category> findByNameContainingIgnoreCase(String keyword);
    
    // Find all categories ordered by name
    List<Category> findAllByOrderByNameAsc();
    
    // Find all categories ordered by creation date (newest first)
    List<Category> findAllByOrderByCreatedAtDesc();
    
    // Custom query to search in name field
    @Query("SELECT c FROM Category c WHERE " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Category> searchCategories(String keyword);
}