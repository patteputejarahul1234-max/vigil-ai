package com.vigilai.service;

import com.vigilai.dto.NotificationResponse;
import com.vigilai.entity.AppNotification;
import com.vigilai.entity.NotificationType;
import com.vigilai.exception.ApiException;
import com.vigilai.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void notify(Long recipientId, NotificationType type, String message, String relatedEntityType, Long relatedEntityId) {
        AppNotification n = AppNotification.builder()
                .recipientId(recipientId)
                .type(type)
                .message(message)
                .relatedEntityType(relatedEntityType)
                .relatedEntityId(relatedEntityId)
                .build();
        notificationRepository.save(n);
    }

    public List<NotificationResponse> getForUser(Long userId, boolean unreadOnly) {
        List<AppNotification> list = unreadOnly
                ? notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(userId)
                : notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId);

        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public long unreadCount(Long userId) {
        return notificationRepository.countByRecipientIdAndReadFalse(userId);
    }

    public void markAsRead(Long notificationId, Long userId) {
        AppNotification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> ApiException.notFound("Notification not found"));

        if (!n.getRecipientId().equals(userId)) {
            throw ApiException.badRequest("This notification does not belong to you");
        }

        n.setRead(true);
        notificationRepository.save(n);
    }

    public void markAllAsRead(Long userId) {
        notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(userId)
                .forEach(n -> {
                    n.setRead(true);
                    notificationRepository.save(n);
                });
    }

    private NotificationResponse toResponse(AppNotification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .message(n.getMessage())
                .relatedEntityId(n.getRelatedEntityId())
                .relatedEntityType(n.getRelatedEntityType())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
