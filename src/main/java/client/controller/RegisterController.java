package client.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import client.pattern.ClientHttp;
import client.pattern.SceneManager;
import common.Constants;
import common.enums.Role;
import common.request.RegisterRequest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController {

  @FXML public Label messageLabel;
  @FXML private TextField txtfirstname; // Khớp fx:id="txtfirstname"
  @FXML private TextField txtlastname; // Khớp fx:id="txtlastname"
  @FXML private TextField txtUsername;
  @FXML private PasswordField txtPassword;
  @FXML private Label lblStatus;

  private final ObjectMapper objectMapper = ClientHttp.mapper();
  private final HttpClient httpClient = ClientHttp.client();

  @FXML
  private void onRegisterButtonClicked() {
    // Kiểm tra an toàn để tránh NullPointerException
    if (txtfirstname == null || txtlastname == null) {
      System.out.println("Lỗi: UI chưa được load đúng ID!");
      return;
    }

    String firstname = txtfirstname.getText().trim();
    String lastname = txtlastname.getText().trim();
    String username = txtUsername.getText().trim();
    String password = txtPassword.getText().trim();

    if (firstname.isEmpty() || lastname.isEmpty() || username.isEmpty() || password.isEmpty()) {
      showError("Vui lòng điền đủ thông tin!");
      return;
    }
    if (!username.matches("^[a-zA-Z0-9]+$")) {
      showError("Tên đăng nhập chỉ được dùng chữ cái không dấu và số!");
      return;
    }

    // Truyền đủ 5 tham số theo đúng RegisterRequest DTO
    RegisterRequest requestDto =
        new RegisterRequest(username, password, firstname, lastname, Role.BIDDER);

    new Thread(
            () -> {
              try {
                String jsonBody = objectMapper.writeValueAsString(requestDto);
                HttpRequest request =
                    HttpRequest.newBuilder()
                        .uri(URI.create(Constants.BASE_URL + "/auth/register"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                Platform.runLater(
                    () -> {
                      if (response.statusCode() == 200 || response.statusCode() == 201) {
                        SceneManager.getInstance()
                            .switchScene(
                                "/client/user/login.fxml",
                                controller -> {
                                  if (controller instanceof LoginController loginController) {
                                    loginController.setUsername(username);
                                  }
                                });
                      } else {
                        showError("Lỗi: " + response.body());
                      }
                    });
              } catch (Exception e) {
                e.printStackTrace();
                showError("Không thể kết nối Server!");
              }
            })
        .start();
  }

  @FXML
  private void onBackToLoginClick() {
    SceneManager.getInstance().switchScene("/client/user/login.fxml");
  }

  private void showError(String message) {
    Platform.runLater(
        () -> {
          lblStatus.setText(message);
          lblStatus.setVisible(true);
          lblStatus.setManaged(true);
        });
  }

  @FXML
  public void initialize() {
    // Lắng nghe phím Enter trên ô Username
    txtUsername.setOnKeyPressed(
        event -> {
          if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
            onRegisterButtonClicked();
          }
        });

    // Lắng nghe phím Enter trên ô Password
    txtPassword.setOnKeyPressed(
        event -> {
          if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
            onRegisterButtonClicked();
          }
        });
  }
}
