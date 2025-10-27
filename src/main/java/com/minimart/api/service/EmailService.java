package com.minimart.api.service;

import com.minimart.api.model.Order;
import com.minimart.api.model.OrderDetail;
import com.minimart.api.model.Product;
import com.minimart.api.model.User;
import com.minimart.api.repository.OrderDetailRepository;
import com.minimart.api.repository.ProductRepository;
import com.minimart.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private UserRepository userRepository; 
    
    @Autowired
    private OrderDetailRepository orderDetailRepository;
    
    @Autowired
    private ProductRepository productRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // ==========================
    // Existing OTP Email Methods
    // ==========================
    public boolean sendOtpEmail(String toEmail, String otpCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Mini Mart - Your OTP Verification Code");
            message.setText(buildOtpEmailBody(otpCode));

            mailSender.send(message);
            System.out.println("OTP email sent successfully to: " + toEmail);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to send email to " + toEmail + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean sendPasswordResetOtpEmail(String toEmail, String otpCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Mini Mart - Password Reset OTP");
            message.setText(buildPasswordResetEmailBody(otpCode));

            mailSender.send(message);
            System.out.println("Password reset OTP email sent successfully to: " + toEmail);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to send password reset email to " + toEmail + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private String buildOtpEmailBody(String otpCode) {
        return "Dear User,\n\n" +
                "Thank you for registering with Mini Mart!\n\n" +
                "Your OTP verification code is: " + otpCode + "\n\n" +
                "This code will expire in 5 minutes.\n\n" +
                "If you did not request this code, please ignore this email.\n\n" +
                "Best regards,\n" +
                "Mini Mart Team";
    }

    private String buildPasswordResetEmailBody(String otpCode) {
        return "Dear User,\n\n" +
                "You have requested to reset your password for your Mini Mart account.\n\n" +
                "Your password reset OTP code is: " + otpCode + "\n\n" +
                "This code will expire in 5 minutes.\n\n" +
                "If you did not request a password reset, please ignore this email and your password will remain unchanged.\n\n" +
                "Best regards,\n" +
                "Mini Mart Team";
    }

    // ==========================
    // ✅ NEW: Order Success Email
    // ==========================
public boolean sendOrderSuccessEmail(Order order) {
    try {
        Optional<User> userOpt = userRepository.findById(order.getUserId());
        if (userOpt.isEmpty()) {
            System.err.println("⚠️ User not found for Order ID: " + order.getId());
            return false;
        }

        User user = userOpt.get();
        String toEmail = user.getEmail();
        String userName = user.getUserName();

        String subject = "🛒 Mini Mart - Order Confirmation #" + order.getId();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String dateTime = order.getCreatedAt().format(formatter);

        // ✅ Fetch order details from repository
        List<OrderDetail> details = orderDetailRepository.findByOrder(order);

        StringBuilder itemList = new StringBuilder();
        BigDecimal total = BigDecimal.ZERO;
        int index = 1;

        for (OrderDetail detail : details) {
            // ✅ Fetch product entity
            Product product = productRepository.findById(detail.getProductId()).orElse(null);
            String productName = product != null ? product.getName() : "Unknown Product";

            BigDecimal itemTotal = detail.getPrice().multiply(BigDecimal.valueOf(detail.getQty()));
            total = total.add(itemTotal);

            itemList.append(String.format("• %d. %s - %d x %.2f - Total: %.2f\n",
                    index++, productName, detail.getQty(), detail.getPrice(), itemTotal));
        }

        // Build final email message
        String messageBody = String.format(
                "Dear %s,\n\n" +
                "🛒 NEW ORDER #%d\n" +
                "📅 %s\n\n" +
                "ORDER ITEMS:\n%s" +
                "💵 Total: %.2f\n\n" +
                "🙏 Thank you for your order!\n" +
                "- Mini Mart Team",
                userName, order.getId(), dateTime, itemList.toString(), total
        );

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(messageBody);

        mailSender.send(message);
        System.out.println("✅ Order success email sent to: " + toEmail);
        return true;

    } catch (Exception e) {
        System.err.println("❌ Failed to send order success email: " + e.getMessage());
        e.printStackTrace();
        return false;
    }
}


}
