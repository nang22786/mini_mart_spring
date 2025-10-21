package com.minimart.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private Integer notificationId;
    private String title;
    private String message;
    private String type;
    private Boolean isRead;
    private LocalDateTime createdAt;
}