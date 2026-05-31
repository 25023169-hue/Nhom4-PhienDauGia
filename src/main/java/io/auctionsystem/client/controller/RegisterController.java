package io.auctionsystem.client.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.auctionsystem.client.pattern.SceneManager;
import io.auctionsystem.common.request.RegisterRequest;
import io.auctionsystem.common.enums.Role;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class RegisterController {

    // --- GIỮ NGUYÊN TOÀN BỘ KHAI BÁO CỦA NHÓM (Tuyệt đối không xóa) ---
    @FXML public Label messageLabel;
    @FXML private TextField txtfirstname;
    @FXML private TextField txtlastname;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblStatus;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @FXML
    private void onRegisterButtonClicked() {
        // 1. Thêm: Báo lỗi TRỰC TIẾP lên màn hình nếu đồng đội vô tình xóa ID bên FXML
        if (txtfirstname == null || txtlastname == null || txtUsername == null || txtPassword == null) {
            System.err.println("Lỗi: UI chưa được load đúng ID!");
            showError("Lỗi giao diện: Chưa load đúng ID form!");
            return;
        }

        String firstname = txtfirstname.getText().trim();
        String lastname = txtlastname.getText().trim();
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        if (firstname.isEmpty() || lastname.isEmpty() || username.isEmpty() || password.isEmpty()) {
            showError("Vui lòng điền đủ thông tin!");
            // 2. Thêm: Tận dụng messageLabel của nhóm để nhấn mạnh lỗi ở khu vực mật khẩu
            if (messageLabel != null) {
                messageLabel.setText("Thiếu thông tin đăng ký!");
                messageLabel.setStyle("-fx-text-fill: red;");
            }
            return;
        }

        // Xóa thông báo lỗi cũ nếu lần nhập này đã hợp lệ
        if (messageLabel != null) messageLabel.setText("");

        RegisterRequest requestDto = new RegisterRequest(username, password, firstname, lastname, Role.BIDDER);

        new Thread(() -> {
            try {
                String jsonBody = objectMapper.writeValueAsString(requestDto);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/auth/register"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                Platform.runLater(() -> {
                    if (response.statusCode() == 200 || response.statusCode() == 201) {
                        // 3. THÊM: Hiện bảng thông báo thành công để người dùng biết chắc chắn
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setHeaderText(null);
                        alert.setContentText("Chúc mừng! Bạn đã đăng ký tài khoản thành công.");
                        alert.showAndWait();

                        SceneManager.getInstance().switchScene("/client/fxml/user/login.fxml");
                    } else {
                        showError("Lỗi: " + response.body());
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                showError("Không thể kết nối Server!");
            }
        }).start();
    }

    @FXML
    private void onBackToLoginClick() {
        SceneManager.getInstance().switchScene("/client/fxml/user/login.fxml");
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            // 4. Thêm: Check null an toàn cho lblStatus
            if (lblStatus != null) {
                lblStatus.setText(message);
                lblStatus.setVisible(true);
                lblStatus.setManaged(true);
            }
        });
    }

    @FXML
    public void initialize() {
        // 5. Thêm: Đảm bảo các listener chỉ chạy khi các ô Text đã được load
        if (txtUsername != null) {
            txtUsername.setOnKeyPressed(event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                    onRegisterButtonClicked();
                }
            });
        }
        if (txtPassword != null) {
            txtPassword.setOnKeyPressed(event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                    onRegisterButtonClicked();
                }
            });
        }
    }
}