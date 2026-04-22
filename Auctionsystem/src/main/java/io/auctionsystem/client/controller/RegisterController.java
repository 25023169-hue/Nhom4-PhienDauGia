package io.auctionsystem.client.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.auctionsystem.client.pattern.SceneManager;
import io.auctionsystem.common.dto.RegisterRequest;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class RegisterController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<String> cbRole;

    @FXML private Label lblUserError;
    @FXML private Label lblPassError;
    @FXML private Label lblStatus;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @FXML
    public void initialize() {
        // 1. Khởi tạo dữ liệu cho ComboBox
        cbRole.setItems(FXCollections.observableArrayList("BIDDER", "SELLER"));
        cbRole.getSelectionModel().selectFirst();

        // 2. Gộp xử lý phím Enter cho tất cả các ô nhập liệu
        txtUsername.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) onRegisterButtonClicked();
        });

        txtPassword.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) onRegisterButtonClicked();
        });

        cbRole.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) onRegisterButtonClicked();
        });
    }

    @FXML
    public void onRegisterButtonClicked() {
        hideErrors();

        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();
        String role = cbRole.getValue();
        boolean hasError = false;

        if (username.isEmpty()) {
            showError(lblUserError);
            hasError = true;
        }
        if (password.isEmpty()) {
            showError(lblPassError);
            hasError = true;
        }

        if (hasError) return;

        new Thread(() -> {
            try {
                RegisterRequest requestDto = new RegisterRequest(username, password, role);
                String jsonBody = objectMapper.writeValueAsString(requestDto);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/auth/register"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        lblStatus.setText("Đăng ký thành công! Hãy đăng nhập.");
                        lblStatus.setTextFill(Color.GREEN);
                        showError(lblStatus);
                    } else {
                        String body = response.body();
                        lblStatus.setTextFill(Color.RED);

                        // SỬA TẠI ĐÂY: Thay vì hiện cục JSON, hiện chữ tiếng Việt nếu lỗi 500
                        if (response.statusCode() == 500 || body.contains("\"status\":500")) {
                            lblStatus.setText("Lỗi hệ thống hoặc Database chưa sẵn sàng!");
                        } else {
                            lblStatus.setText(body); // Hiện lỗi cụ thể từ Server (vd: Trùng tên)
                        }
                        showError(lblStatus);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatus.setText("Lỗi kết nối server!");
                    lblStatus.setTextFill(Color.RED);
                    showError(lblStatus);
                });
            }
        }).start();
    }

    @FXML
    public void onBackToLoginClick() {
        SceneManager.getInstance().switchScene("/client/fxml/login.fxml");
    }

    private void showError(Label label) {
        label.setVisible(true);
        label.setManaged(true);
    }

    private void hideErrors() {
        lblUserError.setVisible(false);
        lblUserError.setManaged(false);
        lblPassError.setVisible(false);
        lblPassError.setManaged(false);
        lblStatus.setVisible(false);
        lblStatus.setManaged(false);
    }
}