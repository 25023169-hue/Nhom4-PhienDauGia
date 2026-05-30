package io.auctionsystem.client.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.auctionsystem.client.pattern.AuctionManager;
import io.auctionsystem.client.pattern.SceneManager;
import io.auctionsystem.common.response.AuthResponse;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ProfileSettingController {
    @FXML private TextField txtUsername, txtLastName, txtFirstName;
    @FXML private Button btnDeleteAccount;

    // --- PHẦN THÊM MỚI ---
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @FXML
    public void initialize() {
        AuthResponse user = AuctionManager.getInstance().getCurrentUser();
        if (user != null) {
            txtUsername.setText(user.getUsername() != null ? user.getUsername() : "");
            txtLastName.setText(user.getLastname() != null ? user.getLastname() : "");
            txtFirstName.setText(user.getFirstname() != null ? user.getFirstname() : "");
        }
    }

    @FXML
    public void saveProfile() {
        String firstName = txtFirstName.getText().trim();
        String lastName = txtLastName.getText().trim();

        if (firstName.isEmpty() || lastName.isEmpty()) {
            showAlert("Vui lòng nhập đủ Họ và Tên!");
            return;
        }

        AuthResponse user = AuctionManager.getInstance().getCurrentUser();
        if (user == null) return;

        new Thread(() -> {
            try {
                Map<String, String> payload = new HashMap<>();
                payload.put("firstname", firstName);
                payload.put("lastname", lastName);
                String jsonBody = objectMapper.writeValueAsString(payload);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/user-profile/" + user.getUserId() + "/name"))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        user.setFirstname(firstName);
                        user.setLastname(lastName);
                        showAlert("Đã lưu thông tin hồ sơ cá nhân thành công!");
                    } else {
                        showAlert("Lỗi: " + response.body());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Lỗi kết nối Server!"));
            }
        }).start();
    }

    @FXML
    public void deleteAccount() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận xóa tài khoản");
        alert.setHeaderText("Cảnh báo: Hành động này không thể hoàn tác!");
        alert.setContentText("Bạn có chắc chắn muốn xóa vĩnh viễn tài khoản này không?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            AuthResponse user = AuctionManager.getInstance().getCurrentUser();
            if (user == null) return;

            btnDeleteAccount.setDisable(true);
            new Thread(() -> {
                try {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:8080/api/user/" + user.getUserId()))
                            .DELETE()
                            .build();

                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    Platform.runLater(() -> handleDeleteResponse(response));
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        btnDeleteAccount.setDisable(false);
                        showAlert("Không thể kết nối đến Server để xóa tài khoản!");
                    });
                }
            }).start();
        }
    }

    private void handleDeleteResponse(HttpResponse<String> response) {
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            AuctionManager.getInstance().isLoggedOut();
            showAlert("Tài khoản đã được xóa thành công.");
            SceneManager.getInstance().switchScene("/client/fxml/user/login.fxml");
            return;
        }

        btnDeleteAccount.setDisable(false);
        showAlert("Không thể xóa tài khoản: " + response.body());
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
