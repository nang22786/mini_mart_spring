package com.minimart.api.repository;

import com.minimart.api.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(Long orderId);
    
    // 🆕 NEW: Check if transaction ID already exists
    boolean existsByTransactionId(String transactionId);
    
    // 🆕 NEW: Find payment by transaction ID
    Optional<Payment> findByTransactionId(String transactionId);
}