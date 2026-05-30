package io.auctionsystem.client.controller;

import io.auctionsystem.client.pattern.AuctionManager;
import io.auctionsystem.client.pattern.SceneManager;
import io.auctionsystem.common.response.AuthResponse;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class SellerDashboardController extends BaseDashboardController {

    private static SellerDashboardController instance;

    @FXML private Label lblSellerName;
    @FXML private Label lblSellerRole;
    @FXML private Button btnProducts;
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
        String storeName = user != null && user.getStoreName() != null && !user.getStoreName().trim().isEmpty()
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
        loadSubView("/client/fxml/user/seller/add_auction_view.fxml");
        setActiveMenu(btnProducts, btnWallet, btnNotifications);
    }

    @FXML
    public void onManageAuctionsClicked() {
        loadSubView("/client/fxml/user/seller/manage_auctions_view.fxml");
        setActiveMenu(btnProducts, btnWallet, btnNotifications);
    }

    @Override
    @FXML
    public void onWalletButtonClicked() {
        super.onWalletButtonClicked();
        setActiveMenu(btnWallet, btnProducts, btnNotifications);
    }

    @Override
    @FXML
    public void onNotificationsClicked() {
        super.onNotificationsClicked();
        setActiveMenu(btnNotifications, btnProducts, btnWallet);
    }

    @FXML
    public void onBuyerChannelClicked() {
        SceneManager.getInstance().switchScene("/client/fxml/user/bidder/bidder_dashboard.fxml");
    }
    @FXML
    public void onOpenSettings() {
        SettingsController.isSellerChannel = true;
        SceneManager.getInstance().switchScene("/client/fxml/settings/settings.fxml");
    }
}
