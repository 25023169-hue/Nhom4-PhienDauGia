package client.controller;

import client.pattern.AuctionManager;
import client.pattern.SceneManager;
import common.enums.Role;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class BidderDashboardController extends BaseDashboardController {

  private static BidderDashboardController instance;

  @FXML private BorderPane rootPane;
  @FXML private VBox sidebar;

  @FXML private Button btnHome;
  @FXML private Button btnProductList;
  @FXML private Button btnWallet;

  public BidderDashboardController() {
    instance = this;
  }

  public static BidderDashboardController getInstance() {
    return instance;
  }

  @FXML
  public void initialize() {
    SettingsController.isSellerChannel = false;
    // CHỈNH SỬA: Lấy trực tiếp firstname vì đã bắt buộc có từ lúc đăng ký
    String display = AuctionManager.getInstance().getFirstname();
    lblWelcome.setText("Xin chào, " + (display != null ? display : "") + "!");
    if (rootPane != null && sidebar != null) {
      sidebar.prefWidthProperty().bind(rootPane.widthProperty().multiply(0.22));
    }

    onHomeButtonClicked();
    initializeNotificationUpdates();
  }

  @Override
  @FXML
  public void onHomeButtonClicked() {
    super.onHomeButtonClicked();
    setActiveMenu(btnHome, btnProductList, btnWallet);
  }

  @Override
  @FXML
  public void onWalletButtonClicked() {
    super.onWalletButtonClicked();
    setActiveMenu(btnWallet, btnHome, btnProductList);
  }

  @FXML
  public void onProductListButtonClicked() {
    loadSubView("/client/user/bidder/product_list_view.fxml");
    setActiveMenu(btnProductList, btnHome, btnWallet);
  }

  @FXML
  public void onLiveBidsClicked() {
    loadSubView("/client/user/bidder/live_bids_view.fxml");
  }

  @FXML
  public void onPurchaseHistoryClicked() {
    loadSubView("/client/user/bidder/purchase_history_view.fxml");
  }

  @FXML
  public void onInventoryClicked() {
    loadSubView("/client/user/bidder/inventory_view.fxml");
  }

  @FXML
  public void onSellerChannelButtonClicked() {
    stopNotificationUpdates();
    if (AuctionManager.getInstance().getRole() == Role.SELLER) {
      SceneManager.getInstance().switchScene("/client/user/seller/seller_dashboard.fxml");
    } else {
      SceneManager.getInstance().switchScene("/client/user/bidder/seller_registration.fxml");
    }
  }

  @FXML
  public void onOpenSettings() {
    stopNotificationUpdates();
    SettingsController.isSellerChannel = false;
    SceneManager.getInstance().switchScene("/client/settings/settings.fxml");
  }
}
