package io.auctionsystem.client.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.auctionsystem.client.pattern.AuctionManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class PasswordSettingController {
  @FXML private PasswordField txtOldPass, txtNewPass;
  @FXML private TextField txtRecovery;

  // --- PHẦN THÊM MỚI ---
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final HttpClient httpClient = HttpClient.newHttpClient();

  @FXML
  public void savePassword() {
    String oldPass = txtOldPass.getText().trim();
    String newPass = txtNewPass.getText().trim();

    if (oldPass.isEmpty() || newPass.isEmpty()) {
      showAlert("Vui lòng nhập đủ mật khẩu cũ và mới!");
      return;
    }

    Long userId = AuctionManager.getInstance().getId();
    if (userId == null) return;

    new Thread(
            () -> {
              try {
                Map<String, String> payload = new HashMap<>();
                payload.put("oldPassword", oldPass);
                payload.put("newPassword", newPass);
                String jsonBody = objectMapper.writeValueAsString(payload);

                HttpRequest request =
                    HttpRequest.newBuilder()
                        .uri(
                            URI.create(
                                "http://localhost:8080/api/user-profile/" + userId + "/password"))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                Platform.runLater(
                    () -> {
                      if (response.statusCode() == 200) {
                        txtOldPass.clear();
                        txtNewPass.clear();
                        showAlert("Đổi mật khẩu thành công!");
                      } else {
                        showAlert("Lỗi: " + response.body());
                      }
                    });
              } catch (Exception e) {
                Platform.runLater(() -> showAlert("Lỗi kết nối Server!"));
              }
            })
        .start();
  }

  private void showAlert(String msg) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setHeaderText(null);
    alert.setContentText(msg);
    alert.showAndWait();
  }
}
