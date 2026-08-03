package com.vigilai.dto;

import com.vigilai.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private NotificationType type;
    private String message;
    private Long relatedEntityId;
    private String relatedEntityType;
    private boolean read;
    private Instant createdAt;
}
