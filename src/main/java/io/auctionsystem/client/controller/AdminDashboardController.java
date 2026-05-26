package io.auctionsystem.client.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.auctionsystem.client.pattern.AuctionManager;
import io.auctionsystem.client.pattern.SceneManager;
import io.auctionsystem.common.response.AuthResponse;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Year;
import java.util.List;
import java.util.Map;

public class AdminDashboardController {

    @FXML private TableView<AuthResponse> userTable;
    @FXML private TableColumn<AuthResponse, Long> colId;
    @FXML private TableColumn<AuthResponse, String> colUsername;
    @FXML private TableColumn<AuthResponse, String> colFullName;
    @FXML private TableColumn<AuthResponse, String> colRole;
    @FXML private TableColumn<AuthResponse, Void> colAction;
    @FXML private BarChart<String, Number> auctionChart;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @FXML
    public void initialize() {
        setupUserTable();
        loadAuctionStats();
        refreshTable();
    }

    private void setupUserTable() {
        colId.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getUserId()));
        colUsername.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getUsername()));
        colFullName.setCellValueFactory(data -> {
            AuthResponse user = data.getValue();
            String firstname = user.getFirstname() == null ? "" : user.getFirstname();
            String lastname = user.getLastname() == null ? "" : user.getLastname();
            return new ReadOnlyStringWrapper((firstname + " " + lastname).trim());
        });
        colRole.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                data.getValue().getRole() == null ? "" : data.getValue().getRole().name()));

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
                    return;
                }

                AuthResponse user = getTableView().getItems().get(getIndex());
                btnToggle.setDisable(user == null || user.getUserId() == null);
                btnToggle.setOnAction(event -> handleToggleBan(user.getUserId()));
                setGraphic(btnToggle);
            }
        });
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
                    List<AuthResponse> users = objectMapper.readValue(
                            response.body(),
                            new TypeReference<List<AuthResponse>>() {});
                    Platform.runLater(() -> userTable.setItems(FXCollections.observableArrayList(users)));
                }
            } catch (Exception e) {
                System.err.println("Loi load bang Admin: " + e.getMessage());
            }
        }).start();
    }

    private void loadAuctionStats() {
        int year = Year.now().getValue();
        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/admin/auctions/monthly-stats?year=" + year))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    return;
                }

                Map<Integer, Long> stats = objectMapper.readValue(
                        response.body(),
                        new TypeReference<Map<Integer, Long>>() {});

                Platform.runLater(() -> {
                    auctionChart.getData().clear();
                    XYChart.Series<String, Number> series = new XYChart.Series<>();
                    series.setName("Số phiên đấu giá " + year);
                    for (int month = 1; month <= 12; month++) {
                        series.getData().add(new XYChart.Data<>(
                                "Tháng " + month,
                                stats.getOrDefault(month, 0L)));
                    }
                    auctionChart.getData().add(series);
                });
            } catch (Exception e) {
                System.err.println("Loi load thong ke Admin: " + e.getMessage());
            }
        }).start();
    }

    private void handleToggleBan(Long userId) {
        if (userId == null) {
            return;
        }

        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/admin/users/toggle-ban/" + userId))
                        .PUT(HttpRequest.BodyPublishers.noBody())
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    refreshTable();
                }
            } catch (Exception e) {
                System.err.println("Loi doi trang thai user Admin: " + e.getMessage());
            }
        }).start();
    }

    @FXML
    public void onLogout() {
        AuctionManager.getInstance().isLoggedOut();
        SceneManager.getInstance().switchScene("/client/fxml/login.fxml");
    }
}
