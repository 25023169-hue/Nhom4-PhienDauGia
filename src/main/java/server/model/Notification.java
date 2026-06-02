package server.model;

import common.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Notification extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user; // Người nhận thông báo

  private String message;
  @Enumerated(EnumType.STRING)
  private NotificationType type;
  private boolean isRead = false;

  public Notification(User user, String message, NotificationType type) {
    this.user = user;
    this.message = message;
    this.type = type;
  }
}
