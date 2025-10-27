package com.minimart.api.repository;

import com.minimart.api.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // For customer: Get user's orders
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    // For admin: Get all orders
    List<Order> findAllByOrderByCreatedAtDesc();

    // 🆕 NEW: For duplicate prevention (Safety #3)
    List<Order> findByUserIdAndStatus(Long userId, String status);

    // 🆕 NEW: For cleanup (Safety #1)
    List<Order> findByStatusAndCreatedAtBefore(String status, LocalDateTime createdAt);
}