package server.service;

import common.dto.NotificationDTO;
import common.enums.NotificationType;
import server.model.Notification;
import server.model.User;
import server.repository.NotificationRepository;
import server.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final UserRepository userRepository;
  private final NotificationRealtimePublisher realtimePublisher;

  public NotificationService(
      NotificationRepository notificationRepository,
      UserRepository userRepository,
      NotificationRealtimePublisher realtimePublisher) {
    this.notificationRepository = notificationRepository;
    this.userRepository = userRepository;
    this.realtimePublisher = realtimePublisher;
  }

  @Transactional
  public void createNotification(Long userId, String message, NotificationType type) {
    User user = userRepository.findById(userId).orElse(null);
    if (user != null) {
      Notification notification = new Notification(user, message, type);
      notificationRepository.save(notification);
      publishUnreadCount(userId);
    }
  }

  @Transactional(readOnly = true)
  public List<NotificationDTO> getNotificationsForUser(Long userId) {
    return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
        .map(
            n ->
                new NotificationDTO(
                    n.getId(), n.getMessage(), n.getCreatedAt(), n.isRead(), n.getType()))
        .collect(Collectors.toList());
  }

  @Transactional
  public void markAsRead(Long notificationId) {
    notificationRepository
        .findById(notificationId)
        .ifPresent(
            n -> {
              n.setRead(true);
              notificationRepository.save(n);
              if (n.getUser() != null) {
                publishUnreadCount(n.getUser().getId());
              }
            });
  }

  @Transactional(readOnly = true)
  public long countUnreadNotifications(Long userId) {
    return notificationRepository.countByUserIdAndIsReadFalse(userId);
  }

  @Transactional
  public void markAllAsRead(Long userId) {
    List<Notification> unreadNotifications =
        notificationRepository.findByUserIdAndIsReadFalse(userId);
    if (!unreadNotifications.isEmpty()) {
      unreadNotifications.forEach(notification -> notification.setRead(true));
      notificationRepository.saveAll(unreadNotifications);
    }
    publishUnreadCount(userId);
  }

  private void publishUnreadCount(Long userId) {
    realtimePublisher.publishUnreadCountAfterCommit(userId, countUnreadNotifications(userId));
  }
}
