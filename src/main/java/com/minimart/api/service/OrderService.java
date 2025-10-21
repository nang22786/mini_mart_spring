package com.minimart.api.service;

import com.minimart.api.model.Order;
import com.minimart.api.model.OrderDetail;
import com.minimart.api.model.Payment;
import com.minimart.api.model.Product;
import com.minimart.api.model.Stock;
import com.minimart.api.repository.OrderRepository;
import com.minimart.api.repository.OrderDetailRepository;
import com.minimart.api.repository.PaymentRepository;
import com.minimart.api.repository.ProductRepository;
import com.minimart.api.repository.StockRepository;
import com.minimart.api.dto.OrderDTO;
import com.minimart.api.dto.OrderSummaryDTO;
import com.minimart.api.dto.PaymentDTO;
import com.minimart.api.dto.OrderDetailDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import com.minimart.api.dto.PendingOrderDTO;
import com.minimart.api.repository.UserRepository;
import com.minimart.api.model.User;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;
import java.io.IOException;
@Service
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private OrderDetailRepository orderDetailRepository;
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private StockRepository stockRepository;
    
    @Autowired
    private UserRepository userRepository;
    
	/**
	 * Create order ONLY (no payment yet)
	 * User will upload payment screenshot later
	 */
	@Transactional
	public Map<String, Object> createOrder(
	        Long userId,
	        BigDecimal amount,
	        List<Map<String, Object>> items,
	        Long addressId
	) {
	    try {
	        // 1. Validate stock availability
	        for (Map<String, Object> item : items) {
	            Integer productId = ((Number) item.get("productId")).intValue();
	            Integer qty = (Integer) item.get("qty");
	            
	            Product product = productRepository.findById(productId)
	                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
	            
	            Stock stock = product.getStock();
	            if (stock == null) {
	                throw new RuntimeException("Stock not found for product: " + product.getName());
	            }
	            
	            if (stock.getQty() < qty) {
	                throw new RuntimeException("Insufficient stock for product: " + product.getName() 
	                    + ". Available: " + stock.getQty() + ", Requested: " + qty);
	            }
	        }
	        
	        // 2. Create Order ONLY (no payment yet!)
	        Order order = new Order();
	        order.setUserId(userId);
	        order.setAmount(amount);
	        order.setStatus("pending");
	        order.setAddressId(addressId);
	        order = orderRepository.save(order);
	        
	        // 3. Create Order Details
	        for (Map<String, Object> item : items) {
	            Integer productId = ((Number) item.get("productId")).intValue();
	            Integer qty = (Integer) item.get("qty");
	            BigDecimal price = new BigDecimal(item.get("price").toString());
	            
	            OrderDetail orderDetail = new OrderDetail();
	            orderDetail.setProductId(productId);
	            orderDetail.setQty(qty);
	            orderDetail.setPrice(price);
	            orderDetail.setOrder(order);
	            
	            orderDetailRepository.save(orderDetail);
	        }
	        
	        // 4. Return response
	        Map<String, Object> response = new HashMap<>();
	        response.put("success", true);
	        response.put("message", "Order created successfully. Please upload payment screenshot.");
	        response.put("orderId", order.getId());
	        response.put("status", "pending");
	        response.put("amount", order.getAmount());
	        response.put("addressId", order.getAddressId());
	        
	        return response;
	        
	    } catch (Exception e) {
	        Map<String, Object> response = new HashMap<>();
	        response.put("success", false);
	        response.put("message", "Error creating order: " + e.getMessage());
	        return response;
	    }
	}
    
    /**
     * Confirm payment (Admin verifies payment in bank account)
     * Changes order status from "pending" to "paid" and deducts stock
     */
    @Transactional
    public Map<String, Object> confirmPayment(Long orderId) {
        try {
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
            
            if (!"pending".equals(order.getStatus())) {
                throw new RuntimeException("Order is not pending. Current status: " + order.getStatus());
            }
            
            Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
            
            payment.setStatus("paid");
            payment.setPayDate(LocalDateTime.now());
            paymentRepository.save(payment);
            
            order.setStatus("paid");
            orderRepository.save(order);
            
            List<OrderDetail> orderDetails = orderDetailRepository.findByOrder(order);
            
            for (OrderDetail detail : orderDetails) {
                Integer productId = detail.getProductId();
                
                Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
                
                Stock stock = product.getStock();
                if (stock == null) {
                    throw new RuntimeException("Stock not found for product: " + product.getName());
                }
                
                int newQty = stock.getQty() - detail.getQty();
                if (newQty < 0) {
                    throw new RuntimeException("Insufficient stock for product: " + product.getName());
                }
                
                stock.setQty(newQty);
                stockRepository.save(stock);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Payment confirmed and stock deducted successfully");
            response.put("orderId", order.getId());
            response.put("status", "paid");
            
            return response;
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error confirming payment: " + e.getMessage());
            return response;
        }
    }
    
    /**
     * Reject payment (Admin marks payment as failed)
     * Changes order status from "pending" to "failed"
     * Stock is NOT deducted
     */
    @Transactional
    public Map<String, Object> rejectPayment(Long orderId, String reason) {
        try {
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
            
            if (!"pending".equals(order.getStatus())) {
                throw new RuntimeException("Order is not pending. Current status: " + order.getStatus());
            }
            
            Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
            
            payment.setStatus("failed");
            paymentRepository.save(payment);
            
            order.setStatus("failed");
            orderRepository.save(order);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Payment rejected: " + (reason != null ? reason : "Invalid payment"));
            response.put("orderId", order.getId());
            response.put("status", "failed");
            
            return response;
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error rejecting payment: " + e.getMessage());
            return response;
        }
    }
    
    /**
     * Update payment status from payment gateway callback
     * This is called automatically when Bakong/payment gateway confirms payment
     */
    @Transactional
    public Map<String, Object> updatePaymentStatus(
            Long orderId, 
            String paymentStatus, 
            String transactionRef
    ) {
        try {
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
            
            Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
            
            // Only update if order is still pending
            if (!"pending".equals(order.getStatus())) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Order is not pending. Current status: " + order.getStatus());
                return response;
            }
            
            // Handle payment success
            if ("success".equalsIgnoreCase(paymentStatus) || "paid".equalsIgnoreCase(paymentStatus)) {
                payment.setStatus("paid");
                payment.setPayDate(LocalDateTime.now());
                paymentRepository.save(payment);
                
                order.setStatus("paid");
                orderRepository.save(order);
                
                // Deduct stock
                List<OrderDetail> orderDetails = orderDetailRepository.findByOrder(order);
                for (OrderDetail detail : orderDetails) {
                    Integer productId = detail.getProductId();
                    Product product = productRepository.findById(productId)
                        .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
                    
                    Stock stock = product.getStock();
                    if (stock == null) {
                        throw new RuntimeException("Stock not found for product: " + product.getName());
                    }
                    
                    int newQty = stock.getQty() - detail.getQty();
                    if (newQty < 0) {
                        throw new RuntimeException("Insufficient stock for product: " + product.getName());
                    }
                    
                    stock.setQty(newQty);
                    stockRepository.save(stock);
                }
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Payment confirmed automatically");
                response.put("orderId", order.getId());
                response.put("status", "paid");
                response.put("transactionRef", transactionRef);
                return response;
            }
            
            // Handle payment failure
            if ("failed".equalsIgnoreCase(paymentStatus) || "rejected".equalsIgnoreCase(paymentStatus)) {
                payment.setStatus("failed");
                paymentRepository.save(payment);
                
                order.setStatus("failed");
                orderRepository.save(order);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Payment failed");
                response.put("orderId", order.getId());
                response.put("status", "failed");
                return response;
            }
            
            // Unknown status
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Unknown payment status: " + paymentStatus);
            return response;
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error updating payment status: " + e.getMessage());
            return response;
        }
    }
    
    /**
     * Check payment status (for polling from Flutter app)
     */
    public Map<String, Object> checkPaymentStatus(Long orderId) {
        try {
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
            
            Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orderId", order.getId());
            response.put("orderStatus", order.getStatus());
            response.put("paymentStatus", payment.getStatus());
            response.put("amount", order.getAmount());
            response.put("paymentMethod", payment.getPaymentMethod());
            response.put("createdAt", order.getCreatedAt());
            
            return response;
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return response;
        }
    }
    
    /**
     * Get all orders for a user
     */
    public List<Order> getUserOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    /**
     * Get user orders as summary (lightweight for list view)
     */
    public List<OrderSummaryDTO> getUserOrdersSummary(Long userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<OrderSummaryDTO> summaries = new ArrayList<>();
        
        for (Order order : orders) {
            int itemCount = orderDetailRepository.findByOrder(order).size();
            Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
            LocalDateTime payDate = payment != null ? payment.getPayDate() : null;
            summaries.add(new OrderSummaryDTO(
                order.getId(),
                order.getUserId(),
                order.getStatus(),
                order.getAmount(),
                order.getCreatedAt(),
                itemCount,
                order.getAddressId(),
                payDate
            ));
        }
        
        return summaries;
    }
    
    /**
     * Get order by ID with details
     */
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found"));
    }
    
    /**
     * Get order details with product info (for single order view)
     */
    public OrderDTO getOrderDetails(Long orderId) {
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new RuntimeException("Order not found"));
    
    OrderDTO dto = new OrderDTO(
        order.getId(),
        order.getStatus(),
        order.getAmount(),
        order.getCreatedAt(),
        order.getUpdatedAt(),
        order.getUserId(),
        order.getAddressId()
    );
    
    // Get order items
    List<OrderDetail> details = orderDetailRepository.findByOrder(order);
    List<OrderDetailDTO> itemDTOs = new ArrayList<>();
    
    for (OrderDetail detail : details) {
        Product product = productRepository.findById(detail.getProductId()).orElse(null);
        
        OrderDetailDTO itemDTO = new OrderDetailDTO(
            detail.getId(),
            detail.getProductId(),
            product != null ? product.getName() : "Unknown Product",
            product != null ? product.getImage() : null,
            detail.getQty(),
            detail.getPrice()
        );
        itemDTOs.add(itemDTO);
    }
    
    dto.setItems(itemDTOs);
    
    // ✅ Get payment info
    Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
    if (payment != null) {
        dto.setPayment(convertToPaymentDTO(payment));
    }
    
    return dto;
}

    
    /**
     * Get all pending orders (for admin)
     */
    public List<Order> getPendingOrders() {
        return orderRepository.findByStatus("pending");
    }
    
    /**
     * Cancel order (only if still pending)
     */
    @Transactional
    public Map<String, Object> cancelOrder(Long orderId) {
        try {
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
            
            if (!"pending".equals(order.getStatus())) {
                throw new RuntimeException("Cannot cancel order. Current status: " + order.getStatus());
            }
            
            order.setStatus("failed");
            orderRepository.save(order);
            
            Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
            if (payment != null) {
                payment.setStatus("failed");
                paymentRepository.save(payment);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Order cancelled successfully");
            return response;
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return response;
        }
    }
    /**
     * Get pending orders with minimal data (for admin dashboard)
     */
    public List<PendingOrderDTO> getPendingOrdersSummary() {
        List<Order> orders = orderRepository.findByStatus("pending");
        List<PendingOrderDTO> summaries = new ArrayList<>();
        
        for (Order order : orders) {
            // Get user info
            User user = userRepository.findById(order.getUserId()).orElse(null);
            String userName = user != null ? user.getUserName() : "Unknown";
            String userEmail = user != null ? user.getEmail() : "Unknown";
            
            // Get payment method
            Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
            String paymentMethod = payment != null ? payment.getPaymentMethod() : "N/A";
            
            // Count items
            int itemCount = orderDetailRepository.findByOrder(order).size();
            
            summaries.add(new PendingOrderDTO(
                order.getId(),
                order.getUserId(),
                userName,
                userEmail,
                order.getAmount(),
                paymentMethod,
                order.getCreatedAt(),
                itemCount
            ));
        }
        
        return summaries;
    }
    @Autowired
    private OCRService ocrService;

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * Upload payment screenshot and verify automatically
     * This is where Payment record is created!
     */
    @Transactional
    public Map<String, Object> uploadPaymentScreenshot(
            Long userId,
            Long orderId,
            MultipartFile screenshot
    ) {
        String fileName = null;
        String screenshotPath = null;
        
        try {
            // 1. Get order and verify ownership
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
            
            if (!order.getUserId().equals(userId)) {
                throw new RuntimeException("Access denied. This order doesn't belong to you.");
            }
            
            // 2. Check order status
            if (!"pending".equals(order.getStatus())) {
                throw new RuntimeException("Order is not pending. Current status: " + order.getStatus());
            }
            
            // 3. Check if payment already exists
            Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
            
            // ✅ Create payment record if doesn't exist!
            if (payment == null) {
                payment = new Payment();
                payment.setOrderId(orderId);
                payment.setUserId(userId);
                payment.setAmount(order.getAmount());
                payment.setPaymentMethod("Bank Transfer");  // Default
                payment.setCurrency("USD");  // Default
                payment.setStatus("pending");
                payment = paymentRepository.save(payment);
            }
            
            // 4. Save screenshot file
            fileName = fileStorageService.storeFile(screenshot, "payment");
            screenshotPath = "/api/files/payments/" + fileName;
            
            System.out.println("✅ Screenshot uploaded: " + fileName);
            
            // 5. Get the actual file location
            Path uploadPath = fileStorageService.getFileStorageLocation("payment");
            File imageFile = uploadPath.resolve(fileName).toFile();
            
            System.out.println("📁 Looking for file at: " + imageFile.getAbsolutePath());
            System.out.println("📂 File exists: " + imageFile.exists());
            
            // 6. Extract text from screenshot using OCR
            String extractedText;
            try {
                extractedText = ocrService.extractText(imageFile);
            } catch (IOException e) {
                System.err.println("❌ OCR failed: " + e.getMessage());
                fileStorageService.deleteFile(fileName, "payment");
                System.out.println("🗑️ Deleted uploaded file due to OCR failure");
                throw new RuntimeException("OCR failed: " + e.getMessage());
            }
            
            // 7. Extract Transaction ID
            String transactionId = ocrService.extractTransactionId(extractedText);
            
            if (transactionId == null || transactionId.trim().isEmpty()) {
                fileStorageService.deleteFile(fileName, "payment");
                System.out.println("🗑️ Deleted uploaded file - no Transaction ID found");
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Could not find Transaction ID in screenshot. Please upload a clear payment screenshot.");
                response.put("orderId", orderId);
                response.put("status", "pending");
                return response;
            }
            
            System.out.println("✅ Transaction ID extracted: " + transactionId);
            
            // 8. Check if Transaction ID already used
            if (paymentRepository.existsByTransactionId(transactionId)) {
                fileStorageService.deleteFile(fileName, "payment");
                System.out.println("❌ Duplicate transaction detected: " + transactionId);
                System.out.println("🗑️ Deleted uploaded file - duplicate transaction");
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "This payment screenshot has already been used. Transaction ID: " + transactionId);
                response.put("orderId", orderId);
                response.put("status", "pending");
                response.put("transactionId", transactionId);
                return response;
            }
            
            // 9. Extract Transaction Date
            LocalDateTime transactionDate = ocrService.extractTransactionDate(extractedText);
            
            if (transactionDate != null) {
                System.out.println("✅ Transaction Date extracted: " + transactionDate);
            } else {
                System.out.println("⚠️ Could not extract Transaction Date (will continue anyway)");
            }
            
            // 10. Verify amount
            boolean amountMatches = ocrService.verifyPayment(extractedText, order.getAmount());
            
            if (!amountMatches) {
                System.out.println("❌ Amount verification failed - deleting file");
                fileStorageService.deleteFile(fileName, "payment");
                System.out.println("🗑️ Deleted uploaded file due to amount mismatch");
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Amount in screenshot doesn't match order amount. Expected: $" + order.getAmount() + ". Please upload correct payment screenshot.");
                response.put("orderId", orderId);
                response.put("status", "pending");
                response.put("transactionId", transactionId);
                return response;
            }
            
            System.out.println("✅ Amount verification successful!");
            
            // 11. Save transaction details & confirm payment
            payment.setScreenshotPath(screenshotPath);
            payment.setTransactionId(transactionId);
            payment.setTransactionDate(transactionDate);
            payment.setStatus("paid");
            payment.setPayDate(LocalDateTime.now());
            paymentRepository.save(payment);
            
            order.setStatus("paid");
            orderRepository.save(order);
            
            System.out.println("✅ Payment status updated to PAID");
            
            // 12. Deduct stock
            List<OrderDetail> orderDetails = orderDetailRepository.findByOrder(order);
            for (OrderDetail detail : orderDetails) {
                Integer productId = detail.getProductId();
                Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
                
                Stock stock = product.getStock();
                if (stock == null) {
                    throw new RuntimeException("Stock not found for product: " + product.getName());
                }
                
                int newQty = stock.getQty() - detail.getQty();
                if (newQty < 0) {
                    throw new RuntimeException("Insufficient stock for product: " + product.getName());
                }
                
                stock.setQty(newQty);
                stockRepository.save(stock);
            }
            
            System.out.println("✅ Stock deducted successfully");
            
            // 13. Success!
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Payment verified and confirmed automatically!");
            response.put("orderId", orderId);
            response.put("status", "paid");
            response.put("transactionId", transactionId);
            response.put("transactionDate", transactionDate != null ? transactionDate.toString() : "N/A");
            response.put("extractedText", extractedText);
            
            System.out.println("🎉 Payment verification successful!");
            
            return response;
            
        } catch (Exception e) {
            // Any error - delete uploaded file if it exists!
            if (fileName != null) {
                try {
                    fileStorageService.deleteFile(fileName, "payment");
                    System.out.println("🗑️ Deleted uploaded file due to error: " + e.getMessage());
                } catch (Exception deleteError) {
                    System.err.println("⚠️ Failed to delete file: " + deleteError.getMessage());
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error processing screenshot: " + e.getMessage());
            e.printStackTrace();
            return response;
        }
    }
	
	/**
	 * Get all orders summary (for admin dashboard)
	 */
	public List<OrderSummaryDTO> getAllOrdersSummary() {
	    // ✅ Use the corrected method
	    List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();
	    List<OrderSummaryDTO> summaries = new ArrayList<>();
	    
	    for (Order order : orders) {
	        int itemCount = orderDetailRepository.findByOrder(order).size();
	        Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
	        LocalDateTime payDate = payment != null ? payment.getPayDate() : null;
	        summaries.add(new OrderSummaryDTO(
	            order.getId(),
	            order.getUserId(),
	            order.getStatus(),
	            order.getAmount(),
	            order.getCreatedAt(),
	            itemCount,
	            order.getAddressId(),
	            payDate
	        ));
	    }
	    
	    return summaries;
	}
	
	/**
	 * Convert Payment entity to PaymentDTO
	 */
	private PaymentDTO convertToPaymentDTO(Payment payment) {
	    if (payment == null) {
	        return null;
	    }
	    
	    return new PaymentDTO(
	        payment.getId(),
	        payment.getOrderId(),
	        payment.getUserId(),
	        payment.getAmount(),
	        payment.getPaymentMethod(),
	        payment.getCurrency(),
	        payment.getScreenshotPath(),
	        payment.getTransactionId(),
	        payment.getTransactionDate(),
	        payment.getStatus(),
	        payment.getPayDate(),
	        payment.getCreatedAt()
	    );
	}
}