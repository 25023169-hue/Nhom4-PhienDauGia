package io.auctionsystem.server.repository;

import io.auctionsystem.server.model.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
  // Lấy danh sách thông báo của 1 user, cái mới nhất hiện lên đầu
  List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
}
