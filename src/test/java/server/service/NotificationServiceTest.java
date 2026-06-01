package server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import common.enums.NotificationType;
import server.model.Bidder;
import server.model.Notification;
import server.repository.NotificationRepository;
import server.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class NotificationServiceTest {

  @Mock private NotificationRepository notificationRepository;
  @Mock private UserRepository userRepository;
  @Mock private NotificationRealtimePublisher realtimePublisher;
  private NotificationService notificationService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    notificationService =
        new NotificationService(notificationRepository, userRepository, realtimePublisher);
  }

  @Test
  void createNotification_ExistingUser_SavesNotification() {
    Bidder user = new Bidder();
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));

    notificationService.createNotification(1L, "message", NotificationType.BID_WON);

    verify(notificationRepository).save(any(Notification.class));
  }

  @Test
  void createNotification_MissingUser_DoesNothing() {
    when(userRepository.findById(1L)).thenReturn(Optional.empty());

    notificationService.createNotification(1L, "message", NotificationType.BID_WON);

    verify(notificationRepository, never()).save(any());
  }

  @Test
  void getNotificationsForUser_MapsDtos() {
    Notification notification = new Notification(new Bidder(), "message", NotificationType.BID_WON);
    notification.setId(2L);
    notification.setCreatedAt(LocalDateTime.now());
    notification.setRead(true);
    when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L))
        .thenReturn(List.of(notification));

    var result = notificationService.getNotificationsForUser(1L);

    assertEquals(1, result.size());
    assertEquals("message", result.getFirst().getMessage());
    assertTrue(result.getFirst().isRead());
  }

  @Test
  void markAsRead_ExistingNotification_SavesUpdatedEntity() {
    Notification notification = new Notification();
    when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

    notificationService.markAsRead(1L);

    ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
    verify(notificationRepository).save(captor.capture());
    assertTrue(captor.getValue().isRead());
  }

  @Test
  void markAsRead_MissingNotification_DoesNothing() {
    when(notificationRepository.findById(1L)).thenReturn(Optional.empty());

    notificationService.markAsRead(1L);

    verify(notificationRepository, never()).save(any());
  }

  @Test
  void countUnreadNotifications_ReturnsRepositoryCount() {
    when(notificationRepository.countByUserIdAndIsReadFalse(1L)).thenReturn(3L);

    assertEquals(3L, notificationService.countUnreadNotifications(1L));
  }

  @Test
  void markAllAsRead_UpdatesUnreadNotifications() {
    Notification first = new Notification();
    Notification second = new Notification();
    when(notificationRepository.findByUserIdAndIsReadFalse(1L)).thenReturn(List.of(first, second));

    notificationService.markAllAsRead(1L);

    assertTrue(first.isRead());
    assertTrue(second.isRead());
    verify(notificationRepository).saveAll(List.of(first, second));
    verify(realtimePublisher).publishUnreadCountAfterCommit(1L, 0L);
  }
}
