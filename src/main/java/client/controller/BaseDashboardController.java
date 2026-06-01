package client.controller;

import client.pattern.AuctionManager;
import client.pattern.ClientHttp;
import client.pattern.SceneManager;
import client.pattern.WebSocketClientManager;
import common.Constants;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;

public abstract class BaseDashboardController {

  // Vùng trống ở giữa để load các View con (Dùng chung cho cả 2 giao diện)
  @FXML protected StackPane contentArea;
  @FXML protected Label lblWelcome;
  @FXML protected Label lblNotificationBadge;
  private StompSession.Subscription notificationSubscription;

  protected static final String ACTIVE_MENU_STYLE =
      "-fx-background-color: #1abc9c; -fx-text-fill: white; -fx-cursor: hand; -fx-alignment: CENTER_LEFT;";
  protected static final String INACTIVE_MENU_STYLE =
      "-fx-background-color: transparent; -fx-text-fill: #bdc3c7; -fx-cursor: hand; -fx-alignment: CENTER_LEFT;";

  // Hàm load các FXML con vào chính giữa màn hình
  protected void loadSubView(String fxmlPath) {
    if (contentArea == null) {
      System.err.println("Lỗi: Chưa có StackPane fx:id='contentArea' ở giao diện!");
      return;
    }
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
      Node view = loader.load();
      contentArea.getChildren().setAll(view);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // Hàm đổi màu nút đang chọn
  protected void setActiveMenu(Button activeButton, Button... inactiveButtons) {
    if (activeButton != null) activeButton.setStyle(ACTIVE_MENU_STYLE);
    for (Button btn : inactiveButtons) {
      if (btn != null) btn.setStyle(INACTIVE_MENU_STYLE);
    }
  }

  // CÁC SỰ KIỆN DÙNG CHUNG CHO CẢ 2 BÊN
  @FXML
  public void onHomeButtonClicked() {
    // Code này giả định bạn sẽ tách phần giao diện chính giữa thành home_view.fxml
    loadSubView("/client/user/home_view.fxml");
  }

  @FXML
  public void onWalletButtonClicked() {
    loadSubView("/client/user/wallet_view.fxml");
  }

  @FXML
  public void onNotificationsClicked() {
    updateNotificationBadge(0);
    loadSubView("/client/notifications_view.fxml");
  }

  protected void initializeNotificationUpdates() {
    updateNotificationBadge(0);
    Long userId = AuctionManager.getInstance().getId();
    if (userId == null) {
      return;
    }
    loadUnreadNotificationCount(userId);
    subscribeToNotificationUpdates(userId);
  }

  protected void stopNotificationUpdates() {
    if (notificationSubscription != null) {
      notificationSubscription.unsubscribe();
      notificationSubscription = null;
    }
  }

  private void loadUnreadNotificationCount(Long userId) {
    startDaemonThread(
        () -> {
          try {
            HttpRequest request =
                HttpRequest.newBuilder()
                    .uri(
                        URI.create(
                            Constants.BASE_URL + "/notifications/" + userId + "/unread-count"))
                    .GET()
                    .build();
            HttpResponse<String> response = ClientHttp.send(request);
            if (response.statusCode() == 200) {
              long unreadCount = Long.parseLong(response.body().trim());
              Platform.runLater(() -> updateNotificationBadge(unreadCount));
            }
          } catch (Exception exception) {
            System.err.println("Không thể tải số thông báo chưa đọc: " + exception.getMessage());
          }
        });
  }

  private void subscribeToNotificationUpdates(Long userId) {
    startDaemonThread(
        () -> {
          WebSocketClientManager.getInstance().connect();
          StompSession session = WebSocketClientManager.getInstance().getSession();
          if (session == null || !session.isConnected()) {
            return;
          }
          notificationSubscription =
              session.subscribe(
                  Constants.TOPIC_NOTIFICATIONS + "/" + userId + "/unread-count",
                  new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                      return Long.class;
                    }

                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                      if (payload instanceof Number count) {
                        Platform.runLater(() -> updateNotificationBadge(count.longValue()));
                      }
                    }
                  });
        });
  }

  private void updateNotificationBadge(long unreadCount) {
    if (lblNotificationBadge == null) {
      return;
    }
    boolean hasUnreadNotifications = unreadCount > 0;
    lblNotificationBadge.setText(Long.toString(unreadCount));
    lblNotificationBadge.setVisible(hasUnreadNotifications);
    lblNotificationBadge.setManaged(hasUnreadNotifications);
  }

  private void startDaemonThread(Runnable task) {
    Thread thread = new Thread(task);
    thread.setDaemon(true);
    thread.start();
  }

  @FXML
  public void onOpenSettings() {
    SceneManager.getInstance().switchScene("/client/settings/settings.fxml");
  }

  @FXML
  public void onLogoutButtonClicked() {
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setHeaderText("Xác nhận đăng xuất");
    alert.setContentText("Bạn có chắc chắn muốn đăng xuất?");
    Window ownerWindow = lblWelcome.getScene().getWindow();
    alert.initOwner(ownerWindow);
    alert.initStyle(StageStyle.UNDECORATED);

    ButtonType btnYes = new ButtonType("Có", ButtonBar.ButtonData.YES);
    ButtonType btnNo = new ButtonType("Không", ButtonBar.ButtonData.NO);
    alert.getButtonTypes().setAll(btnYes, btnNo);

    alert
        .showAndWait()
        .ifPresent(
            response -> {
              if (response == btnYes) {
                stopNotificationUpdates();
                AuctionManager.getInstance().isLoggedOut();
                SceneManager.getInstance().switchScene("/client/user/login.fxml");
              }
            });
  }
}
