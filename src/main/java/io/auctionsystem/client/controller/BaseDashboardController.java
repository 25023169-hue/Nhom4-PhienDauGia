package io.auctionsystem.client.controller;

import io.auctionsystem.client.pattern.AuctionManager;
import io.auctionsystem.client.pattern.SceneManager;
import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public abstract class BaseDashboardController {

  // Vùng trống ở giữa để load các View con (Dùng chung cho cả 2 giao diện)
  @FXML protected StackPane contentArea;
  @FXML protected Label lblWelcome;

  protected static final String ACTIVE_MENU_STYLE =
      "-fx-background-color: #1abc9c; -fx-text-fill: white; -fx-cursor: hand; -fx-alignment: CENTER_LEFT;";
  protected static final String INACTIVE_MENU_STYLE =
      "-fx-background-color: transparent; -fx-text-fill: #bdc3c7; -fx-cursor: hand; -fx-alignment: CENTER_LEFT;";

  // Hàm load các FXML con vào chính giữa màn hình
  protected void loadSubView(String fxmlPath) {
    if (contentArea == null) {
      System.err.println("Lỗi: Chưa có StackPane fx:id='contentArea' ở giao diện!");
      return;
    }
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
      Node view = loader.load();
      contentArea.getChildren().setAll(view);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // Hàm đổi màu nút đang chọn
  protected void setActiveMenu(Button activeButton, Button... inactiveButtons) {
    if (activeButton != null) activeButton.setStyle(ACTIVE_MENU_STYLE);
    for (Button btn : inactiveButtons) {
      if (btn != null) btn.setStyle(INACTIVE_MENU_STYLE);
    }
  }

  // ==========================================
  // CÁC SỰ KIỆN DÙNG CHUNG CHO CẢ 2 BÊN
  // ==========================================
  @FXML
  public void onHomeButtonClicked() {
    // Code này giả định bạn sẽ tách phần giao diện chính giữa thành home_view.fxml
    loadSubView("/client/fxml/user/home_view.fxml");
  }

  @FXML
  public void onWalletButtonClicked() {
    loadSubView("/client/fxml/user/wallet_view.fxml");
  }

  @FXML
  public void onNotificationsClicked() {
    loadSubView("/client/fxml/notifications_view.fxml");
  }

  @FXML
  public void onOpenSettings() {
    SceneManager.getInstance().switchScene("/client/fxml/settings/settings.fxml");
  }

  @FXML
  public void onLogoutButtonClicked() {
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    alert.setHeaderText("Xác nhận đăng xuất");
    alert.setContentText("Bạn có chắc chắn muốn đăng xuất?");
    Window ownerWindow = lblWelcome.getScene().getWindow();
    alert.initOwner(ownerWindow);
    alert.initStyle(StageStyle.UNDECORATED);

    ButtonType btnYes = new ButtonType("Có", ButtonBar.ButtonData.YES);
    ButtonType btnNo = new ButtonType("Không", ButtonBar.ButtonData.NO);
    alert.getButtonTypes().setAll(btnYes, btnNo);

    alert
        .showAndWait()
        .ifPresent(
            response -> {
              if (response == btnYes) {
                AuctionManager.getInstance().isLoggedOut();
                SceneManager.getInstance().switchScene("/client/fxml/user/login.fxml");
              }
            });
  }
}
