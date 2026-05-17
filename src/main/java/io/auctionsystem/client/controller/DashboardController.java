package io.auctionsystem.client.controller;

import io.auctionsystem.client.pattern.AuctionManager;
import io.auctionsystem.client.pattern.SceneManager;
import io.auctionsystem.common.enums.Role;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.io.IOException;

public class DashboardController {

    private static DashboardController instance;

    @FXML private BorderPane rootPane;
    @FXML private VBox sidebar;
    @FXML private StackPane contentArea; // Vùng trống ở giữa để load các View con

    @FXML private Button btnHome;
    @FXML private Button btnProductList;
    @FXML private Button btnWallet;
    @FXML private Label lblWelcome;

    private static final String ACTIVE_MENU_STYLE = "-fx-background-color: #1abc9c; -fx-text-fill: white; -fx-cursor: hand; -fx-alignment: CENTER_LEFT;";
    private static final String INACTIVE_MENU_STYLE = "-fx-background-color: transparent; -fx-text-fill: #bdc3c7; -fx-cursor: hand; -fx-alignment: CENTER_LEFT;";

    public DashboardController() {
        instance = this;
    }

    public static DashboardController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        String firstname = AuctionManager.getInstance().getFirstname();
        String username = AuctionManager.getInstance().getUsername();
        String display = (firstname != null && !firstname.trim().isEmpty()) ? firstname : username;
        lblWelcome.setText("Xin chào, " + display + "!");

        if (rootPane != null && sidebar != null) {
            sidebar.prefWidthProperty().bind(rootPane.widthProperty().multiply(0.22));
        }

        onHomeButtonClicked(); // Mặc định mở trang chủ
    }

    private void loadSubView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onHomeButtonClicked() {
        loadSubView("/client/fxml/home_view.fxml");
        setActiveMenu(btnHome, btnProductList, btnWallet);
    }

    @FXML
    public void onProductListButtonClicked() {
        loadSubView("/client/fxml/product_list_view.fxml");
        setActiveMenu(btnProductList, btnHome, btnWallet);
    }

    @FXML
    public void onWalletButtonClicked() {
        loadSubView("/client/fxml/wallet_view.fxml");
        setActiveMenu(btnWallet, btnHome, btnProductList);
    }

    private void setActiveMenu(Button activeButton, Button... inactiveButtons) {
        if (activeButton != null) activeButton.setStyle(ACTIVE_MENU_STYLE);
        for (Button btn : inactiveButtons) {
            if (btn != null) btn.setStyle(INACTIVE_MENU_STYLE);
        }
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

        alert.showAndWait().ifPresent(response -> {
            if (response == btnYes) {
                AuctionManager.getInstance().isLoggedOut();
                SceneManager.getInstance().switchScene("/client/fxml/login.fxml");
            }
        });
    }

    @FXML
    public void onSellerChannelButtonClicked() {
        if (AuctionManager.getInstance().getRole() == Role.SELLER) {
            SceneManager.getInstance().switchScene("/client/fxml/seller_dashboard.fxml");
        } else {
            SceneManager.getInstance().switchScene("/client/fxml/seller_registration.fxml");
        }
    }

    @FXML
    public void onOpenSettings() {
        SceneManager.getInstance().switchScene("/client/fxml/settings.fxml");
    }
}