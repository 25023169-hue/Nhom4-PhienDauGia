package io.auctionsystem.client.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.auctionsystem.client.pattern.AuctionManager;
import io.auctionsystem.common.dto.NotificationDTO;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class NotificationController {

    @FXML private ListView<String> listNotifications;
    private final ObservableList<String> notiList = FXCollections.observableArrayList();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @FXML
    public void initialize() {
        listNotifications.setItems(notiList);
        loadNotifications();
    }

    private void loadNotifications() {
        Long userId = AuctionManager.getInstance().getId();
        if (userId == null) return;

        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/notifications/" + userId))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        try {
                            List<NotificationDTO> notis = objectMapper.readValue(response.body(), new TypeReference<List<NotificationDTO>>() {});
                            notiList.clear();
                            for (NotificationDTO noti : notis) {
                                String status = noti.isRead() ? "" : "[MỚI] ";
                                notiList.add(status + noti.getMessage());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}