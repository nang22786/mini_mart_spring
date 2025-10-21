package com.minimart.api.repository;

import com.minimart.api.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Integer> {
    
    // Find stock by product ID
    Optional<Stock> findByProductId(Integer productId);
    
    // Check if stock exists for product
    boolean existsByProductId(Integer productId);
    
    // Delete stock by product ID
    void deleteByProductId(Integer productId);
}