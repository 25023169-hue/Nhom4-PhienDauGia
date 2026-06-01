package client.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import client.pattern.AuctionManager;
import client.pattern.ClientHttp;
import client.pattern.SceneManager;
import common.Constants;
import common.enums.Role;
import common.request.LoginRequest;
import common.response.AuthResponse;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

  @FXML private TextField txtUsername;
  @FXML private PasswordField txtPassword;

  // Các Label lỗi
  @FXML private Label lblUserError;
  @FXML private Label lblPassError;
  @FXML private Label lblGeneralError;

  private final ObjectMapper objectMapper = ClientHttp.mapper();
  private final HttpClient httpClient = ClientHttp.client();

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
    if (!username.isEmpty() && !username.matches("^[a-zA-Z0-9]+$")) {
      lblGeneralError.setText("Tài khoản hoặc mật khẩu không chính xác!");
      showError(lblGeneralError);
      hasError = true;
    }

    if (hasError) return;

    // 3. Gửi Request lên Server
    new Thread(
            () -> {
              try {
                LoginRequest requestDto = new LoginRequest(username, password);
                String jsonBody = objectMapper.writeValueAsString(requestDto);

                HttpRequest request =
                    HttpRequest.newBuilder()
                        .uri(URI.create(Constants.LOGIN_ENDPOINT))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                Platform.runLater(
                    () -> {
                      if (response.statusCode() == 200) {
                        try {
                          // Cấu hình để Jackson không bị crash khi Server trả về dư trường
                          objectMapper.configure(
                              com.fasterxml.jackson.databind.DeserializationFeature
                                  .FAIL_ON_UNKNOWN_PROPERTIES,
                              false);

                          AuthResponse authResp =
                              objectMapper.readValue(response.body(), AuthResponse.class);
                          AuctionManager.getInstance().setCurrentUser(authResp);

                          System.out.println("Đăng nhập thành công: " + authResp.getFirstname());

                          // =========================================================
                          // CẬP NHẬT: Thêm nhánh rẻ cho Role.ADMIN
                          // =========================================================
                          if (authResp.getRole() == Role.ADMIN) {
                            SceneManager.getInstance()
                                .switchScene("/client/admin_dashboard.fxml");
                          } else if (authResp.getRole() == Role.SELLER) {
                            SceneManager.getInstance()
                                .switchScene("/client/user/seller/seller_dashboard.fxml");
                          } else {
                            SceneManager.getInstance()
                                .switchScene("/client/user/bidder/bidder_dashboard.fxml");
                          }
                          // =========================================================

                        } catch (Exception e) {
                          System.err.println(">>> Lỗi parse JSON: " + e.getMessage());
                          lblGeneralError.setText("Lỗi xử lý phản hồi từ server.");
                          showError(lblGeneralError);
                        }
                      } else {
                        // Hiển thị lỗi từ Server trả về (ví dụ: "Mật khẩu không chính xác")
                        lblGeneralError.setText(response.body());
                        showError(lblGeneralError);
                      }
                    });
              } catch (Exception e) {
                Platform.runLater(
                    () -> {
                      lblGeneralError.setText("Không thể kết nối đến máy chủ!");
                      showError(lblGeneralError);
                    });
              }
            })
        .start();
  }

  @FXML
  public void onRegisterLinkClick() {
    SceneManager.getInstance().switchScene("/client/user/register.fxml");
  }

  public void setUsername(String username) {
    txtUsername.setText(username);
    txtPassword.requestFocus();
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
    txtUsername.setOnKeyPressed(
        event -> {
          if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
            onLoginButtonClicked();
          }
        });

    // Lắng nghe phím Enter trên ô Password
    txtPassword.setOnKeyPressed(
        event -> {
          if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
            onLoginButtonClicked();
          }
        });
  }
}
