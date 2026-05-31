package io.auctionsystem.client.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.auctionsystem.client.pattern.SceneManager;
import io.auctionsystem.common.response.AuthResponse;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class AdminDashboardController {
  // 1. Khai báo các thành phần FXML (Phải khớp fx:id trong file fxml)
  @FXML private TabPane tabPane;
  @FXML private Label statUsers;
  @FXML private TableView<AuthResponse> usersTable;
  @FXML private TableColumn<AuthResponse, Long> colUserId;
  @FXML private TableColumn<AuthResponse, String> colUsername;
  @FXML private TableColumn<AuthResponse, String> colFullname;
  @FXML private TableColumn<AuthResponse, String> colRole;
  @FXML private TableColumn<AuthResponse, Void> colAction; // Cột chứa nút bấm

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final HttpClient httpClient = HttpClient.newHttpClient();

  @FXML
  public void initialize() {
    // --- PHẦN 1: SETUP BẢNG DỮ LIỆU ---
    colUserId.setCellValueFactory(new PropertyValueFactory<>("userId"));
    colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
    colFullname.setCellValueFactory(
        cellData -> {
          AuthResponse user = cellData.getValue();
          return new SimpleStringProperty(joinName(user.getLastname(), user.getFirstname()));
        });
    colRole.setCellValueFactory(new PropertyValueFactory<>("role"));

    // Tạo nút bấm "Khóa/Mở" trong cột Thao tác
    colAction.setCellFactory(
        param ->
            new TableCell<>() {
              private final Button btnToggle = new Button("Khóa/Mở");

              {
                btnToggle.setStyle(
                    "-fx-background-color: #f39c12; -fx-text-fill: white; -fx-cursor: hand;");
              }

              @Override
              protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                  setGraphic(null);
                } else {
                  btnToggle.setOnAction(
                      event -> {
                        AuthResponse user = getTableView().getItems().get(getIndex());
                        handleToggleBan(user.getUserId());
                      });
                  setGraphic(btnToggle);
                }
              }
            });

    // --- PHẦN 2: LOAD DỮ LIỆU THẬT TỪ SERVER ---
    refreshTable();
  }

  private void refreshTable() {
    new Thread(
            () -> {
              try {
                HttpRequest request =
                    HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/admin/users"))
                        .GET()
                        .build();

                HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                  List<AuthResponse> users =
                      objectMapper.readValue(
                          response.body(), new TypeReference<List<AuthResponse>>() {});

                  Platform.runLater(
                      () -> {
                        usersTable.setItems(FXCollections.observableArrayList(users));
                        statUsers.setText(String.valueOf(users.size()));
                      });
                }
              } catch (Exception e) {
                System.err.println("Lỗi load bảng Admin: " + e.getMessage());
              }
            })
        .start();
  }

  private void handleToggleBan(Long userId) {
    new Thread(
            () -> {
              try {
                HttpRequest request =
                    HttpRequest.newBuilder()
                        .uri(
                            URI.create(
                                "http://localhost:8080/api/admin/users/toggle-ban/" + userId))
                        .PUT(HttpRequest.BodyPublishers.noBody())
                        .build();

                HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                  System.out.println("Đã thay đổi trạng thái user " + userId);
                  refreshTable(); // Cập nhật lại bảng ngay sau khi khóa/mở
                }
              } catch (Exception e) {
                e.printStackTrace();
              }
            })
        .start();
  }

  @FXML
  public void onLogoutClicked() {
    SceneManager.getInstance().switchScene("/client/fxml/user/login.fxml");
  }

  @FXML
  public void onDashboardClicked() {
    tabPane.getSelectionModel().select(0);
  }

  @FXML
  public void onManageUsersClicked() {
    tabPane.getSelectionModel().select(1);
  }

  @FXML
  public void onApproveAuctionsClicked() {
    tabPane.getSelectionModel().select(2);
  }

  @FXML
  public void onSettingsClicked() {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setHeaderText(null);
    alert.setContentText("Cài đặt dành cho Admin chưa được triển khai.");
    alert.showAndWait();
  }

  private String joinName(String lastname, String firstname) {
    String fullName =
        ((lastname == null ? "" : lastname) + " " + (firstname == null ? "" : firstname)).trim();
    return fullName.isEmpty() ? "-" : fullName;
  }
}
