package com.minimart.api.repository;

import com.minimart.api.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {
    
    // Find all cart items for a specific user using custom query
    @Query("SELECT c FROM Cart c WHERE c.user.id = :userId")
    List<Cart> findByUserId(@Param("userId") Long userId);
    
    // Find specific cart item by user and product using custom query
    @Query("SELECT c FROM Cart c WHERE c.user.id = :userId AND c.product.id = :productId")
    Optional<Cart> findByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Integer productId);
    
    // Delete all cart items for a user using custom query
    @Modifying
    @Query("DELETE FROM Cart c WHERE c.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
    
    // Count cart items for a user
    @Query("SELECT COUNT(c) FROM Cart c WHERE c.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);
    
    // Check if product exists in user's cart
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Cart c WHERE c.user.id = :userId AND c.product.id = :productId")
    boolean existsByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Integer productId);
}