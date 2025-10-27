package com.minimart.api.service;

import com.minimart.api.model.Order;
import com.minimart.api.model.OrderDetail;
import com.minimart.api.model.Payment;
import com.minimart.api.model.Product;
import com.minimart.api.repository.OrderDetailRepository;
import com.minimart.api.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TelegramNotificationService {
    
    // ✅ ORDER BOT (for order notifications)
    private static final String ORDER_BOT_TOKEN = "8160018635:AAHkidA6eHD7uCIWY8rnAUlYtoyn6MruaPM";
    private static final String ORDER_CHAT_ID = "7812715738";
    
    // ✅ PAYMENT BOT (for payment control)
    private static final String PAYMENT_BOT_TOKEN = "8482430847:AAGGo9goyAtW3O4hLS4jXBQuCT3IV08omKg";
    private static final String PAYMENT_CHAT_ID = "7812715738";
    
    @Autowired
    private OrderDetailRepository orderDetailRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * 🆕 Send order success notification (ONLY when status = paid)
     * Sent to: Telegram (owner)
     * 
     * Usage: telegramService.sendOrderSuccessNotification(order);
     */
    public boolean sendOrderSuccessNotification(Order order) {
        try {
            String message = buildOrderSuccessMessage(order);
            return sendToOrderBot(message);
        } catch (Exception e) {
            System.err.println("❌ Failed to send order notification: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 🆕 Send payment notification (for payment bot control)
     * Sent to: Telegram (owner only) - Payment Bot
     * 
     * Usage: telegramService.sendPaymentNotification(order, payment);
     */
    public boolean sendPaymentNotification(Order order, Payment payment) {
        try {
            String message = buildPaymentMessage(order, payment);
            return sendToPaymentBot(message);
        } catch (Exception e) {
            System.err.println("❌ Failed to send payment notification: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Build order success message
     */
    private String buildOrderSuccessMessage(Order order) {
        StringBuilder message = new StringBuilder();
        
        // Header
        message.append("✅ *ORDER PAID #").append(order.getId()).append("*\n");
        
        // Date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        message.append("📅 ").append(order.getUpdatedAt().format(formatter)).append("\n\n");
        
        // Order Items
        message.append("*ORDER ITEMS:*\n");
        List<OrderDetail> orderDetails = orderDetailRepository.findByOrder(order);
        
        BigDecimal subtotal = BigDecimal.ZERO;
        int itemNumber = 1;
        
        for (OrderDetail detail : orderDetails) {
            Product product = productRepository.findById(detail.getProductId()).orElse(null);
            String productName = product != null ? product.getName() : "Unknown Product";
            
            BigDecimal itemTotal = detail.getPrice().multiply(new BigDecimal(detail.getQty()));
            subtotal = subtotal.add(itemTotal);
            
            message.append("• ")
                   .append(itemNumber++).append(". ")
                   .append(escapeMarkdown(productName)).append(" - ")
                   .append(detail.getQty()).append(" x $")
                   .append(String.format("%.2f", detail.getPrice()))
                   .append(" = $")
                   .append(String.format("%.2f", itemTotal))
                   .append("\n");
        }
        
        message.append("\n💵 *Total: $").append(String.format("%.2f", order.getAmount())).append("*\n\n");
        message.append("✨ Order is being processed!");
        
        return message.toString();
    }
    
    /**
     * Build payment notification message (for payment bot)
     */
    private String buildPaymentMessage(Order order, Payment payment) {
        StringBuilder message = new StringBuilder();
        
        message.append("💰 *PAYMENT RECEIVED*\n\n");
        message.append("🛒 Order ID: #").append(order.getId()).append("\n");
        message.append("💵 Amount: $").append(String.format("%.2f", payment.getAmount())).append("\n");
        message.append("💳 Method: ").append(payment.getPaymentMethod()).append("\n");
        
        if (payment.getTransactionId() != null && !payment.getTransactionId().isEmpty()) {
            message.append("🔖 Transaction: ").append(escapeMarkdown(payment.getTransactionId())).append("\n");
        }
        
        message.append("📊 Status: ").append(payment.getStatus().toUpperCase()).append("\n");
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (payment.getPayDate() != null) {
            message.append("📅 Paid: ").append(payment.getPayDate().format(formatter)).append("\n");
        }
        
        return message.toString();
    }
    
    /**
     * Send message to ORDER BOT
     */
    private boolean sendToOrderBot(String message) {
        return sendTelegramMessage(ORDER_BOT_TOKEN, ORDER_CHAT_ID, message);
    }
    
    /**
     * Send message to PAYMENT BOT
     */
    private boolean sendToPaymentBot(String message) {
        return sendTelegramMessage(PAYMENT_BOT_TOKEN, PAYMENT_CHAT_ID, message);
    }
    
    /**
     * Generic method to send message to any Telegram Bot
     */
    private boolean sendTelegramMessage(String botToken, String chatId, String message) {
        try {
            String apiUrl = "https://api.telegram.org/bot" + botToken + "/sendMessage";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("chat_id", chatId);
            requestBody.put("text", message);
            requestBody.put("parse_mode", "Markdown");
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                apiUrl,
                request,
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("✅ Telegram notification sent successfully!");
                return true;
            } else {
                System.err.println("❌ Telegram API error: " + response.getStatusCode());
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Failed to send Telegram message: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Escape special characters for Telegram Markdown
     */
    private String escapeMarkdown(String text) {
        if (text == null) return "";
        return text.replace("_", "\\_")
                   .replace("[", "\\[")
                   .replace("]", "\\]")
                   .replace("(", "\\(")
                   .replace(")", "\\)")
                   .replace("~", "\\~")
                   .replace("`", "\\`")
                   .replace(">", "\\>")
                   .replace("#", "\\#")
                   .replace("+", "\\+")
                   .replace("-", "\\-")
                   .replace("=", "\\=")
                   .replace("|", "\\|")
                   .replace("{", "\\{")
                   .replace("}", "\\}")
                   .replace(".", "\\.")
                   .replace("!", "\\!");
    }
    
    /**
     * Send custom message to ORDER BOT
     */
    public boolean sendCustomMessage(String message) {
        return sendToOrderBot(message);
    }
}