package io.auctionsystem.client.controller;

import io.auctionsystem.client.model.TransactionModel; // Đã cập nhật import model mới của bạn
import io.auctionsystem.client.pattern.AuctionManager;
import io.auctionsystem.client.pattern.SceneManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public class WalletController {

    @FXML private Label lblBalance;
    @FXML private Label lblTransactionError;
    @FXML private HBox bankErrorBox;
    @FXML private Button btnSetupBank;
    @FXML private TextField txtAmount;
    @FXML private TextField txtNote;

    // Các thành phần bộ lọc tìm kiếm
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbTimeFilter;
    @FXML private Button btnFilterIn;
    @FXML private Button btnFilterOut;

    // Cập nhật TableView và TableColumn theo TransactionModel
    @FXML private TableView<TransactionModel> tableWalletHistory;
    @FXML private TableColumn<TransactionModel, String> colTransactionTime;
    @FXML private TableColumn<TransactionModel, String> colTransactionType;
    @FXML private TableColumn<TransactionModel, String> colMoneyIn;
    @FXML private TableColumn<TransactionModel, String> colMoneyOut;
    @FXML private TableColumn<TransactionModel, String> colLastBalance;
    @FXML private TableColumn<TransactionModel, String> colTransactionNote;

    private static final ObservableList<TransactionModel> WALLET_TRANSACTIONS = FXCollections.observableArrayList();

    // Sử dụng FilteredList bao bọc danh sách gốc phục vụ bài toán lọc dữ liệu
    private FilteredList<TransactionModel> filteredTransactions;

    private static final NumberFormat VND_FORMAT = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String SETTINGS_BANK_TAB = "BANK";

    // Biến trạng thái dòng tiền: 0 = Tất cả, 1 = Chỉ tiền vào, 2 = Chỉ tiền ra
    private int flowFilterState = 0;

    @FXML
    public void initialize() {
        colTransactionTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        colTransactionType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colMoneyIn.setCellValueFactory(new PropertyValueFactory<>("moneyIn"));
        colMoneyOut.setCellValueFactory(new PropertyValueFactory<>("moneyOut"));
        colLastBalance.setCellValueFactory(new PropertyValueFactory<>("lastBalance"));
        colTransactionNote.setCellValueFactory(new PropertyValueFactory<>("note"));

        // Khởi tạo FilteredList dựa trên danh sách dữ liệu gốc
        filteredTransactions = new FilteredList<>(WALLET_TRANSACTIONS, p -> true);
        tableWalletHistory.setItems(filteredTransactions);

        // Khởi tạo danh mục lựa chọn cho hộp phân loại thời gian
        cbTimeFilter.setItems(FXCollections.observableArrayList("Tất cả thời gian", "Hôm nay", "Tuần này", "Tháng này"));
        cbTimeFilter.setValue("Tất cả thời gian");

        // Lắng nghe sự thay đổi của ô tìm kiếm và ComboBox để tự động kích hoạt lọc dữ liệu
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        cbTimeFilter.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());

        refreshWalletBalance();

        if (!AuctionManager.getInstance().hasBankInfo()) {
            showWalletError("Bạn chưa có thông tin tài khoản ngân hàng.", true);
        } else {
            hideWalletError();
        }
    }

    // Tổ hợp xử lý đồng thời cả 3 bộ lọc: Tìm kiếm từ khóa, Khoảng thời gian và Hướng dòng tiền
    private void applyFilters() {
        String searchText = txtSearch.getText() == null ? "" : txtSearch.getText().toLowerCase().trim();
        String timeSelection = cbTimeFilter.getValue() == null ? "Tất cả thời gian" : cbTimeFilter.getValue();
        LocalDateTime now = LocalDateTime.now();

        filteredTransactions.setPredicate(transaction -> {
            // 1. Kiểm tra từ khóa tìm kiếm (Lọc theo Ghi chú)
            if (!searchText.isEmpty() && !transaction.getNote().toLowerCase().contains(searchText)) {
                return false;
            }

            // 2. Kiểm tra phân loại thời gian
            LocalDateTime txTime = transaction.getRawTime();
            if (timeSelection.equals("Hôm nay")) {
                if (!txTime.toLocalDate().equals(LocalDate.now())) return false;
            } else if (timeSelection.equals("Tuần này")) {
                if (ChronoUnit.DAYS.between(txTime, now) > 7) return false;
            } else if (timeSelection.equals("Tháng này")) {
                if (txTime.getMonth() != now.getMonth() || txTime.getYear() != now.getYear()) return false;
            }

            // 3. Kiểm tra phân loại hướng dòng tiền (Tiền vào / Tiền ra)
            if (flowFilterState == 1 && !"Nạp".equals(transaction.getType())) {
                return false; // Đang chọn nút Tiền vào thì chỉ giữ lại các dòng loại "Nạp"
            }
            if (flowFilterState == 2 && !"Rút".equals(transaction.getType())) {
                return false; // Đang chọn nút Tiền ra thì chỉ giữ lại các dòng loại "Rút"
            }

            return true;
        });
    }

    @FXML
    public void onFilterInClicked() {
        if (flowFilterState == 1) {
            flowFilterState = 0; // Nếu đang bật mà click lại thì tắt lọc dòng tiền
            btnFilterIn.setStyle("-fx-background-color: transparent; -fx-border-color: #bdc3c7; -fx-text-fill: #7f8c8d; -fx-font-weight: bold;");
        } else {
            flowFilterState = 1; // Kích hoạt chỉ xem tiền vào
            btnFilterIn.setStyle("-fx-background-color: #2ecc71; -fx-border-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold;");
            btnFilterOut.setStyle("-fx-background-color: transparent; -fx-border-color: #bdc3c7; -fx-text-fill: #7f8c8d; -fx-font-weight: bold;");
        }
        applyFilters();
    }

    @FXML
    public void onFilterOutClicked() {
        if (flowFilterState == 2) {
            flowFilterState = 0; // Nếu đang bật mà click lại thì tắt lọc dòng tiền
            btnFilterOut.setStyle("-fx-background-color: transparent; -fx-border-color: #bdc3c7; -fx-text-fill: #7f8c8d; -fx-font-weight: bold;");
        } else {
            flowFilterState = 2; // Kích hoạt chỉ xem tiền ra
            btnFilterOut.setStyle("-fx-background-color: #e67e22; -fx-border-color: #e67e22; -fx-text-fill: white; -fx-font-weight: bold;");
            btnFilterIn.setStyle("-fx-background-color: transparent; -fx-border-color: #bdc3c7; -fx-text-fill: #7f8c8d; -fx-font-weight: bold;");
        }
        applyFilters();
    }

    @FXML
    public void onClearFilterClicked() {
        txtSearch.clear();
        cbTimeFilter.setValue("Tất cả thời gian");
        flowFilterState = 0;
        btnFilterIn.setStyle("-fx-background-color: transparent; -fx-border-color: #bdc3c7; -fx-text-fill: #7f8c8d; -fx-font-weight: bold;");
        btnFilterOut.setStyle("-fx-background-color: transparent; -fx-border-color: #bdc3c7; -fx-text-fill: #7f8c8d; -fx-font-weight: bold;");
        applyFilters();
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
            showWalletError("Bạn chưa có thông tin tài khoản ngân hàng.", true);
            return;
        }

        double amount = parseWalletAmount();
        if (amount <= 0) {
            showWalletError("Vui lòng nhập số tiền hợp lệ.", false);
            return;
        }

        double currentBalance = AuctionManager.getInstance().getBalance();
        if (!isDeposit && amount > currentBalance) {
            showWalletError("Số dư không đủ để rút tiền.", false);
            return;
        }

        double newBalance = isDeposit ? currentBalance + amount : currentBalance - amount;
        AuctionManager.getInstance().setBalance(newBalance);
        refreshWalletBalance();

        String formattedAmount = formatCurrency(amount);
        String moneyInVal = isDeposit ? "+ " + formattedAmount : "";
        String moneyOutVal = !isDeposit ? "- " + formattedAmount : "";
        String strLastBalance = formatCurrency(newBalance);

        String note = txtNote.getText().trim();
        if (note.isEmpty()) {
            note = isDeposit ? "Nạp tiền vào ví" : "Rút tiền về tài khoản ngân hàng";
        }

        LocalDateTime timeNow = LocalDateTime.now();
        // Tạo instance trực tiếp bằng TransactionModel mới tách lớp
        WALLET_TRANSACTIONS.addFirst(new TransactionModel(
                timeNow,
                timeNow.format(TIME_FORMAT),
                moneyInVal,
                moneyOutVal,
                strLastBalance,
                type,
                note
        ));

        txtAmount.clear();
        txtNote.clear();
        hideWalletError();
    }

    private double parseWalletAmount() {
        try {
            return Double.parseDouble(txtAmount.getText().trim().replace(".", "").replace(",", ""));
        } catch (Exception e) {
            return -1;
        }
    }

    private void refreshWalletBalance() {
        lblBalance.setText(formatCurrency(AuctionManager.getInstance().getBalance()));
    }

    private String formatCurrency(double amount) {
        return VND_FORMAT.format(amount);
    }

    private void showWalletError(String message, boolean isBankError) {
        lblTransactionError.setText(message);
        btnSetupBank.setVisible(isBankError);
        btnSetupBank.setManaged(isBankError);
        bankErrorBox.setVisible(true);
        bankErrorBox.setManaged(true);
    }

    private void hideWalletError() {
        bankErrorBox.setVisible(false);
        bankErrorBox.setManaged(false);
    }
}