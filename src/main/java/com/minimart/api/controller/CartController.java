package com.minimart.api.controller;

import com.minimart.api.model.Cart;
import com.minimart.api.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "*")
public class CartController {
    
    @Autowired
    private CartService cartService;
    
    /**
     * Get user's cart
     * GET /api/cart/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getUserCart(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Cart> cartItems = cartService.getUserCart(userId);
            response.put("success", true);
            response.put("data", cartItems);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error fetching cart: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get cart summary (total items, total price)
     * GET /api/cart/{userId}/summary
     */
    @GetMapping("/{userId}/summary")
    public ResponseEntity<Map<String, Object>> getCartSummary(@PathVariable Long userId) {
        try {
            Map<String, Object> summary = cartService.getCartSummary(userId);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error fetching cart summary: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get cart item count
     * GET /api/cart/{userId}/count
     */
    @GetMapping("/{userId}/count")
    public ResponseEntity<Map<String, Object>> getCartCount(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long count = cartService.getCartCount(userId);
            response.put("success", true);
            response.put("count", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error fetching cart count: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Add product to cart
     * POST /api/cart/add
     * Body: { "userId": 1, "productId": 5, "qty": 2 }
     */
    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addToCart(@RequestBody Map<String, Object> request) {
        try {
            // Safe type conversion with null checks
            Long userId = null;
            Integer productId = null;
            Integer qty = null;
            
            if (request.get("userId") != null) {
                Object userIdObj = request.get("userId");
                if (userIdObj instanceof Number) {
                    userId = ((Number) userIdObj).longValue();
                }
            }
            
            if (request.get("productId") != null) {
                Object productIdObj = request.get("productId");
                if (productIdObj instanceof Number) {
                    productId = ((Number) productIdObj).intValue();
                }
            }
            
            if (request.get("qty") != null) {
                Object qtyObj = request.get("qty");
                if (qtyObj instanceof Number) {
                    qty = ((Number) qtyObj).intValue();
                }
            }
            
            if (userId == null || productId == null || qty == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "userId, productId, and qty are required");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (qty <= 0) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Quantity must be greater than 0");
                return ResponseEntity.badRequest().body(response);
            }
            
            Map<String, Object> result = cartService.addToCart(userId, productId, qty);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error adding to cart: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Update cart item quantity
     * PUT /api/cart/{cartId}
     * Body: { "qty": 5 }
     */
    @PutMapping("/{cartId}")
    public ResponseEntity<Map<String, Object>> updateCartQuantity(
            @PathVariable Integer cartId,
            @RequestBody Map<String, Object> request) {
        try {
            Integer qty = null;
            
            if (request.get("qty") != null) {
                Object qtyObj = request.get("qty");
                if (qtyObj instanceof Number) {
                    qty = ((Number) qtyObj).intValue();
                }
            }
            
            if (qty == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "qty is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            Map<String, Object> result = cartService.updateCartQuantity(cartId, qty);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error updating cart: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Remove item from cart
     * DELETE /api/cart/{cartId}
     */
    @DeleteMapping("/{cartId}")
    public ResponseEntity<Map<String, Object>> removeFromCart(@PathVariable Integer cartId) {
        try {
            Map<String, Object> result = cartService.removeFromCart(cartId);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error removing from cart: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Clear user's entire cart
     * DELETE /api/cart/clear/{userId}
     */
    @DeleteMapping("/clear/{userId}")
    public ResponseEntity<Map<String, Object>> clearCart(@PathVariable Long userId) {
        try {
            Map<String, Object> result = cartService.clearCart(userId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error clearing cart: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}