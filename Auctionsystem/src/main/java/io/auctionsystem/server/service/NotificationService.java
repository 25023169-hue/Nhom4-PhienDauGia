package io.auctionsystem.server.service;

import io.auctionsystem.common.dto.NotificationDTO;
import io.auctionsystem.server.repogistory.NotificationDAO;
import io.auctionsystem.server.repogistory.UserDAO;
import io.auctionsystem.server.model.Notification;
import io.auctionsystem.server.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationDAO notificationDAO;
    private final UserDAO userDAO;

    public NotificationService(NotificationDAO notificationDAO, UserDAO userDAO) {
        this.notificationDAO = notificationDAO;
        this.userDAO = userDAO;
    }

    @Transactional
    public void createNotification(Long userId, String message, String type) {
        User user = userDAO.findById(userId).orElse(null);
        if (user != null) {
            Notification notification = new Notification(user, message, type);
            notificationDAO.save(notification);
            // TODO: Gọi WebSocketNotificationService.sendToUser(userId, message) tại đây
        }
    }

    public List<NotificationDTO> getNotificationsForUser(Long userId) {
        return notificationDAO.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(n -> new NotificationDTO(n.getId(), n.getMessage(), n.getCreatedAt(), n.isRead(), n.getType()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        notificationDAO.findById(notificationId).ifPresent(n -> n.setRead(true));
    }
}