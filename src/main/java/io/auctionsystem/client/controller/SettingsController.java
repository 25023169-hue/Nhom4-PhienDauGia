package io.auctionsystem.client.controller;

import io.auctionsystem.client.pattern.AuctionManager;
import io.auctionsystem.client.pattern.SceneManager;
import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

public class SettingsController {

  @FXML private StackPane contentArea;
  @FXML private Button btnProfile, btnBank, btnAddress, btnPassword;

  public static boolean isSellerChannel = false;

  @FXML
  public void initialize() {
    String requestedTab = AuctionManager.getInstance().consumeSettingsTabRequest();
    if ("BANK".equals(requestedTab)) {
      showBank();
    } else {
      showProfile();
    }
  }

  // Đổi tên các file fxml gọi ở đây
  public void showProfile() {
    loadTab("/client/fxml/settings/profile_setting.fxml", btnProfile);
  }

  public void showBank() {
    loadTab("/client/fxml/settings/bank_setting.fxml", btnBank);
  }

  public void showAddress() {
    loadTab("/client/fxml/settings/address_setting.fxml", btnAddress);
  }

  public void showPassword() {
    loadTab("/client/fxml/settings/password_setting.fxml", btnPassword);
  }

  private void loadTab(String fxmlPath, Button activeBtn) {
    try {
      contentArea.getChildren().clear();
      FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
      Parent view = loader.load();
      contentArea.getChildren().add(view);
      updateButtonStyles(activeBtn);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void updateButtonStyles(Button activeBtn) {
    String normalStyle =
        "-fx-background-color: transparent; -fx-text-fill: black; -fx-font-weight: normal;";
    btnProfile.setStyle(normalStyle);
    btnBank.setStyle(normalStyle);
    btnAddress.setStyle(normalStyle);
    btnPassword.setStyle(normalStyle);

    activeBtn.setStyle(
        "-fx-background-color: #e9ecef; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
  }

  @FXML
  public void onBack() {
    if (isSellerChannel) {
      SceneManager.getInstance().switchScene("/client/fxml/user/seller/seller_dashboard.fxml");
    } else {
      SceneManager.getInstance().switchScene("/client/fxml/user/bidder/bidder_dashboard.fxml");
    }
  }
}
