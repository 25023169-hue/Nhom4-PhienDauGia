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

  @GetMapping("/{userId}/unread-count")
  public long getUnreadCount(@PathVariable Long userId) {
    return notificationService.countUnreadNotifications(userId);
  }

  @PutMapping("/{id}/read")
  public void readNotification(@PathVariable Long id) {
    notificationService.markAsRead(id);
  }

  @PutMapping("/user/{userId}/read-all")
  public void readAllNotifications(@PathVariable Long userId) {
    notificationService.markAllAsRead(userId);
  }
}
