package com.minimart.api.service;

import com.minimart.api.model.Cart;
import com.minimart.api.model.Product;
import com.minimart.api.model.User;
import com.minimart.api.repository.CartRepository;
import com.minimart.api.repository.ProductRepository;
import com.minimart.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CartService {
    
    @Autowired
    private CartRepository cartRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    // Get user's cart with all items
    public List<Cart> getUserCart(Long userId) {
        return cartRepository.findByUserId(userId);
    }
    
    // Add product to cart (or update quantity if already exists)
    @Transactional
    public Map<String, Object> addToCart(Long userId, Integer productId, Integer qty) {
        Map<String, Object> response = new HashMap<>();
        
        // Validate user
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            response.put("success", false);
            response.put("message", "User not found");
            return response;
        }
        
        // Validate product
        Optional<Product> productOpt = productRepository.findById(productId);
        if (!productOpt.isPresent()) {
            response.put("success", false);
            response.put("message", "Product not found");
            return response;
        }
        
        Product product = productOpt.get();
        
        // Get available stock once
        Integer availableStock = (product.getStock() != null && product.getStock().getQty() != null) 
                                  ? product.getStock().getQty() : 0;
        
        // Check stock availability
        if (availableStock < qty) {
            response.put("success", false);
            response.put("message", "Insufficient stock. Available: " + availableStock);
            return response;
        }
        
        // Check if item already exists in cart
        Optional<Cart> existingCart = cartRepository.findByUserIdAndProductId(userId, productId);
        
        Cart cart;
        if (existingCart.isPresent()) {
            // Update existing cart item
            cart = existingCart.get();
            Integer newQty = cart.getQty() + qty;
            
            // Check if new quantity exceeds stock
            if (newQty > availableStock) {
                response.put("success", false);
                response.put("message", "Cannot add more. Total would exceed available stock");
                return response;
            }
            
            cart.setQty(newQty);
            response.put("message", "Cart item updated successfully");
        } else {
            // Create new cart item
            cart = new Cart();
            cart.setUser(userOpt.get());
            cart.setProduct(product);
            cart.setQty(qty);
            response.put("message", "Product added to cart successfully");
        }
        
        Cart savedCart = cartRepository.save(cart);
        
        response.put("success", true);
        response.put("cart", savedCart);
        return response;
    }
    
    // Update cart item quantity
    @Transactional
    public Map<String, Object> updateCartQuantity(Integer cartId, Integer qty) {
        Map<String, Object> response = new HashMap<>();
        
        Optional<Cart> cartOpt = cartRepository.findById(cartId);
        if (!cartOpt.isPresent()) {
            response.put("success", false);
            response.put("message", "Cart item not found");
            return response;
        }
        
        Cart cart = cartOpt.get();
        Product product = cart.getProduct();
        
        // Validate quantity
        if (qty <= 0) {
            response.put("success", false);
            response.put("message", "Quantity must be greater than 0");
            return response;
        }
        
        // Check stock
        Integer availableStock = (product.getStock() != null && product.getStock().getQty() != null) 
                                  ? product.getStock().getQty() : 0;
        
        if (qty > availableStock) {
            response.put("success", false);
            response.put("message", "Insufficient stock. Available: " + availableStock);
            return response;
        }
        
        cart.setQty(qty);
        Cart updatedCart = cartRepository.save(cart);
        
        response.put("success", true);
        response.put("message", "Cart quantity updated successfully");
        response.put("cart", updatedCart);
        return response;
    }
    
    // Remove item from cart
    @Transactional
    public Map<String, Object> removeFromCart(Integer cartId) {
        Map<String, Object> response = new HashMap<>();
        
        if (!cartRepository.existsById(cartId)) {
            response.put("success", false);
            response.put("message", "Cart item not found");
            return response;
        }
        
        cartRepository.deleteById(cartId);
        
        response.put("success", true);
        response.put("message", "Item removed from cart successfully");
        return response;
    }
    
    // Clear user's entire cart
    @Transactional
    public Map<String, Object> clearCart(Long userId) {
        Map<String, Object> response = new HashMap<>();
        
        cartRepository.deleteByUserId(userId);
        
        response.put("success", true);
        response.put("message", "Cart cleared successfully");
        return response;
    }
    
    // Get cart summary (total items, total price)
    public Map<String, Object> getCartSummary(Long userId) {
        Map<String, Object> summary = new HashMap<>();
        List<Cart> cartItems = cartRepository.findByUserId(userId);
        
        int totalItems = 0;
        BigDecimal totalPrice = BigDecimal.ZERO;
        
        for (Cart item : cartItems) {
            totalItems += item.getQty();
            totalPrice = totalPrice.add(item.getSubtotal());
        }
        
        summary.put("totalItems", totalItems);
        summary.put("totalPrice", totalPrice);
        summary.put("itemCount", cartItems.size());
        summary.put("items", cartItems);
        
        return summary;
    }
    
    // Get cart item count for user
    public Long getCartCount(Long userId) {
        return cartRepository.countByUserId(userId);
    }
}