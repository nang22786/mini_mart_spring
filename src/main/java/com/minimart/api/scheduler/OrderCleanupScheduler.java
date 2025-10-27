package com.minimart.api.scheduler;

import com.minimart.api.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 🔒 SAFETY #1: Automatic cleanup of failed orders
 * Runs daily at 2:00 AM to delete failed orders older than 7 days
 */
@Component
public class OrderCleanupScheduler {

    @Autowired
    private OrderService orderService;

    /**
     * Cleanup failed orders every day at 2:00 AM
     * Cron expression: "0 0 2 * * ?" = At 02:00:00 AM every day
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupFailedOrders() {
        System.out.println("🗑️ [SCHEDULED] Starting daily cleanup of failed orders...");
        
        Map<String, Object> result = orderService.cleanupFailedOrders();
        
        if ((Boolean) result.get("success")) {
            System.out.println("✅ [SCHEDULED] Cleanup completed: " + result.get("message"));
        } else {
            System.err.println("❌ [SCHEDULED] Cleanup failed: " + result.get("message"));
        }
    }
}