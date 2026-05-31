package io.auctionsystem.server.service;

import io.auctionsystem.common.dto.NotificationDTO;
import io.auctionsystem.server.model.Notification;
import io.auctionsystem.server.model.User;
import io.auctionsystem.server.repository.NotificationRepository;
import io.auctionsystem.server.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final UserRepository userRepository;

  public NotificationService(
      NotificationRepository notificationRepository, UserRepository userRepository) {
    this.notificationRepository = notificationRepository;
    this.userRepository = userRepository;
  }

  @Transactional
  public void createNotification(Long userId, String message, String type) {
    User user = userRepository.findById(userId).orElse(null);
    if (user != null) {
      Notification notification = new Notification(user, message, type);
      notificationRepository.save(notification);
    }
  }

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
            });
  }
}
