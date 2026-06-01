package server.repository;

import server.model.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
  List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

  List<Notification> findByUserIdAndIsReadFalse(Long userId);

  long countByUserIdAndIsReadFalse(Long userId);
}
