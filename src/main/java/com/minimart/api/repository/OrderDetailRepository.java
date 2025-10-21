package com.minimart.api.repository;

import com.minimart.api.model.OrderDetail;
import com.minimart.api.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {
    List<OrderDetail> findByOrder(Order order);
}