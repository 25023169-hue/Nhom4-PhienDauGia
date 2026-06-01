package common.dto;

import common.enums.NotificationType;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO implements Serializable {
  private static final long serialVersionUID = 1L;
  private Long NotiId;
  private String message;
  private LocalDateTime createdAt;
  private boolean isRead;
  private NotificationType type;
}
