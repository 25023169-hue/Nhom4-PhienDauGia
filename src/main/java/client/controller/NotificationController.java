package client.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import client.pattern.AuctionManager;
import client.pattern.ClientHttp;
import common.Constants;
import common.dto.NotificationDTO;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

public class NotificationController {

  @FXML private ListView<NotificationDTO> listNotifications;

  private static final DateTimeFormatter DISPLAY_FORMATTER =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
  private final ObservableList<NotificationDTO> notiList = FXCollections.observableArrayList();

  private final ObjectMapper objectMapper = ClientHttp.mapper();

  @FXML
  public void initialize() {
    listNotifications.setItems(notiList);
    listNotifications.setCellFactory(list -> new NotificationCell());
    listNotifications.setOnMouseClicked(this::handleNotificationClicked);
    markAllAsReadAndLoadNotifications();
  }

  private void markAllAsReadAndLoadNotifications() {
    Long userId = AuctionManager.getInstance().getId();
    if (userId == null) {
      loadNotifications();
      return;
    }

    new Thread(
            () -> {
              try {
                HttpRequest request =
                    HttpRequest.newBuilder()
                        .uri(
                            URI.create(
                                Constants.BASE_URL + "/notifications/user/" + userId + "/read-all"))
                        .PUT(HttpRequest.BodyPublishers.noBody())
                        .build();
                ClientHttp.send(request);
              } catch (Exception e) {
                e.printStackTrace();
              } finally {
                loadNotifications();
              }
            })
        .start();
  }

  private void loadNotifications() {
    Long userId = AuctionManager.getInstance().getId();
    if (userId == null) return;

    new Thread(
            () -> {
              try {
                HttpRequest request =
                    HttpRequest.newBuilder()
                        .uri(URI.create(Constants.BASE_URL + "/notifications/" + userId))
                        .GET()
                        .build();

                HttpResponse<String> response =
                    ClientHttp.send(request);

                Platform.runLater(() -> handleNotificationsResponse(response));
              } catch (Exception e) {
                e.printStackTrace();
              }
            })
        .start();
  }

  private void handleNotificationsResponse(HttpResponse<String> response) {
    if (response.statusCode() != 200) {
      return;
    }
    try {
      List<NotificationDTO> notis =
          objectMapper.readValue(response.body(), new TypeReference<List<NotificationDTO>>() {});
      notiList.setAll(notis);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void handleNotificationClicked(MouseEvent event) {
    if (event.getClickCount() != 2) {
      return;
    }
    NotificationDTO selected = listNotifications.getSelectionModel().getSelectedItem();
    if (selected == null || selected.isRead() || selected.getNotiId() == null) {
      return;
    }
    markAsRead(selected);
  }

  private void markAsRead(NotificationDTO notification) {
    new Thread(
            () -> {
              try {
                HttpRequest request =
                    HttpRequest.newBuilder()
                        .uri(
                            URI.create(
                                Constants.BASE_URL
                                    + "/notifications/"
                                    + notification.getNotiId()
                                    + "/read"))
                        .PUT(HttpRequest.BodyPublishers.noBody())
                        .build();
                HttpResponse<String> response =
                    ClientHttp.send(request);
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                  Platform.runLater(
                      () -> {
                        notification.setRead(true);
                        listNotifications.refresh();
                      });
                }
              } catch (Exception e) {
                e.printStackTrace();
              }
            })
        .start();
  }

  private static class NotificationCell extends ListCell<NotificationDTO> {
    @Override
    protected void updateItem(NotificationDTO notification, boolean empty) {
      super.updateItem(notification, empty);
      if (empty || notification == null) {
        setText(null);
        setGraphic(null);
        return;
      }

      Label title = new Label((notification.isRead() ? "" : "[Mới] ") + notification.getMessage());
      title.setWrapText(true);
      title.setStyle(
          "-fx-font-weight: "
              + (notification.isRead() ? "normal" : "bold")
              + "; -fx-text-fill: #2c3e50;");

      String time =
          notification.getCreatedAt() == null
              ? ""
              : notification.getCreatedAt().format(DISPLAY_FORMATTER);
      String type = notification.getType() == null ? "Thông báo" : notification.getType().name();
      Label meta = new Label(type + (time.isBlank() ? "" : " • " + time));
      meta.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");

      VBox box = new VBox(4, title, meta);
      box.setStyle("-fx-padding: 10 6 10 6;");
      setText(null);
      setGraphic(box);
    }
  }
}
