package client.controller;

import client.ServerConnectionException;
import client.pattern.AuctionManager;
import client.pattern.ClientHttp;
import client.pattern.SceneManager;
import common.Constants;
import common.response.AuthResponse;
import java.net.URI;
import java.net.URLEncoder;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class SellerRegistrationController {

  @FXML private VBox introPane;
  @FXML private VBox formPane;
  @FXML private TextField txtStoreName;
  @FXML private Label lblError;



  @FXML
  public void initialize() {
    showIntroPane();
    lblError.setVisible(false);
    lblError.setManaged(false);
  }

  @FXML
  public void onStartRegistrationButtonClicked() {
    introPane.setVisible(false);
    introPane.setManaged(false);
    formPane.setVisible(true);
    formPane.setManaged(true);
    txtStoreName.requestFocus();
  }

  @FXML
  public void onConfirmButtonClicked() {
    // 1. Reset trạng thái ban đầu
    lblError.setVisible(false);
    lblError.setManaged(false);
    lblError.setStyle("-fx-text-fill: #e74c3c;"); // Mặc định màu đỏ của lỗi

    String storeName = txtStoreName.getText().trim();

    // 2. Validate nội bộ tại Client
    if (storeName.isEmpty()) {
      showError("Vui lòng nhập tên Cửa hàng/Shop!");
      return;
    }

    // Lấy thông tin user hiện tại từ AuctionManager
    AuthResponse currentUser = AuctionManager.getInstance().getCurrentUser();
    if (currentUser == null) {
      showError("Lỗi: Không tìm thấy phiên đăng nhập!");
      return;
    }

    // --- FIX LỖI 500 Ở ĐÂY ---
    Long id = currentUser.getUserId();
    if (id == null) {
      showError("Lỗi: Không lấy được ID tài khoản (ID null). Vui lòng đăng xuất và đăng nhập lại!");
      return;
    }

    // 3. Gửi Request lên Server
    new Thread(
            () -> {
              try {
                // Encode URL chuẩn xác để hỗ trợ cả Tiếng Việt có dấu và ký tự đặc biệt
                String encodedStoreName = URLEncoder.encode(storeName, StandardCharsets.UTF_8);

                String url =
                    Constants.BASE_URL
                        + "/auth/upgrade-seller/"
                        + id
                        + "?storeName="
                        + encodedStoreName;

                System.out.println(
                    ">>> ĐANG GỌI API: "
                        + url); // Sẽ in ra chuẩn .../upgrade-seller/1?storeName=...

                HttpRequest request =
                    HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();

                HttpResponse<String> response =
                    ClientHttp.send(request);

                Platform.runLater(
                    () -> {
                      if (response.statusCode() == 200) {
                        // --- IN RA DÒNG ĐĂNG KÝ THÀNH CÔNG ---
                        System.out.println(">>> ĐĂNG KÝ THÀNH CÔNG CHO ID: " + id);
                        currentUser.setRole(common.enums.Role.SELLER);

                        // Hiển thị thông báo trực quan lên giao diện
                        lblError.setText("✔ Chúc mừng! Đăng ký Kênh người bán thành công.");
                        lblError.setStyle(
                            "-fx-text-fill: #2ecc71; -fx-font-weight: bold;"); // Chuyển sang màu
                        // xanh lá
                        lblError.setVisible(true);
                        lblError.setManaged(true);

                        // Dừng lại 2 giây để người dùng kịp đọc trước khi chuyển màn hình
                        PauseTransition pause = new PauseTransition(Duration.seconds(2));
                        pause.setOnFinished(
                            event -> {
                              SceneManager.getInstance()
                                  .switchScene("/client/user/seller/seller_dashboard.fxml");
                            });
                        pause.play();

                      } else {
                        // Nếu Server trả về lỗi (như lỗi 400 Duplicate entry)
                        lblError.setStyle("-fx-text-fill: #e74c3c;");
                        showError("Đăng ký thất bại: " + response.body());
                      }
                    });
              } catch (Exception e) {
                Platform.runLater(() -> showError(ServerConnectionException.MESSAGE));
              }
            })
        .start();
  }

  @FXML
  public void onCancelButtonClicked() {
    SceneManager.getInstance().switchScene("/client/user/bidder/bidder_dashboard.fxml");
  }

  private void showIntroPane() {
    introPane.setVisible(true);
    introPane.setManaged(true);
    formPane.setVisible(false);
    formPane.setManaged(false);
  }

  private void showError(String message) {
    lblError.setText(message);
    lblError.setVisible(true);
    lblError.setManaged(true);
  }
}
