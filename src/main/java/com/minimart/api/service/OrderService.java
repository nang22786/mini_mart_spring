package com.minimart.api.service;

import com.minimart.api.dto.bakong.BakongTransactionResponse;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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
    private EmailService emailService;

    @Autowired
    private TelegramNotificationService telegramService;

    @Autowired
    private BakongKHQRService bakongKHQRService;

    @Value("${bakong.api.base-url}")
    private String bakongApiBaseUrl;

    @Value("${bakong.api.token}")
    private String bakongApiToken;

    // ============================================================
    // 🆕 NEW: CHECKOUT WITH KHQR (Order Before Payment Flow)
    // ============================================================

    /**
     * 🆕 CHECKOUT: Create Order (pending) + Generate KHQR
     * POST /api/orders/checkout
     */
    @Transactional
    public Map<String, Object> checkout(
            Long userId,
            BigDecimal amount,
            List<Map<String, Object>> items,
            Long addressId) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 🔒 SAFETY #3: Prevent duplicate pending orders
            List<Order> pendingOrders = orderRepository.findByUserIdAndStatus(userId, "pending");
            if (!pendingOrders.isEmpty()) {
                response.put("success", false);
                response.put("message", "You already have a pending order. Please complete or wait for it to expire.");
                response.put("pendingOrderId", pendingOrders.get(0).getId());
                return response;
            }

            // 1. Validate stock availability (but DON'T deduct yet!)
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

            // 2. Create Order with status "pending"
            Order order = new Order();
            order.setUserId(userId);
            order.setAmount(amount);
            order.setStatus("pending");
            order.setAddressId(addressId);
            order = orderRepository.save(order);

            System.out.println("✅ Order created - Order #" + order.getId() + " (status: pending)");

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

            // 4. Generate KHQR
            String billNumber = "ORDER-" + order.getId();
            BakongKHQRService.KHQRGenerationResult khqrResult = bakongKHQRService.generateKHQR(billNumber, amount);

            if (!khqrResult.isSuccess()) {
                response.put("success", false);
                response.put("message", "Order created but KHQR generation failed: " + khqrResult.getError());
                response.put("orderId", order.getId());
                return response;
            }

            System.out.println("✅ KHQR generated - MD5: " + khqrResult.getMd5());

            // 5. Create Payment record with KHQR details
            Payment payment = new Payment();
            payment.setOrderId(order.getId());
            payment.setUserId(userId);
            payment.setAmount(amount);
            payment.setCurrency("USD");
            payment.setPaymentMethod("KHQR");
            payment.setStatus("pending");
            payment.setKhqrMd5(khqrResult.getMd5());
            payment.setKhqrQr(khqrResult.getQrCode());
            payment.setExpiresAt(LocalDateTime.now().plusMinutes(5));
            payment.setCreatedAt(LocalDateTime.now());
            payment = paymentRepository.save(payment);

            System.out.println("✅ Payment record created - Payment #" + payment.getId());
            System.out.println("   Expires at: " + payment.getExpiresAt());

            // 6. Start background monitoring
            startPaymentMonitoring(payment.getId(), order.getId());

            // 🔒 NO notifications here! Only when status = "paid"
            System.out.println("⏳ Waiting for payment... Notifications will be sent when order is paid.");

            // 7. Return response to Flutter
            response.put("success", true);
            response.put("message", "Order created successfully. Please scan QR code to pay.");
            response.put("orderId", order.getId());
            response.put("paymentId", payment.getId());
            response.put("qrCode", khqrResult.getQrCode());
            response.put("amount", amount);
            response.put("status", "pending");
            response.put("expiresAt", payment.getExpiresAt().toString());

            return response;

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error during checkout: " + e.getMessage());
            e.printStackTrace();
            return response;
        }
    }

    /**
     * Start background payment monitoring
     */
    private void startPaymentMonitoring(Long paymentId, Long orderId) {
        CompletableFuture.runAsync(() -> {
            monitorPaymentStatus(paymentId, orderId);
        });
    }

    /**
     * Monitor payment status by checking Bakong API every 10 seconds for 5 minutes
     */
    private void monitorPaymentStatus(Long paymentId, Long orderId) {
        try {
            System.out.println("🔍 Starting payment monitoring - Payment #" + paymentId);

            // ✅ ADD INITIAL DELAY - Wait 30 seconds before first check
            System.out.println("⏳ Waiting 30 seconds before first check...");
            Thread.sleep(30000); // Wait 30 seconds FIRST

            // Check every 30 seconds for 5 minutes (10 checks)
            for (int i = 0; i < 10; i++) {
                Payment payment = paymentRepository.findById(paymentId).orElse(null);
                if (payment == null) {
                    System.err.println("❌ Payment not found: " + paymentId);
                    return;
                }

                // Check if khqr_md5 exists
                if (payment.getKhqrMd5() == null || payment.getKhqrMd5().isEmpty()) {
                    System.err.println("❌ Cannot monitor - Payment #" + paymentId + " has NULL khqr_md5");
                    updateOrderToFailed(orderId, paymentId);
                    return;
                }

                // If already paid or failed, stop monitoring
                if ("paid".equals(payment.getStatus()) || "failed".equals(payment.getStatus())) {
                    System.out.println(
                            "⏹️ Monitoring stopped - Payment #" + paymentId + " status: " + payment.getStatus());
                    return;
                }

                // Check if expired
                if (LocalDateTime.now().isAfter(payment.getExpiresAt())) {
                    System.out.println("⏱️ Payment expired - Payment #" + paymentId);
                    updateOrderToFailed(orderId, paymentId);
                    return;
                }

                // Check payment status with Bakong API
                System.out.println("🔍 Checking Bakong API - Payment #" + paymentId + " (attempt " + (i + 1) + "/10)");

                boolean isPaid = checkBakongPayment(payment);

                if (isPaid) {
                    System.out.println("✅ Payment confirmed by Bakong! - Payment #" + paymentId);
                    updateOrderToPaid(orderId, paymentId);
                    return;
                }

                // Wait 30 seconds before next check
                if (i < 9) { // Don't sleep after last attempt
                    System.out.println("⏳ Waiting 30 seconds before next check...");
                    Thread.sleep(30000); // Wait 30 seconds between checks
                }
            }

            // If we reach here, payment timed out
            System.out.println("⏱️ Payment monitoring timeout (5 min) - Payment #" + paymentId);
            updateOrderToFailed(orderId, paymentId);

        } catch (InterruptedException e) {
            System.err.println("❌ Monitoring interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("❌ Error monitoring payment: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Check Bakong API for payment confirmation
     */
    private boolean checkBakongPayment(Payment payment) {
        try {
            // String url = bakongApiBaseUrl + "/v1/check_transaction_by_md5";
            String url = "https://api-bakong.nbc.gov.kh/v1/check_transaction_by_md5";

            HttpHeaders headers = new HttpHeaders();
            // headers.set("Authorization", "Bearer " + bakongApiToken);
            headers.set("Authorization",
                    "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJkYXRhIjp7ImlkIjoiODJlODk4NDcxMjU5NDFhNSJ9LCJpYXQiOjE3NjEzMTA5ODAsImV4cCI6MTc2OTA4Njk4MH0.j4A8jvYII8niPGTamo31_pyf-Qu_H_SzV0mWobXeKR0");
            headers.set("Content-Type", "application/json");

            Map<String, String> body = new HashMap<>();
            body.put("md5", payment.getKhqrMd5());

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<BakongTransactionResponse> responseEntity = restTemplate.exchange(url, HttpMethod.POST,
                    request, BakongTransactionResponse.class);

            BakongTransactionResponse bakongResponse = responseEntity.getBody();

            if (bakongResponse != null && bakongResponse.isPaid()) {
                BigDecimal paidAmount = bakongResponse.getData().getAmount();

                // Verify amount matches
                if (paidAmount.compareTo(payment.getAmount()) == 0) {
                    // Save transaction ID
                    payment.setTransactionId(bakongResponse.getData().getHash());
                    paymentRepository.save(payment);
                    return true;
                } else {
                    System.err.println("❌ Amount mismatch - Expected: " + payment.getAmount() + ", Got: " + paidAmount);
                    return false;
                }
            }

            return false;

        } catch (Exception e) {
            System.err.println("❌ Bakong API error: " + e.getMessage());
            return false;
        }
    }

    /**
     * 🔒 SAFETY #2: Update order to PAID and deduct stock
     */
    @Transactional
    public void updateOrderToPaid(Long orderId, Long paymentId) {
        try {
            Order order = orderRepository.findById(orderId).orElse(null);
            Payment payment = paymentRepository.findById(paymentId).orElse(null);

            if (order == null || payment == null) {
                System.err.println("❌ Order or Payment not found");
                return;
            }

            // Update payment status
            payment.setStatus("paid");
            payment.setPayDate(LocalDateTime.now());
            paymentRepository.save(payment);

            // Update order status
            order.setStatus("paid");
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            System.out.println("✅ Order #" + orderId + " status updated to PAID");

            // 🔒 Deduct stock ONLY when status = "paid"
            List<OrderDetail> orderDetails = orderDetailRepository.findByOrder(order);

            for (OrderDetail detail : orderDetails) {
                Integer productId = detail.getProductId();
                Product product = productRepository.findById(productId).orElse(null);

                if (product != null && product.getStock() != null) {
                    Stock stock = product.getStock();
                    int newQty = stock.getQty() - detail.getQty();
                    stock.setQty(Math.max(newQty, 0)); // Prevent negative stock
                    stockRepository.save(stock);

                    System.out.println("✅ Stock deducted - Product #" + productId + " - New qty: " + newQty);
                }
            }

            System.out.println("🎉 Payment successful! Order #" + orderId + " completed.");

            // 🆕 SEND NOTIFICATIONS (ONLY when status = "paid")
            try {
                // 1. Send to ORDER BOT (Telegram - owner)
                telegramService.sendOrderSuccessNotification(order);
                System.out.println("✅ Order notification sent to Telegram (owner)");

                // 2. Send to PAYMENT BOT (Telegram - owner)
                telegramService.sendPaymentNotification(order, payment);
                System.out.println("✅ Payment notification sent to Payment Bot (owner)");

                // 3. Send email to customer
                emailService.sendOrderSuccessEmail(order);
                System.out.println("✅ Order email sent to customer");

            } catch (Exception e) {
                System.err.println("⚠️ Failed to send notifications: " + e.getMessage());
                // Don't fail the whole transaction if notification fails
            }

        } catch (Exception e) {
            System.err.println("❌ Error updating order to paid: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Update order to FAILED (timeout)
     */
    @Transactional
    public void updateOrderToFailed(Long orderId, Long paymentId) {
        try {
            Order order = orderRepository.findById(orderId).orElse(null);
            Payment payment = paymentRepository.findById(paymentId).orElse(null);

            if (order == null || payment == null) {
                System.err.println("❌ Order or Payment not found");
                return;
            }

            // Update payment status
            payment.setStatus("failed");
            paymentRepository.save(payment);

            // Update order status
            order.setStatus("failed");
            orderRepository.save(order);

            System.out.println("❌ Order #" + orderId + " status updated to FAILED (timeout)");

        } catch (Exception e) {
            System.err.println("❌ Error updating order to failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 🔒 SAFETY #1: Cleanup old failed orders
     */
    @Transactional
    public Map<String, Object> cleanupFailedOrders() {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);

            List<Order> failedOrders = orderRepository.findByStatusAndCreatedAtBefore("failed", cutoffDate);

            int deletedCount = 0;
            for (Order order : failedOrders) {
                // Delete order details first
                List<OrderDetail> orderDetails = orderDetailRepository.findByOrder(order);
                orderDetailRepository.deleteAll(orderDetails);

                // Delete payment records
                paymentRepository.findByOrderId(order.getId()).ifPresent(payment -> {
                    paymentRepository.delete(payment);
                });

                // Delete order
                orderRepository.delete(order);
                deletedCount++;
            }

            System.out.println("🗑️ Cleaned up " + deletedCount + " failed orders older than 7 days");

            return Map.of(
                    "success", true,
                    "deletedCount", deletedCount,
                    "message", "Cleaned up " + deletedCount + " failed orders");

        } catch (Exception e) {
            System.err.println("❌ Error cleaning up failed orders: " + e.getMessage());
            return Map.of(
                    "success", false,
                    "message", "Error: " + e.getMessage());
        }
    }

    // ============================================================
    // 📊 VIEW METHODS (For Customer & Admin)
    // ============================================================

    /**
     * Get all orders for a user
     */
    public List<Order> getUserOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Get user orders as summary (for My Orders)
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
                    payDate,
                    order.getNotificationRead() != null ? order.getNotificationRead() : false // ✅ ADD THIS
            ));
        }

        return summaries;
    }

    /**
     * Get order by ID
     */
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    /**
     * Get order details with items and payment (for single order view)
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
                order.getAddressId(),
                order.getNotificationRead() != null ? order.getNotificationRead() : false // ✅ ADD THIS
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
                    detail.getPrice());
            itemDTOs.add(itemDTO);
        }

        dto.setItems(itemDTOs);

        // Get payment info
        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        if (payment != null) {
            dto.setPayment(convertToPaymentDTO(payment));
        }

        return dto;
    }

    /**
     * Get all orders summary (for admin dashboard)
     */
    public List<OrderSummaryDTO> getAllOrdersSummary() {
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
                    payDate,
                    order.getNotificationRead() != null ? order.getNotificationRead() : false // ✅ ADD THIS
            ));
        }

        return summaries;
    }

    /**
     * 🆕 UPDATE ORDER STATUS (Owner only)
     */
    public Map<String, Object> updateOrderStatus(Long orderId, String newStatus) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Find order
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

            String oldStatus = order.getStatus();

            // Update order status
            order.setStatus(newStatus);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);

            // Update payment status if exists
            Optional<Payment> paymentOpt = paymentRepository.findByOrderId(orderId);
            if (paymentOpt.isPresent()) {
                Payment payment = paymentOpt.get();
                payment.setStatus(newStatus);
                if ("paid".equals(newStatus)) {
                    payment.setPayDate(LocalDateTime.now());
                }
                paymentRepository.save(payment);
            }

            System.out.println("✅ Order #" + orderId + " status: " + oldStatus + " → " + newStatus);

            // Return simple data (convert LocalDateTime to String!)
            Map<String, Object> orderData = new HashMap<>();
            orderData.put("id", order.getId());
            orderData.put("userId", order.getUserId());
            orderData.put("status", order.getStatus());
            orderData.put("amount", order.getAmount());
            orderData.put("createdAt", order.getCreatedAt() != null ? order.getCreatedAt().toString() : null);
            orderData.put("updatedAt", order.getUpdatedAt() != null ? order.getUpdatedAt().toString() : null);
            orderData.put("addressId", order.getAddressId());

            result.put("success", true);
            result.put("message", "Order #" + orderId + " status updated to " + newStatus + " successfully");
            result.put("order", orderData);
            return result;

        } catch (Exception e) {
            System.err.println("❌ Error updating order status: " + e.getMessage());
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * 🆕 DELETE ORDER (Owner only)
     */
    public Map<String, Object> deleteOrder(Long orderId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Check if order exists
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

            // Delete payment if exists
            Optional<Payment> paymentOpt = paymentRepository.findByOrderId(orderId);
            if (paymentOpt.isPresent()) {
                paymentRepository.delete(paymentOpt.get());
                System.out.println("🗑️ Deleted payment for Order #" + orderId);
            }

            // Delete order details (using your existing method)
            List<OrderDetail> orderDetails = orderDetailRepository.findByOrder(order);
            if (!orderDetails.isEmpty()) {
                orderDetailRepository.deleteAll(orderDetails);
                System.out.println("🗑️ Deleted " + orderDetails.size() + " order details for Order #" + orderId);
            }

            // Delete order
            orderRepository.delete(order);
            System.out.println("🗑️ Deleted Order #" + orderId);

            result.put("success", true);
            result.put("message", "Order #" + orderId + " and all related data deleted successfully");
            return result;

        } catch (Exception e) {
            System.err.println("❌ Error deleting order: " + e.getMessage());
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
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
                payment.getTransactionId(),
                payment.getStatus(),
                payment.getPayDate(),
                payment.getCreatedAt());
    }
    // ============================================================
    // 🔔 NOTIFICATION METHODS
    // ============================================================

    /**
     * Mark failed order notification as read
     */
    @Transactional
    public Map<String, Object> markNotificationAsRead(Long orderId, Long userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Find order
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

            // Check if order belongs to user
            if (!order.getUserId().equals(userId)) {
                result.put("success", false);
                result.put("message", "Access denied. This order doesn't belong to you.");
                return result;
            }

            // Mark as read
            order.setNotificationRead(true);
            orderRepository.save(order);

            System.out.println("✅ Order #" + orderId + " notification marked as read by User #" + userId);

            result.put("success", true);
            result.put("message", "Notification marked as read");
            return result;

        } catch (Exception e) {
            System.err.println("❌ Error marking notification as read: " + e.getMessage());
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }

    /**
     * Mark all failed order notifications as read for a user
     */
    @Transactional
    public Map<String, Object> markAllFailedAsRead(Long userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Find all unread failed orders for user
            List<Order> failedOrders = orderRepository.findByUserIdAndStatus(userId, "failed");

            int markedCount = 0;
            for (Order order : failedOrders) {
                if (order.getNotificationRead() == null || !order.getNotificationRead()) {
                    order.setNotificationRead(true);
                    orderRepository.save(order);
                    markedCount++;
                }
            }

            System.out.println("✅ Marked " + markedCount + " failed order notifications as read for User #" + userId);

            result.put("success", true);
            result.put("message", "All failed order notifications marked as read");
            result.put("markedCount", markedCount);
            return result;

        } catch (Exception e) {
            System.err.println("❌ Error marking all failed as read: " + e.getMessage());
            result.put("success", false);
            result.put("message", e.getMessage());
            result.put("markedCount", 0);
            return result;
        }
    }

    /**
     * Get count of unread failed orders for a user
     */
    public Integer getUnreadFailedCount(Long userId) {
        try {
            List<Order> failedOrders = orderRepository.findByUserIdAndStatus(userId, "failed");

            int count = 0;
            for (Order order : failedOrders) {
                if (order.getNotificationRead() == null || !order.getNotificationRead()) {
                    count++;
                }
            }

            return count;

        } catch (Exception e) {
            System.err.println("❌ Error getting unread failed count: " + e.getMessage());
            return 0;
        }
    }

    // ============================================================
    // 🔄 RETRY PAYMENT FOR FAILED ORDER
    // ============================================================
    @Transactional
    public Map<String, Object> retryPayment(Long orderId, Long userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 1. Find the order
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

            System.out.println("🔄 Found Order #" + orderId + " (status: " + order.getStatus() + ")");

            // 2. Security check - verify user owns this order
            if (!order.getUserId().equals(userId)) {
                System.err.println("❌ Security: User #" + userId + " attempted to retry Order #" + orderId
                        + " (owned by User #" + order.getUserId() + ")");
                result.put("success", false);
                result.put("message", "Access denied. You don't have permission to retry payment for this order.");
                return result;
            }

            // 3. Validate order status is "failed"
            if (!"failed".equalsIgnoreCase(order.getStatus())) {
                System.err.println(
                        "❌ Order #" + orderId + " is not in failed state (current: " + order.getStatus() + ")");
                result.put("success", false);
                result.put("message", "Only failed orders can be retried. Current status: " + order.getStatus());
                return result;
            }

            System.out.println("✅ Order #" + orderId + " validation passed - proceeding with retry");

            // 4. Update order status to "pending"
            order.setStatus("pending");
            order.setUpdatedAt(LocalDateTime.now());
            order.setNotificationRead(false); // Reset notification so it appears as new
            order = orderRepository.save(order);

            System.out.println("✅ Order #" + orderId + " status updated: failed → pending");

            // 5. Find existing payment record
            Optional<Payment> paymentOpt = paymentRepository.findByOrderId(orderId);
            Payment payment;

            if (paymentOpt.isPresent()) {
                // Update existing payment
                payment = paymentOpt.get();
                payment.setStatus("pending");

                System.out.println("✅ Found existing Payment #" + payment.getId() + " - updating status to pending");
            } else {
                // Create new payment record (shouldn't happen, but just in case)
                payment = new Payment();
                payment.setOrderId(orderId);
                payment.setUserId(userId);
                payment.setAmount(order.getAmount());
                payment.setCurrency("USD");
                payment.setPaymentMethod("KHQR");
                payment.setStatus("pending");
                payment.setCreatedAt(LocalDateTime.now());

                System.out.println("⚠️ No existing payment found - creating new Payment record");
            }

            // 6. Generate new KHQR code
            String billNumber = "ORDER-" + orderId + "-RETRY";
            BakongKHQRService.KHQRGenerationResult khqrResult = bakongKHQRService.generateKHQR(billNumber,
                    order.getAmount());

            if (!khqrResult.isSuccess()) {
                System.err.println("❌ KHQR generation failed: " + khqrResult.getError());
                result.put("success", false);
                result.put("message", "Order status updated but KHQR generation failed: " + khqrResult.getError());
                return result;
            }

            System.out.println("✅ New KHQR generated - MD5: " + khqrResult.getMd5());

            // 7. Update payment with new KHQR details
            payment.setKhqrMd5(khqrResult.getMd5());
            payment.setKhqrQr(khqrResult.getQrCode());
            payment.setExpiresAt(LocalDateTime.now().plusMinutes(5)); // 5 minutes timeout
            payment = paymentRepository.save(payment);

            System.out.println("✅ Payment #" + payment.getId() + " updated with new KHQR");
            System.out.println("   Expires at: " + payment.getExpiresAt());

            // 8. Start background monitoring
            // startPaymentMonitoring(payment.getId(), orderId);

            System.out.println("⏳ Payment monitoring started for Order #" + orderId);

            // 9. Prepare response data
            Map<String, Object> orderData = new HashMap<>();
            orderData.put("id", order.getId());
            orderData.put("userId", order.getUserId());
            orderData.put("status", order.getStatus());
            orderData.put("amount", order.getAmount());
            orderData.put("createdAt", order.getCreatedAt() != null ? order.getCreatedAt().toString() : null);
            orderData.put("updatedAt", order.getUpdatedAt() != null ? order.getUpdatedAt().toString() : null);
            orderData.put("addressId", order.getAddressId());
            orderData.put("notificationRead", order.getNotificationRead());

            Map<String, Object> paymentData = new HashMap<>();
            paymentData.put("id", payment.getId());
            paymentData.put("orderId", payment.getOrderId());
            paymentData.put("amount", payment.getAmount());
            paymentData.put("currency", payment.getCurrency());
            paymentData.put("paymentMethod", payment.getPaymentMethod());
            paymentData.put("status", payment.getStatus());
            paymentData.put("qrCode", payment.getKhqrQr());
            paymentData.put("expiresAt", payment.getExpiresAt() != null ? payment.getExpiresAt().toString() : null);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("order", orderData);
            responseData.put("payment", paymentData);
            responseData.put("qrCode", khqrResult.getQrCode());

            result.put("success", true);
            result.put("message", "Order status updated to pending. Please scan the QR code to complete payment.");
            result.put("data", responseData);

            System.out.println("✅ Retry payment successful for Order #" + orderId);

            return result;

        } catch (Exception e) {
            System.err.println("❌ Error during retry payment for Order #" + orderId + ": " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Error retrying payment: " + e.getMessage());
            return result;
        }
    }
}