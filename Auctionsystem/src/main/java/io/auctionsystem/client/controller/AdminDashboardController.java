package io.auctionsystem.client.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.auctionsystem.client.pattern.SceneManager;
import io.auctionsystem.common.dto.AuthResponse;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;              // ← THÊM

public class AdminDashboardController {

    @FXML
    private TableView<AuthResponse> userTable;

    @FXML
    private TableColumn<AuthResponse, Long> colId;

    @FXML
    private TableColumn<AuthResponse, String> colUsername;

    @FXML
    private TableColumn<AuthResponse, String> colFullName;

    @FXML
    private TableColumn<AuthResponse, String> colRole;

    @FXML
    private TableColumn<AuthResponse, Void> colAction;

    @FXML
    private BarChart<String, Number> auctionChart;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @FXML
    public void initialize() {
        setupChart();
        setupTable();
        refreshTable();
    }

    // =========================
    // BIỂU ĐỒ — GỌI API THẬT
    // =========================

    private void setupChart() {

        new Thread(() -> {

            try {

                HttpRequest request =
                        HttpRequest.newBuilder()
                                .uri(URI.create(
                                        "http://localhost:8080/api/admin/stats/monthly?year=2026"
                                ))
                                .GET()
                                .build();

                HttpResponse<String> response =
                        httpClient.send(request,
                                HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {

                    Map<Integer, Long> stats =
                            objectMapper.readValue(
                                    response.body(),
                                    new TypeReference<Map<Integer, Long>>() {}
                            );

                    XYChart.Series<String, Number> series =
                            new XYChart.Series<>();

                    series.setName("Số phiên đấu giá 2026");

                    for (int m = 1; m <= 12; m++) {
                        long count = stats.getOrDefault(m, 0L);
                        series.getData().add(
                                new XYChart.Data<>("Tháng " + m, count)
                        );
                    }

                    Platform.runLater(() -> {
                        auctionChart.getData().clear();
                        auctionChart.getData().add(series);
                    });
                }

            } catch (Exception e) {
                System.out.println("Lỗi load chart: " + e.getMessage());
            }

        }).start();
    }

    // =========================
    // TABLE
    // =========================

    private void setupTable() {

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colFullName.setCellValueFactory(new PropertyValueFactory<>("firstname"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));

        colAction.setCellFactory(param -> new TableCell<>() {

            private final Button btnToggle = new Button();

            @Override
            protected void updateItem(Void item, boolean empty) {

                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                } else {

                    AuthResponse user =
                            getTableView().getItems().get(getIndex());

                    if (user.isBanned()) {
                        btnToggle.setText("Mở khóa");
                        btnToggle.setStyle(
                                "-fx-background-color: #27ae60;" +
                                        "-fx-text-fill: white;" +
                                        "-fx-cursor: hand;"
                        );
                    } else {
                        btnToggle.setText("Khóa");
                        btnToggle.setStyle(
                                "-fx-background-color: #e74c3c;" +
                                        "-fx-text-fill: white;" +
                                        "-fx-cursor: hand;"
                        );
                    }

                    btnToggle.setOnAction(event -> handleToggleBan(user.getId()));
                    setGraphic(btnToggle);
                }
            }
        });
    }

    // =========================
    // LOAD USER
    // =========================

    private void refreshTable() {

        new Thread(() -> {

            try {

                HttpRequest request =
                        HttpRequest.newBuilder()
                                .uri(URI.create("http://localhost:8080/api/admin/users"))
                                .GET()
                                .build();

                HttpResponse<String> response =
                        httpClient.send(request,
                                HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {

                    List<AuthResponse> users =
                            objectMapper.readValue(
                                    response.body(),
                                    new TypeReference<List<AuthResponse>>() {}
                            );

                    Platform.runLater(() ->
                            userTable.setItems(
                                    FXCollections.observableArrayList(users)
                            )
                    );
                }

            } catch (Exception e) {
                System.out.println("Lỗi load bảng admin: " + e.getMessage());
            }

        }).start();
    }

    // =========================
    // KHÓA / MỞ KHÓA USER
    // =========================

    private void handleToggleBan(Long userId) {

        new Thread(() -> {

            try {

                HttpRequest request =
                        HttpRequest.newBuilder()
                                .uri(URI.create(
                                        "http://localhost:8080/api/admin/users/toggle-ban/" + userId
                                ))
                                .PUT(HttpRequest.BodyPublishers.noBody())
                                .build();

                HttpResponse<String> response =
                        httpClient.send(request,
                                HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    System.out.println("Đã đổi trạng thái user " + userId);
                    refreshTable();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }).start();
    }

    // =========================
    // ĐĂNG XUẤT
    // =========================

    @FXML
    public void onLogout() {
        SceneManager.getInstance().switchScene("/client/fxml/login.fxml");
    }
}