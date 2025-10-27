package com.minimart.api.controller;

import com.minimart.api.model.Order;
import com.minimart.api.service.OrderService;
import com.minimart.api.util.JwtUtil;
import com.minimart.api.dto.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    private Long extractUserIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Invalid authorization header");
        }
        String token = authHeader.substring(7);
        return jwtUtil.extractUserId(token);
    }
    
    private String extractRoleFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Invalid authorization header");
        }
        String token = authHeader.substring(7);
        return jwtUtil.extractRole(token);
    }
    
    // ============================================================
    // 🆕 NEW: CHECKOUT WITH KHQR
    // ============================================================
    
    /**
     * 🆕 CHECKOUT: Create Order + Generate KHQR
     * POST /api/orders/checkout
     */
    @PostMapping("/checkout")
    public ResponseEntity<Map<String, Object>> checkout(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> request
    ) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long userId = extractUserIdFromToken(authHeader);
            
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            Long addressId = request.get("addressId") != null 
                ? ((Number) request.get("addressId")).longValue() 
                : null;
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) request.get("items");
            
            System.out.println("🛒 Checkout request - User #" + userId + " - Amount: $" + amount);
            
            Map<String, Object> result = orderService.checkout(userId, amount, items, addressId);
            
            if ((Boolean) result.get("success")) {
                response.put("success", true);
                response.put("message", result.get("message"));
                response.put("data", result);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                response.put("success", false);
                response.put("message", result.get("message"));
                response.put("data", result);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", "Unauthorized: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ============================================================
    // 📊 CUSTOMER ENDPOINTS
    // ============================================================
    
    /**
     * Get all orders for logged-in user (My Orders)
     * GET /api/orders/my-orders
     */
    @GetMapping("/my-orders")
    public ResponseEntity<Map<String, Object>> getMyOrders(
            @RequestHeader("Authorization") String authHeader
    ) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long userId = extractUserIdFromToken(authHeader);
            List<OrderSummaryDTO> orders = orderService.getUserOrdersSummary(userId);
            
            response.put("success", true);
            response.put("message", "Orders retrieved successfully");
            response.put("count", orders.size());
            response.put("data", orders);
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", "Unauthorized: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get order details by ID
     * GET /api/orders/{orderId}
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrderDetails(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long orderId
    ) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long tokenUserId = extractUserIdFromToken(authHeader);
            String role = extractRoleFromToken(authHeader);
            
            Order order = orderService.getOrderById(orderId);
            
            // Check if user owns this order OR is owner
            if (!order.getUserId().equals(tokenUserId) && !"owner".equalsIgnoreCase(role)) {
                response.put("success", false);
                response.put("message", "Access denied. This order doesn't belong to you.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            OrderDTO orderDetails = orderService.getOrderDetails(orderId);
            
            response.put("success", true);
            response.put("message", "Order details retrieved successfully");
            response.put("data", orderDetails);
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Order not found")) {
                response.put("success", false);
                response.put("message", e.getMessage());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            response.put("success", false);
            response.put("message", "Unauthorized: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ============================================================
    // 🔧 OWNER ENDPOINTS
    // ============================================================
    
    /**
     * Get all orders (Owner only)
     * GET /api/orders/all
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllOrders(
            @RequestHeader("Authorization") String authHeader
    ) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String role = extractRoleFromToken(authHeader);
            if (!"owner".equalsIgnoreCase(role)) {
                response.put("success", false);
                response.put("message", "Access denied. Owner only.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            List<OrderSummaryDTO> orders = orderService.getAllOrdersSummary();
            response.put("success", true);
            response.put("message", "All orders retrieved successfully");
            response.put("count", orders.size());
            response.put("data", orders);
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", "Unauthorized: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 🆕 UPDATE ORDER STATUS (Owner only)
     * PUT /api/orders/{orderId}
     * 
     * Body: {
     *   "status": "paid" or "failed" or "pending"
     * }
     */
    @PutMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> updateOrderStatus(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long orderId,
            @RequestBody Map<String, String> request
    ) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String role = extractRoleFromToken(authHeader);
            if (!"owner".equalsIgnoreCase(role)) {
                response.put("success", false);
                response.put("message", "Access denied. Owner only.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            String newStatus = request.get("status");
            
            if (newStatus == null || newStatus.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Status is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            // Validate status
            if (!newStatus.equals("pending") && !newStatus.equals("paid") && !newStatus.equals("failed")) {
                response.put("success", false);
                response.put("message", "Invalid status. Must be: pending, paid, or failed");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            System.out.println("📝 Owner updating Order #" + orderId + " to status: " + newStatus);
            
            Map<String, Object> result = orderService.updateOrderStatus(orderId, newStatus);
            
            if ((Boolean) result.get("success")) {
                response.put("success", true);
                response.put("message", result.get("message"));
                response.put("data", result.get("order"));
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", result.get("message"));
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", "Unauthorized: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 🆕 DELETE ORDER (Owner only)
     * DELETE /api/orders/{orderId}
     * 
     * Deletes order, order details, and payment
     */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> deleteOrder(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long orderId
    ) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String role = extractRoleFromToken(authHeader);
            if (!"owner".equalsIgnoreCase(role)) {
                response.put("success", false);
                response.put("message", "Access denied. Owner only.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            System.out.println("🗑️ Owner deleting Order #" + orderId);
            
            Map<String, Object> result = orderService.deleteOrder(orderId);
            
            if ((Boolean) result.get("success")) {
                response.put("success", true);
                response.put("message", result.get("message"));
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", result.get("message"));
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", "Unauthorized: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 🗑️ Cleanup failed orders (Owner only)
     * DELETE /api/orders/cleanup-failed
     * 
     * Deletes failed orders older than 7 days
     */
    @DeleteMapping("/cleanup-failed")
    public ResponseEntity<Map<String, Object>> cleanupFailedOrders(
            @RequestHeader("Authorization") String authHeader
    ) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String role = extractRoleFromToken(authHeader);
            if (!"owner".equalsIgnoreCase(role)) {
                response.put("success", false);
                response.put("message", "Access denied. Owner only.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            System.out.println("🗑️ Owner triggered cleanup of failed orders");
            
            Map<String, Object> result = orderService.cleanupFailedOrders();
            
            response.put("success", result.get("success"));
            response.put("message", result.get("message"));
            response.put("deletedCount", result.get("deletedCount"));
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", "Unauthorized: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
 // ============================================================
    // 🔔 NOTIFICATION ENDPOINTS
    // ============================================================

    /**
     * Mark failed order notification as read
     * POST /api/orders/{orderId}/mark-notification-read
     */
    @PostMapping("/{orderId}/mark-notification-read")
    public ResponseEntity<Map<String, Object>> markNotificationAsRead(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long orderId
    ) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long userId = extractUserIdFromToken(authHeader);
            
            System.out.println("📖 User #" + userId + " marking Order #" + orderId + " notification as read");
            
            Map<String, Object> result = orderService.markNotificationAsRead(orderId, userId);
            
            if ((Boolean) result.get("success")) {
                response.put("success", true);
                response.put("message", result.get("message"));
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", result.get("message"));
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", "Unauthorized: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Mark all failed order notifications as read
     * POST /api/orders/mark-all-failed-read
     */
    @PostMapping("/mark-all-failed-read")
    public ResponseEntity<Map<String, Object>> markAllFailedAsRead(
            @RequestHeader("Authorization") String authHeader
    ) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long userId = extractUserIdFromToken(authHeader);
            
            System.out.println("📖 User #" + userId + " marking all failed notifications as read");
            
            Map<String, Object> result = orderService.markAllFailedAsRead(userId);
            
            response.put("success", true);
            response.put("message", result.get("message"));
            response.put("markedCount", result.get("markedCount"));
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", "Unauthorized: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get unread failed orders count
     * GET /api/orders/unread-failed-count
     */
    @GetMapping("/unread-failed-count")
    public ResponseEntity<Map<String, Object>> getUnreadFailedCount(
            @RequestHeader("Authorization") String authHeader
    ) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long userId = extractUserIdFromToken(authHeader);
            
            Integer count = orderService.getUnreadFailedCount(userId);
            
            response.put("success", true);
            response.put("count", count);
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", "Unauthorized: " + e.getMessage());
            response.put("count", 0);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } catch (Exception e) {
            response.put("success", true);
            response.put("count", 0);
            return ResponseEntity.ok(response);
        }
    }
 // ============================================================
 // 🔄 RETRY PAYMENT FOR FAILED ORDER
 // ============================================================

 @PostMapping("/{orderId}/retry-payment")
 public ResponseEntity<Map<String, Object>> retryPayment(
         @RequestHeader("Authorization") String authHeader,
         @PathVariable Long orderId
 ) {
     Map<String, Object> response = new HashMap<>();
     
     try {
         Long userId = extractUserIdFromToken(authHeader);
         
         System.out.println("🔄 User #" + userId + " attempting to retry payment for Order #" + orderId);
         
         Map<String, Object> result = orderService.retryPayment(orderId, userId);
         
         if ((Boolean) result.get("success")) {
             response.put("success", true);
             response.put("message", result.get("message"));
             response.put("data", result.get("data"));
             return ResponseEntity.ok(response);
         } else {
             response.put("success", false);
             response.put("message", result.get("message"));
             return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
         }
         
     } catch (RuntimeException e) {
         response.put("success", false);
         response.put("message", "Unauthorized: " + e.getMessage());
         return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
     } catch (Exception e) {
         response.put("success", false);
         response.put("message", "Error: " + e.getMessage());
         e.printStackTrace();
         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
     }
 }
}