package server.controller;

import common.dto.NotificationDTO;
import server.service.NotificationService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

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
