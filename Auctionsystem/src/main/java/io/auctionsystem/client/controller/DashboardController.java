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
    @FXML private Label lblWelcome;

    @FXML private TableView<AuctionItemDTO> tableItems;
    @FXML private TableColumn<AuctionItemDTO, Long> colId;
    @FXML private TableColumn<AuctionItemDTO, String> colName;
    @FXML private TableColumn<AuctionItemDTO, Double> colCurrentPrice;
    @FXML private TableColumn<AuctionItemDTO, String> colEndTime;
    @FXML private TableColumn<AuctionItemDTO, String> colStatus;
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