package io.auctionsystem.client.controller;

import io.auctionsystem.client.pattern.AuctionManager;
import io.auctionsystem.client.pattern.SceneManager;
import io.auctionsystem.common.dto.AuctionItemDTO;
import io.auctionsystem.common.enums.Role;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public class DashboardController {

    @FXML private BorderPane rootPane;
    @FXML private VBox sidebar;
    @FXML private VBox homeView;
    @FXML private VBox productListView;
    @FXML private Button btnHome;
    @FXML private Button btnProductList;
    @FXML private Label lblWelcome;

    @FXML private TableView<AuctionItemDTO> tableItems;
    @FXML private TableColumn<AuctionItemDTO, Long> colId;
    @FXML private TableColumn<AuctionItemDTO, String> colName;
    @FXML private TableColumn<AuctionItemDTO, Double> colCurrentPrice;
    @FXML private TableColumn<AuctionItemDTO, String> colEndTime;
    @FXML private TableColumn<AuctionItemDTO, String> colStatus;

    private static final String ACTIVE_MENU_STYLE = "-fx-background-color: #1abc9c; -fx-text-fill: white; "
            + "-fx-cursor: hand; -fx-alignment: CENTER_LEFT;";
    private static final String INACTIVE_MENU_STYLE = "-fx-background-color: transparent; -fx-text-fill: #bdc3c7; "
            + "-fx-cursor: hand; -fx-alignment: CENTER_LEFT;";

    @FXML
    public void initialize() {
        String firstname = AuctionManager.getInstance().getFirstname();
        String username = AuctionManager.getInstance().getUsername();

        String display = (firstname != null && !firstname.trim().isEmpty()) ? firstname : username;
        lblWelcome.setText("Xin chào, " + display + "!");

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colEndTime.setCellValueFactory(new PropertyValueFactory<>("endTime"));

        if (rootPane != null && sidebar != null) {
            sidebar.prefWidthProperty().bind(rootPane.widthProperty().multiply(0.22));
        }

        showHomeView();
    }

    @FXML
    public void onHomeButtonClicked() {
        showHomeView();
    }

    @FXML
    public void onProductListButtonClicked() {
        showProductListView();
    }

    private void showHomeView() {
        switchContent(homeView, productListView);
        setActiveMenu(btnHome, btnProductList);
    }

    private void showProductListView() {
        switchContent(productListView, homeView);
        setActiveMenu(btnProductList, btnHome);
    }

    private void switchContent(VBox visibleView, VBox hiddenView) {
        if (visibleView != null) {
            visibleView.setVisible(true);
            visibleView.setManaged(true);
        }
        if (hiddenView != null) {
            hiddenView.setVisible(false);
            hiddenView.setManaged(false);
        }
    }

    private void setActiveMenu(Button activeButton, Button inactiveButton) {
        if (activeButton != null) {
            activeButton.setStyle(ACTIVE_MENU_STYLE);
        }
        if (inactiveButton != null) {
            inactiveButton.setStyle(INACTIVE_MENU_STYLE);
        }
    }

    @FXML
    public void onLogoutButtonClicked() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText("Xác nhận đăng xuất");
        alert.setContentText("Bạn có chắc chắn muốn đăng xuất khỏi hệ thống?");

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
        Role currentRole = AuctionManager.getInstance().getRole();

        if (currentRole == Role.SELLER) {
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
