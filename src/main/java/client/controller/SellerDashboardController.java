package client.controller;

import client.pattern.AuctionManager;
import client.pattern.SceneManager;
import common.dto.SellerProductDTO;
import common.response.AuthResponse;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class SellerDashboardController extends BaseDashboardController {

  private static SellerDashboardController instance;

  @FXML private Label lblSellerName;
  @FXML private Label lblSellerRole;
  @FXML private Button btnProducts;
  @FXML private Button btnOrders;
  @FXML private Button btnRevenue;
  @FXML private Button btnWallet;
  @FXML private Button btnNotifications;

  public SellerDashboardController() {
    instance = this;
  }

  public static SellerDashboardController getInstance() {
    return instance;
  }

  @FXML
  public void initialize() {
    SettingsController.isSellerChannel = true;
    AuthResponse user = AuctionManager.getInstance().getCurrentUser();
    String storeName =
        user != null && user.getStoreName() != null && !user.getStoreName().trim().isEmpty()
            ? user.getStoreName().trim()
            : AuctionManager.getInstance().getUsername();

    lblSellerName.setText(storeName);
    lblSellerRole.setText("Chủ Kênh");
    lblWelcome.setText("Xin chào, " + storeName + "!");
    onManageAuctionsClicked();
  }

  @Override
  @FXML
  public void onHomeButtonClicked() {
    onManageAuctionsClicked();
  }

  @FXML
  public void onAddAuctionClicked() {
    SellerAddProductController.startCreating();
    loadSubView("/client/user/seller/add_auction_view.fxml");
    setActiveMenu(btnProducts, btnOrders, btnRevenue, btnWallet, btnNotifications);
  }

  public void onEditProductClicked(SellerProductDTO product) {
    SellerAddProductController.startEditing(product);
    loadSubView("/client/user/seller/add_auction_view.fxml");
    setActiveMenu(btnProducts, btnOrders, btnRevenue, btnWallet, btnNotifications);
  }

  @FXML
  public void onManageAuctionsClicked() {
    loadSubView("/client/user/seller/manage_auctions_view.fxml");
    setActiveMenu(btnProducts, btnOrders, btnRevenue, btnWallet, btnNotifications);
  }

  public void onProductClicked(SellerProductDTO product) {
    SellerProductDetailController.setProduct(product);
    loadSubView("/client/user/seller/product_detail_view.fxml");
    setActiveMenu(btnProducts, btnOrders, btnRevenue, btnWallet, btnNotifications);
  }

  @FXML
  public void onManageOrdersClicked() {
    loadSubView("/client/user/seller/order_management_view.fxml");
    setActiveMenu(btnOrders, btnProducts, btnRevenue, btnWallet, btnNotifications);
  }

  @FXML
  public void onRevenueStatisticsClicked() {
    loadSubView("/client/user/seller/revenue_statistics_view.fxml");
    setActiveMenu(btnRevenue, btnProducts, btnOrders, btnWallet, btnNotifications);
  }

  @Override
  @FXML
  public void onWalletButtonClicked() {
    super.onWalletButtonClicked();
    setActiveMenu(btnWallet, btnProducts, btnOrders, btnRevenue, btnNotifications);
  }

  @Override
  @FXML
  public void onNotificationsClicked() {
    super.onNotificationsClicked();
    setActiveMenu(btnNotifications, btnProducts, btnOrders, btnRevenue, btnWallet);
  }

  @FXML
  public void onBuyerChannelClicked() {
    SceneManager.getInstance().switchScene("/client/user/bidder/bidder_dashboard.fxml");
  }

  @FXML
  public void onOpenSettings() {
    SettingsController.isSellerChannel = true;
    SceneManager.getInstance().switchScene("/client/settings/settings.fxml");
  }
}
