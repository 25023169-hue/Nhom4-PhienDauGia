package server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class NotificationRealtimePublisher {

  private final SimpMessagingTemplate messagingTemplate;

  public void publishUnreadCountAfterCommit(Long userId, long unreadCount) {
    if (userId == null) {
      return;
    }
    afterCommit(
        () ->
            messagingTemplate.convertAndSend(
                "/topic/notifications/" + userId + "/unread-count", unreadCount));
  }

  private void afterCommit(Runnable action) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      action.run();
      return;
    }

    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            action.run();
          }
        });
  }
}
