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

    // LỖI ĐÃ SỬA: Đổi tên biến từ "notificationRepogistory" (đánh máy sai)
    // thành "notificationRepository" (đúng chính tả)
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
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
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(n -> new NotificationDTO(n.getId(), n.getMessage(), n.getCreatedAt(), n.isRead(), n.getType()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        // LỖI ĐÃ SỬA: Thêm gọi save() sau khi set isRead = true.
        // Trước đây chỉ gọi n.setRead(true) mà không save() → thay đổi
        // KHÔNG được ghi vào database dù có @Transactional, vì entity có thể
        // ở trạng thái detached (không được managed bởi EntityManager hiện tại).
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n); // ← THÊM DÒNG NÀY
        });
    }
}