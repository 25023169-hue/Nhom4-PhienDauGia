package io.auctionsystem.server.API;

import io.auctionsystem.common.dto.NotificationDTO;
import io.auctionsystem.server.service.NotificationService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationAPI {

    private final NotificationService notificationService;

    public NotificationAPI(NotificationService notificationService) {
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