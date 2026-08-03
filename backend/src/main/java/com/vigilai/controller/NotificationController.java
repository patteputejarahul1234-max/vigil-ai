package com.vigilai.controller;

import com.vigilai.dto.NotificationResponse;
import com.vigilai.security.CurrentUser;
import com.vigilai.service.NotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "In-app notifications: task assignments, status changes, invites")
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUser currentUser;

    public NotificationController(NotificationService notificationService, CurrentUser currentUser) {
        this.notificationService = notificationService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<NotificationResponse> list(Authentication auth,
                                            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        return notificationService.getForUser(currentUser.idOf(auth), unreadOnly);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(Authentication auth) {
        return Map.of("count", notificationService.unreadCount(currentUser.idOf(auth)));
    }

    @PutMapping("/{notificationId}/read")
    public void markAsRead(Authentication auth, @PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId, currentUser.idOf(auth));
    }

    @PutMapping("/read-all")
    public void markAllAsRead(Authentication auth) {
        notificationService.markAllAsRead(currentUser.idOf(auth));
    }
}
