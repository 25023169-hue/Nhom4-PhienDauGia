package io.auctionsystem.client.controller;

import io.auctionsystem.client.pattern.AuctionManager;
import io.auctionsystem.client.pattern.SceneManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class WalletController {

    @FXML private Label lblWalletBalance;
    @FXML private Label lblWalletError;
    @FXML private HBox walletBankErrorBox;
    @FXML private TextField txtWalletAmount;
    @FXML private TextField txtWalletNote;
    @FXML private TableView<WalletTransaction> tableWalletHistory;
    @FXML private TableColumn<WalletTransaction, String> colWalletTime;
    @FXML private TableColumn<WalletTransaction, String> colWalletAmount;
    @FXML private TableColumn<WalletTransaction, String> colLastBalance;
    @FXML private TableColumn<WalletTransaction, String> colWalletType;
    @FXML private TableColumn<WalletTransaction, String> colWalletNote;

    private static final ObservableList<WalletTransaction> WALLET_TRANSACTIONS = FXCollections.observableArrayList();
    private static final NumberFormat VND_FORMAT = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String SETTINGS_BANK_TAB = "BANK";

    @FXML
    public void initialize() {
        colWalletTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        colWalletAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colLastBalance.setCellValueFactory(new PropertyValueFactory<>("lastBalance"));
        colWalletType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colWalletNote.setCellValueFactory(new PropertyValueFactory<>("note"));
        tableWalletHistory.setItems(WALLET_TRANSACTIONS);

        refreshWalletBalance();
        if (!AuctionManager.getInstance().hasBankInfo()) {
            showWalletError("Bạn chưa có thông tin tài khoản ngân hàng.");
        } else {
            hideWalletError();
        }
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
        String strLastBalance = formatCurrency(newBalance);
        String note = txtWalletNote.getText().trim();
        if (note.isEmpty()) {
            note = isDeposit ? "Nạp tiền vào ví" : "Rút tiền về tài khoản ngân hàng";
        }

        WALLET_TRANSACTIONS.add(0, new WalletTransaction(
                LocalDateTime.now().format(TIME_FORMAT),
                signedAmount,
                strLastBalance,
                type,
                note
        ));

        txtWalletAmount.clear();
        txtWalletNote.clear();
        hideWalletError();
    }

    private double parseWalletAmount() {
        try {
            return Double.parseDouble(txtWalletAmount.getText().trim().replace(".", "").replace(",", ""));
        } catch (Exception e) {
            return -1;
        }
    }

    private void refreshWalletBalance() {
        lblWalletBalance.setText(formatCurrency(AuctionManager.getInstance().getBalance()));
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

    public static class WalletTransaction {
        private final String time;
        private final String amount;
        private final String lastBalance;
        private final String type;
        private final String note;

        public WalletTransaction(String time, String amount, String lastBalance, String type, String note) {
            this.time = time;
            this.amount = amount;
            this.lastBalance = lastBalance;
            this.type = type;
            this.note = note;
        }

        public String getTime() { return time; }
        public String getAmount() { return amount; }
        public String getLastBalance() { return lastBalance; }
        public String getType() { return type; }
        public String getNote() { return note; }
    }
}