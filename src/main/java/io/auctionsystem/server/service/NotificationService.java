package io.auctionsystem.server.service;

import io.auctionsystem.common.dto.NotificationDTO;
import io.auctionsystem.server.repository.NotificationRepository;
import io.auctionsystem.server.repository.UserRepository;
import io.auctionsystem.server.model.Notification;
import io.auctionsystem.server.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepogistory;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationDAO, UserRepository userRepository) {
        this.notificationRepogistory = notificationDAO;
        this.userRepository = userRepository;
    }

    @Transactional
    public void createNotification(Long userId, String message, String type) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            Notification notification = new Notification(user, message, type);
            notificationRepogistory.save(notification);
            // TODO: Gọi WebSocketNotificationService.sendToUser(userId, message) tại đây
        }
    }

    public List<NotificationDTO> getNotificationsForUser(Long userId) {
        return notificationRepogistory.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(n -> new NotificationDTO(n.getId(), n.getMessage(), n.getCreatedAt(), n.isRead(), n.getType()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepogistory.findById(notificationId).ifPresent(n -> n.setRead(true));
    }
}