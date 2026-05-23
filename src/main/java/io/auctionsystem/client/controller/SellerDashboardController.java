package io.auctionsystem.client.controller;

import io.auctionsystem.client.pattern.AuctionManager;
import io.auctionsystem.common.response.AuthResponse;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class SellerDashboardController extends BaseDashboardController {

    private static SellerDashboardController instance;

    @FXML private Label lblSellerName;
    @FXML private Label lblSellerRole;
    @FXML private Button btnProducts;

    public SellerDashboardController() {
        instance = this;
    }

    public static SellerDashboardController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
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
        loadSubView("/client/fxml/add_auction_view.fxml");
        setActiveMenu(btnProducts);
    }

    @FXML
    public void onManageAuctionsClicked() {
        loadSubView("/client/fxml/manage_auctions_view.fxml");
        setActiveMenu(btnProducts);
    }
}
