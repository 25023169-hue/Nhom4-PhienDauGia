package io.auctionsystem.server.dao;

import io.auctionsystem.server.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationDAO extends JpaRepository<Notification, Long> {
    // Lấy danh sách thông báo của 1 user, cái mới nhất hiện lên đầu
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
}