package com.minimart.api.repository;

import com.minimart.api.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Order> findByStatus(String status);
    
    // ✅ Correct method name
    List<Order> findAllByOrderByCreatedAtDesc();
}