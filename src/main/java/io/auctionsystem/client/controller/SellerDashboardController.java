package io.auctionsystem.client.controller;

import io.auctionsystem.client.pattern.AuctionManager;
import io.auctionsystem.client.pattern.SceneManager;
import io.auctionsystem.common.response.AuthResponse;
import javafx.fxml.FXML;

public class SellerDashboardController extends BaseDashboardController {

    @FXML
    public void initialize() {
        AuthResponse user = AuctionManager.getInstance().getCurrentUser();
        String storeName = (user != null && user.getStoreName() != null) ? user.getStoreName().trim() : "";
        lblWelcome.setText("Xin chào, " + storeName + "!");
        onHomeButtonClicked();
    }

    // -- CÁC NÚT RIÊNG CỦA SELLER --
    @FXML
    public void onAddAuctionClicked() {
        loadSubView("/client/fxml/add_auction_view.fxml");
    }

    @FXML
    public void onManageAuctionsClicked() {
        loadSubView("/client/fxml/manage_auctions_view.fxml");
    }

    @FXML
    public void onRevenueChartClicked() {
        loadSubView("/client/fxml/revenue_chart_view.fxml");
    }

    @FXML
    public void onBackToBuyerChannelClicked() {
        SceneManager.getInstance().switchScene("/client/fxml/bidder_dashboard.fxml");
    }

    @FXML
    public void onOpenSettings() {
        SettingsController.isSellerChannel = true;
        SceneManager.getInstance().switchScene("/client/fxml/settings.fxml");
    }
}