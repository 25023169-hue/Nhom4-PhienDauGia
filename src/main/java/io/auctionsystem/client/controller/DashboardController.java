package io.auctionsystem.client.controller;

import io.auctionsystem.client.pattern.AuctionManager;
import io.auctionsystem.client.pattern.SceneManager;
import io.auctionsystem.common.dto.AuctionItemDTO;
import io.auctionsystem.common.enums.Role;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DashboardController {

    @FXML private BorderPane rootPane;
    @FXML private VBox sidebar;
    @FXML private VBox homeView;
    @FXML private VBox productListView;
    @FXML private VBox walletView;
    @FXML private Button btnHome;
    @FXML private Button btnProductList;
    @FXML private Button btnWallet;
    @FXML private Label lblWelcome;

    @FXML private TableView<AuctionItemDTO> tableItems;
    @FXML private TableColumn<AuctionItemDTO, Long> colId;
    @FXML private TableColumn<AuctionItemDTO, String> colName;
    @FXML private TableColumn<AuctionItemDTO, Double> colCurrentPrice;
    @FXML private TableColumn<AuctionItemDTO, String> colEndTime;
    @FXML private TableColumn<AuctionItemDTO, String> colStatus;

    @FXML private Label lblWalletBalance;
    @FXML private Label lblWalletError;
    @FXML private HBox walletBankErrorBox;
    @FXML private TextField txtWalletAmount;
    @FXML private TextField txtWalletNote;
    @FXML private TableView<WalletTransaction> tableWalletHistory;
    @FXML private TableColumn<WalletTransaction, String> colWalletTime;
    @FXML private TableColumn<WalletTransaction, String> colWalletAmount;
    @FXML private TableColumn<WalletTransaction, String> colWalletType;
    @FXML private TableColumn<WalletTransaction, String> colWalletNote;

    private static final ObservableList<WalletTransaction> WALLET_TRANSACTIONS = FXCollections.observableArrayList();
    private static final NumberFormat VND_FORMAT = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String SETTINGS_BANK_TAB = "BANK";
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

        colWalletTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        colWalletAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colWalletType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colWalletNote.setCellValueFactory(new PropertyValueFactory<>("note"));
        tableWalletHistory.setItems(WALLET_TRANSACTIONS);

        if (rootPane != null && sidebar != null) {
            sidebar.prefWidthProperty().bind(rootPane.widthProperty().multiply(0.22));
        }

        refreshWalletBalance();
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

    @FXML
    public void onWalletButtonClicked() {
        showWalletView();
    }

    @FXML
    public void onDepositButtonClicked() {
        handleWalletTransaction("Nạp", true);
    }

    @FXML
    public void onWithdrawButtonClicked() {
        handleWalletTransaction("Rút", false);
    }

    @FXML
    public void onOpenBankSettings() {
        AuctionManager.getInstance().requestSettingsTab(SETTINGS_BANK_TAB);
        SceneManager.getInstance().switchScene("/client/fxml/settings.fxml");
    }

    private void showHomeView() {
        switchContent(homeView, productListView, walletView);
        setActiveMenu(btnHome, btnProductList, btnWallet);
    }

    private void showProductListView() {
        switchContent(productListView, homeView, walletView);
        setActiveMenu(btnProductList, btnHome, btnWallet);
    }

    private void showWalletView() {
        switchContent(walletView, homeView, productListView);
        setActiveMenu(btnWallet, btnHome, btnProductList);
        refreshWalletBalance();
        if (!AuctionManager.getInstance().hasBankInfo()) {
            showWalletError("Bạn chưa có thông tin tài khoản ngân hàng.");
        } else {
            hideWalletError();
        }
    }

    private void switchContent(VBox visibleView, VBox... hiddenViews) {
        if (visibleView != null) {
            visibleView.setVisible(true);
            visibleView.setManaged(true);
        }
        for (VBox hiddenView : hiddenViews) {
            if (hiddenView != null) {
                hiddenView.setVisible(false);
                hiddenView.setManaged(false);
            }
        }
    }

    private void setActiveMenu(Button activeButton, Button... inactiveButtons) {
        if (activeButton != null) {
            activeButton.setStyle(ACTIVE_MENU_STYLE);
        }
        for (Button inactiveButton : inactiveButtons) {
            if (inactiveButton != null) {
                inactiveButton.setStyle(INACTIVE_MENU_STYLE);
            }
        }
    }

    private void handleWalletTransaction(String type, boolean isDeposit) {
        if (!AuctionManager.getInstance().hasBankInfo()) {
            showWalletError("Bạn chưa có thông tin tài khoản ngân hàng.");
            return;
        }

        double amount = parseWalletAmount();
        if (amount <= 0) {
            showWalletError("Vui lòng nhập số tiền hợp lệ.");
            return;
        }

        double currentBalance = AuctionManager.getInstance().getBalance();
        if (!isDeposit && amount > currentBalance) {
            showWalletError("Số dư không đủ để rút tiền.");
            return;
        }

        double newBalance = isDeposit ? currentBalance + amount : currentBalance - amount;
        AuctionManager.getInstance().setBalance(newBalance);
        refreshWalletBalance();

        String signedAmount = (isDeposit ? "+ " : "- ") + formatCurrency(amount);
        String note = txtWalletNote.getText().trim();
        if (note.isEmpty()) {
            note = isDeposit ? "Nạp tiền vào ví" : "Rút tiền về tài khoản ngân hàng";
        }
        WALLET_TRANSACTIONS.add(0, new WalletTransaction(
                LocalDateTime.now().format(TIME_FORMAT),
                signedAmount,
                type,
                note
        ));

        txtWalletAmount.clear();
        txtWalletNote.clear();
        hideWalletError();
    }

    private double parseWalletAmount() {
        try {
            String normalized = txtWalletAmount.getText()
                    .trim()
                    .replace(".", "")
                    .replace(",", "");
            return Double.parseDouble(normalized);
        } catch (Exception e) {
            return -1;
        }
    }

    private void refreshWalletBalance() {
        if (lblWalletBalance != null) {
            lblWalletBalance.setText(formatCurrency(AuctionManager.getInstance().getBalance()));
        }
    }

    private String formatCurrency(double amount) {
        return VND_FORMAT.format(amount);
    }

    private void showWalletError(String message) {
        lblWalletError.setText(message);
        walletBankErrorBox.setVisible(true);
        walletBankErrorBox.setManaged(true);
    }

    private void hideWalletError() {
        walletBankErrorBox.setVisible(false);
        walletBankErrorBox.setManaged(false);
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
                WALLET_TRANSACTIONS.clear();
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

    public static class WalletTransaction {
        private final String time;
        private final String amount;
        private final String type;
        private final String note;

        public WalletTransaction(String time, String amount, String type, String note) {
            this.time = time;
            this.amount = amount;
            this.type = type;
            this.note = note;
        }

        public String getTime() {
            return time;
        }

        public String getAmount() {
            return amount;
        }

        public String getType() {
            return type;
        }

        public String getNote() {
            return note;
        }
    }
}
