package io.auctionsystem.server.controller;

import io.auctionsystem.common.dto.NotificationDTO;
import io.auctionsystem.server.service.NotificationService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/{userId}")
    public List<NotificationDTO> getMyNotifications(@PathVariable Long userId) {
        return notificationService.getNotificationsForUser(userId);
    }

    @PutMapping("/{id}/read")
    public void readNotification(@PathVariable Long id) {
        notificationService.markAsRead(id);
    }
}