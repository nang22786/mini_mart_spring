package com.minimart.api.controller;

import com.minimart.api.model.Order;
import com.minimart.api.repository.OrderRepository;
import com.minimart.api.service.OrderService;
import com.minimart.api.util.JwtUtil;
import com.minimart.api.dto.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    
    @Autowired
    private OrderRepository orderRepository;
    
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
    
	/**
	 * Create new order (NO payment yet!)
	 * POST /api/orders/create
	 */
	@PostMapping("/create")
	public ResponseEntity<Map<String, Object>> createOrder(
	        @RequestHeader("Authorization") String authHeader,
	        @RequestBody Map<String, Object> request
	) {
	    Map<String, Object> response = new HashMap<>();
	    
	    try {
	        Long tokenUserId = extractUserIdFromToken(authHeader);
	        
	        // ✅ Only need amount, items, and addressId
	        BigDecimal amount = new BigDecimal(request.get("amount").toString());
	        
	        Long addressId = request.get("addressId") != null 
	            ? ((Number) request.get("addressId")).longValue() 
	            : null;
	        
	        @SuppressWarnings("unchecked")
	        List<Map<String, Object>> items = (List<Map<String, Object>>) request.get("items");
	        
	        // ✅ No payment info needed!
	        Map<String, Object> result = orderService.createOrder(
	            tokenUserId, amount, items, addressId
	        );
	        
	        if ((Boolean) result.get("success")) {
	            response.put("success", true);
	            response.put("message", "Order created successfully");
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
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
	    }
	}
    
    /**
     * Get all orders for logged-in user
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
     * Get all pending orders (Admin only)
     * GET /api/orders/pending
     */
    @GetMapping("/pending")
    public ResponseEntity<Map<String, Object>> getPendingOrders(
            @RequestHeader("Authorization") String authHeader
    ) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String role = extractRoleFromToken(authHeader);
            if (!"admin".equalsIgnoreCase(role) && !"owner".equalsIgnoreCase(role)) {
                response.put("success", false);
                response.put("message", "Access denied. Admin only.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            List<PendingOrderDTO> orders = orderService.getPendingOrdersSummary();
            response.put("success", true);
            response.put("message", "Pending orders retrieved successfully");
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
     * Get all orders (Admin/Owner only)
     * GET /api/orders/all
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllOrders(
            @RequestHeader("Authorization") String authHeader
    ) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String role = extractRoleFromToken(authHeader);
            if (!"admin".equalsIgnoreCase(role) && !"owner".equalsIgnoreCase(role)) {
                response.put("success", false);
                response.put("message", "Access denied. Admin only.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            // ✅ Use DTO instead of full Order entity!
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
     * Get all orders for a specific user (Admin only)
     * GET /api/orders/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserOrders(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long userId
    ) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String role = extractRoleFromToken(authHeader);
            if (!"admin".equalsIgnoreCase(role) && !"owner".equalsIgnoreCase(role)) {
                response.put("success", false);
                response.put("message", "Access denied. Admin only.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            List<OrderSummaryDTO> orders = orderService.getUserOrdersSummary(userId);
            response.put("success", true);
            response.put("message", "User orders retrieved successfully");
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
     * Check payment status
     * GET /api/orders/{orderId}/payment-status
     */
    @GetMapping("/{orderId}/payment-status")
    public ResponseEntity<Map<String, Object>> checkPaymentStatus(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long orderId
    ) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long tokenUserId = extractUserIdFromToken(authHeader);
            String role = extractRoleFromToken(authHeader);
            
            Order order = orderService.getOrderById(orderId);
            
            if (!order.getUserId().equals(tokenUserId) 
                && !"admin".equalsIgnoreCase(role) 
                && !"owner".equalsIgnoreCase(role)) {
                response.put("success", false);
                response.put("message", "Access denied.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            Map<String, Object> status = orderService.checkPaymentStatus(orderId);
            response.put("success", true);
            response.put("message", "Payment status retrieved successfully");
            response.put("data", status);
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", "Unauthorized: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Order not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
    
    /**
     * Get order by ID
     * GET /api/orders/{orderId}
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrderById(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long orderId
    ) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long tokenUserId = extractUserIdFromToken(authHeader);
            String role = extractRoleFromToken(authHeader);
            
            Order order = orderService.getOrderById(orderId);
            
            if (!order.getUserId().equals(tokenUserId) 
                && !"admin".equalsIgnoreCase(role) 
                && !"owner".equalsIgnoreCase(role)) {
                response.put("success", false);
                response.put("message", "Access denied.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            OrderDTO orderDetails = orderService.getOrderDetails(orderId);
            response.put("success", true);
            response.put("message", "Order details retrieved successfully");
            response.put("data", orderDetails);
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", "Unauthorized: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Order not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
    
    /**
     * Simulate payment callback
     * POST /api/orders/{orderId}/simulate-payment ///////////////////////////////////////////////////////////////////
     */
    @PostMapping("/{orderId}/simulate-payment")
    public ResponseEntity<Map<String, Object>> simulatePayment(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> request
    ) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String status = request.getOrDefault("status", "success");
            String transactionRef = request.getOrDefault("transactionRef", "TEST-" + System.currentTimeMillis());
            
            Map<String, Object> result = orderService.updatePaymentStatus(
                orderId, status, transactionRef
            );
            
            response.put("success", true);
            response.put("message", "Payment simulated successfully");
            response.put("data", result);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Upload payment screenshot
     * POST /api/orders/{orderId}/upload-screenshot
     */
    @PostMapping("/{orderId}/upload-screenshot")
    public ResponseEntity<Map<String, Object>> uploadScreenshot(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long orderId,
            @RequestParam("screenshot") MultipartFile screenshot
    ) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long tokenUserId = extractUserIdFromToken(authHeader);
            
            Map<String, Object> result = orderService.uploadPaymentScreenshot(
                tokenUserId, orderId, screenshot
            );
            
            if ((Boolean) result.get("success")) {
                response.put("success", true);
                response.put("message", result.get("message"));
                response.put("data", result);
                return ResponseEntity.ok(response);
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Confirm payment (Admin)
     * PUT /api/orders/{orderId}/confirm-payment
     */
    @PutMapping("/{orderId}/confirm-payment")
    public ResponseEntity<Map<String, Object>> confirmPayment(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long orderId
    ) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String role = extractRoleFromToken(authHeader);
            if (!"admin".equalsIgnoreCase(role) && !"owner".equalsIgnoreCase(role)) {
                response.put("success", false);
                response.put("message", "Access denied. Admin only.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            Map<String, Object> result = orderService.confirmPayment(orderId);
            
            if ((Boolean) result.get("success")) {
                response.put("success", true);
                response.put("message", result.get("message"));
                response.put("data", result);
                return ResponseEntity.ok(response);
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Reject payment (Admin)
     * PUT /api/orders/{orderId}/reject-payment
     */
    @PutMapping("/{orderId}/reject-payment")
    public ResponseEntity<Map<String, Object>> rejectPayment(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long orderId,
            @RequestBody(required = false) Map<String, String> request
    ) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String role = extractRoleFromToken(authHeader);
            if (!"admin".equalsIgnoreCase(role) && !"owner".equalsIgnoreCase(role)) {
                response.put("success", false);
                response.put("message", "Access denied. Admin only.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            String reason = request != null ? request.get("reason") : "Payment verification failed";
            Map<String, Object> result = orderService.rejectPayment(orderId, reason);
            
            if ((Boolean) result.get("success")) {
                response.put("success", true);
                response.put("message", result.get("message"));
                response.put("data", result);
                return ResponseEntity.ok(response);
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Payment webhook/callback
     * POST /api/orders/payment-callback
     */
    @PostMapping("/payment-callback")
    public ResponseEntity<Map<String, Object>> paymentCallback(
            @RequestBody Map<String, Object> request
    ) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long orderId = ((Number) request.get("orderId")).longValue();
            String paymentStatus = (String) request.get("status");
            String transactionRef = (String) request.get("transactionRef");
            
            Map<String, Object> result = orderService.updatePaymentStatus(
                orderId, paymentStatus, transactionRef
            );
            
            if ((Boolean) result.get("success")) {
                response.put("success", true);
                response.put("message", "Payment callback processed successfully");
                response.put("data", result);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", result.get("message"));
                response.put("data", result);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Cancel order
     * DELETE /api/orders/{orderId}
     */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> cancelOrder(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long orderId
    ) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long tokenUserId = extractUserIdFromToken(authHeader);
            String role = extractRoleFromToken(authHeader);
            
            Order order = orderService.getOrderById(orderId);
            
            if (!order.getUserId().equals(tokenUserId) 
                    && !"admin".equalsIgnoreCase(role) 
                    && !"owner".equalsIgnoreCase(role)) {
                response.put("success", false);
                response.put("message", "Access denied.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            Map<String, Object> result = orderService.cancelOrder(orderId);
            
            if ((Boolean) result.get("success")) {
                response.put("success", true);
                response.put("message", result.get("message"));
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}