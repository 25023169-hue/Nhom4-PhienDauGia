package io.auctionsystem.client.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.auctionsystem.client.pattern.SceneManager;
import io.auctionsystem.common.response.AuthResponse;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class AdminDashboardController {
    // 1. Khai báo các thành phần FXML (Phải khớp fx:id trong file fxml)
    @FXML private TableView<AuthResponse> userTable;
    @FXML private TableColumn<AuthResponse, Long> colId;
    @FXML private TableColumn<AuthResponse, String> colUsername;
    @FXML private TableColumn<AuthResponse, String> colFullName;
    @FXML private TableColumn<AuthResponse, String> colRole;
    @FXML private TableColumn<AuthResponse, Void> colAction; // Cột chứa nút bấm
    @FXML private BarChart<String, Number> auctionChart;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @FXML
    public void initialize() {
        // --- PHẦN 1: SETUP BIỂU ĐỒ ---
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Số phiên đấu giá 2026");
        series.getData().add(new XYChart.Data<>("Tháng 1", 25));
        series.getData().add(new XYChart.Data<>("Tháng 2", 40));
        series.getData().add(new XYChart.Data<>("Tháng 3", 65));
        auctionChart.getData().add(series);

        // --- PHẦN 2: SETUP BẢNG DỮ LIỆU ---
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colFullName.setCellValueFactory(new PropertyValueFactory<>("firstname"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));

        // Tạo nút bấm "Khóa/Mở" trong cột Thao tác
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btnToggle = new Button("Khóa/Mở");
            {
                btnToggle.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-cursor: hand;");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    btnToggle.setOnAction(event -> {
                        AuthResponse user = getTableView().getItems().get(getIndex());
                        handleToggleBan(user.getUserId());
                    });
                    setGraphic(btnToggle);
                }
            }
        });

        // --- PHẦN 3: LOAD DỮ LIỆU THẬT TỪ SERVER ---
        refreshTable();
    }

    private void refreshTable() {
        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/admin/users"))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    List<AuthResponse> users = objectMapper.readValue(response.body(),
                            new TypeReference<List<AuthResponse>>() {});

                    Platform.runLater(() -> {
                        userTable.setItems(FXCollections.observableArrayList(users));
                    });
                }
            } catch (Exception e) {
                System.err.println("Lỗi load bảng Admin: " + e.getMessage());
            }
        }).start();
    }

    private void handleToggleBan(Long userId) {
        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/admin/users/toggle-ban/" + userId))
                        .PUT(HttpRequest.BodyPublishers.noBody())
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    System.out.println("Đã thay đổi trạng thái user " + userId);
                    refreshTable(); // Cập nhật lại bảng ngay sau khi khóa/mở
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    public void onLogout() {
        SceneManager.getInstance().switchScene("/client/fxml/user/login.fxml");
    }
}