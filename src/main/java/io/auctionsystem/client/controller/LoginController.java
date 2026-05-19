package io.auctionsystem.client.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.auctionsystem.client.pattern.AuctionManager;
import io.auctionsystem.client.pattern.SceneManager;
import io.auctionsystem.common.response.AuthResponse;
import io.auctionsystem.common.request.LoginRequest;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;

    // Các Label lỗi
    @FXML private Label lblUserError;
    @FXML private Label lblPassError;
    @FXML private Label lblGeneralError;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @FXML
    public void onLoginButtonClicked() {
        // 1. Reset các lỗi cũ
        hideAllErrors();


        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();
        boolean hasError = false;

        // 2. Validate nội bộ tại Client
        if (username.isEmpty()) {
            showError(lblUserError);
            hasError = true;
        }
        if (password.isEmpty()) {
            showError(lblPassError);
            hasError = true;
        }

        if (hasError) return;

        // 3. Gửi Request lên Server
        new Thread(() -> {
            try {
                LoginRequest requestDto = new LoginRequest(username, password);
                String jsonBody = objectMapper.writeValueAsString(requestDto);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/auth/login"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        try {
                            // Cấu hình để Jackson không bị crash khi Server trả về dư trường
                            objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

                            AuthResponse authResp = objectMapper.readValue(response.body(), AuthResponse.class);
                            AuctionManager.getInstance().setCurrentUser(authResp);

                            System.out.println("Đăng nhập thành công: " + authResp.getFirstname());

                            // CHUYỂN MÀN HÌNH SANG DASHBOARD THÔNG QUA SCENEMANAGER
                            SceneManager.getInstance().switchScene("/client/fxml/bidder_dashboard.fxml");

                        } catch (Exception e) {
                            System.err.println(">>> Lỗi parse JSON ngầm (Đã bỏ qua): " + e.getMessage());
                        }
                    } else {
                        // Hiển thị lỗi từ Server trả về (ví dụ: "Mật khẩu không chính xác")
                        lblGeneralError.setText(response.body());
                        showError(lblGeneralError);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblGeneralError.setText("Không thể kết nối đến máy chủ!");
                    showError(lblGeneralError);
                });
            }
        }).start();
    }

    @FXML
    public void onRegisterLinkClick() {
        SceneManager.getInstance().switchScene("/client/fxml/register.fxml");
    }

    // ================= HÀM HỖ TRỢ HIỂN THỊ LỖI =================
    private void showError(Label label) {
        label.setVisible(true);
        label.setManaged(true);
    }

    private void hideAllErrors() {
        Label[] errors = {lblUserError, lblPassError, lblGeneralError};
        for (Label lbl : errors) {
            lbl.setVisible(false);
            lbl.setManaged(false);
        }
    }
    // =========================================================

    @FXML
    public void initialize() {
        // Lắng nghe phím Enter trên ô Username
        txtUsername.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                onLoginButtonClicked();
            }
        });

        // Lắng nghe phím Enter trên ô Password
        txtPassword.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                onLoginButtonClicked();
            }
        });
    }
}