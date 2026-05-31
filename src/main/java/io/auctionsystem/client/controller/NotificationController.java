package io.auctionsystem.client.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.auctionsystem.client.pattern.AuctionManager;
import io.auctionsystem.common.dto.NotificationDTO;
import java.net.URI;
import java.net.http.HttpClient;
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
  private final HttpClient httpClient = HttpClient.newHttpClient();
  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @FXML
  public void initialize() {
    listNotifications.setItems(notiList);
    listNotifications.setCellFactory(list -> new NotificationCell());
    listNotifications.setOnMouseClicked(this::handleNotificationClicked);
    loadNotifications();
  }

  private void loadNotifications() {
    Long userId = AuctionManager.getInstance().getId();
    if (userId == null) return;

    new Thread(
            () -> {
              try {
                HttpRequest request =
                    HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/notifications/" + userId))
                        .GET()
                        .build();

                HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                Platform.runLater(
                    () -> {
                      if (response.statusCode() == 200) {
                        try {
                          List<NotificationDTO> notis =
                              objectMapper.readValue(
                                  response.body(), new TypeReference<List<NotificationDTO>>() {});
                          notiList.setAll(notis);
                        } catch (Exception e) {
                          e.printStackTrace();
                        }
                      }
                    });
              } catch (Exception e) {
                e.printStackTrace();
              }
            })
        .start();
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
                                "http://localhost:8080/api/notifications/"
                                    + notification.getNotiId()
                                    + "/read"))
                        .PUT(HttpRequest.BodyPublishers.noBody())
                        .build();
                HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
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
      String type =
          notification.getType() == null || notification.getType().isBlank()
              ? "Thông báo"
              : notification.getType();
      Label meta = new Label(type + (time.isBlank() ? "" : " • " + time));
      meta.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");

      VBox box = new VBox(4, title, meta);
      box.setStyle("-fx-padding: 10 6 10 6;");
      setText(null);
      setGraphic(box);
    }
  }
}
