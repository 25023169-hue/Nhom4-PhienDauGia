package io.auctionsystem.client.controller;

import io.auctionsystem.client.model.TransactionModel;
import io.auctionsystem.client.pattern.AuctionManager;
import io.auctionsystem.client.pattern.SceneManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.type.TypeReference;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WalletController {

    @FXML private Label lblBalance;
    @FXML private Label lblTotalBalance;
    @FXML private Label lblHeldBalance;
    @FXML private Label lblTransactionError;
    @FXML private HBox bankErrorBox;
    @FXML private Button btnSetupBank;
    @FXML private TextField txtAmount;
    @FXML private TextField txtNote;

    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbTimeFilter;
    @FXML private Button btnFilterIn;
    @FXML private Button btnFilterOut;

    @FXML private TableView<TransactionModel> tableWalletHistory;
    @FXML private TableColumn<TransactionModel, String> colTransactionTime;
    @FXML private TableColumn<TransactionModel, String> colTransactionType;
    @FXML private TableColumn<TransactionModel, String> colMoneyIn;
    @FXML private TableColumn<TransactionModel, String> colMoneyOut;
    @FXML private TableColumn<TransactionModel, String> colLastBalance;
    @FXML private TableColumn<TransactionModel, String> colTransactionNote;

    private static final ObservableList<TransactionModel> WALLET_TRANSACTIONS = FXCollections.observableArrayList();
    private FilteredList<TransactionModel> filteredTransactions;

    private static final NumberFormat VND_FORMAT = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String SETTINGS_BANK_TAB = "BANK";

    private int flowFilterState = 0;

    @FXML
    public void initialize() {
        colTransactionTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        colTransactionType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colMoneyIn.setCellValueFactory(new PropertyValueFactory<>("moneyIn"));
        colMoneyOut.setCellValueFactory(new PropertyValueFactory<>("moneyOut"));
        colLastBalance.setCellValueFactory(new PropertyValueFactory<>("lastBalance"));
        colTransactionNote.setCellValueFactory(new PropertyValueFactory<>("note"));

        filteredTransactions = new FilteredList<>(WALLET_TRANSACTIONS, p -> true);
        tableWalletHistory.setItems(filteredTransactions);

        cbTimeFilter.setItems(FXCollections.observableArrayList("Tất cả thời gian", "Hôm nay", "Tuần này", "Tháng này"));
        cbTimeFilter.setValue("Tất cả thời gian");

        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        cbTimeFilter.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());

        refreshWalletBalance();

        // LỖI ĐÃ SỬA: loadTransactionHistoryFromServer() trước đây gọi HTTP blocking
        // trực tiếp trên JavaFX UI Thread → UI bị đóng băng khi chờ server.
        // Đã chuyển toàn bộ logic HTTP vào new Thread() bên trong hàm đó.
        loadTransactionHistoryFromServer();

        if (!AuctionManager.getInstance().hasBankInfo()) {
            showWalletError("Bạn chưa có thông tin tài khoản ngân hàng.", true);
        } else {
            hideWalletError();
        }
    }

    // LỖI ĐÃ SỬA: Bọc toàn bộ HTTP call trong new Thread() để không block UI Thread
    private void loadTransactionHistoryFromServer() {
        Long userId = AuctionManager.getInstance().getId();
        if (userId == null) return;

        new Thread(() -> {
            try {
                String urlString = "http://localhost:8080/api/user/" + userId + "/transactions";

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(urlString))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                // Cập nhật UI phải chạy trên JavaFX Thread
                Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        try {
                            WALLET_TRANSACTIONS.clear();
                            ObjectMapper mapper = new ObjectMapper();

                            List<Map<String, Object>> transactions = mapper.readValue(
                                    response.body(),
                                    new TypeReference<List<Map<String, Object>>>() {}
                            );

                            for (Map<String, Object> tx : transactions) {
                                String type = (String) tx.get("type");
                                double moneyIn = ((Number) tx.getOrDefault("moneyIn", 0.0)).doubleValue();
                                double moneyOut = ((Number) tx.getOrDefault("moneyOut", 0.0)).doubleValue();
                                double lastBalance = ((Number) tx.getOrDefault("lastBalance", 0.0)).doubleValue();
                                String note = (String) tx.get("note");
                                String timeStr = (String) tx.get("time");

                                String formattedAmount = formatCurrency(moneyIn > 0 ? moneyIn : moneyOut);
                                String moneyInVal = moneyIn > 0 ? "+ " + formattedAmount : "";
                                String moneyOutVal = moneyOut > 0 ? "- " + formattedAmount : "";
                                String strLastBalance = formatCurrency(lastBalance);

                                WALLET_TRANSACTIONS.add(new TransactionModel(
                                        LocalDateTime.now(),
                                        timeStr,
                                        moneyInVal,
                                        moneyOutVal,
                                        strLastBalance,
                                        type,
                                        note
                                ));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void applyFilters() {
        String searchText = txtSearch.getText() == null ? "" : txtSearch.getText().toLowerCase().trim();
        String timeSelection = cbTimeFilter.getValue() == null ? "Tất cả thời gian" : cbTimeFilter.getValue();
        LocalDateTime now = LocalDateTime.now();

        filteredTransactions.setPredicate(transaction -> {

            if (!searchText.isEmpty()) {
                String note = transaction.getNote() == null ? "" : transaction.getNote().toLowerCase().trim();
                String moneyIn = transaction.getMoneyIn() == null ? "" : transaction.getMoneyIn().toLowerCase().trim();
                String moneyOut = transaction.getMoneyOut() == null ? "" : transaction.getMoneyOut().toLowerCase().trim();

                String rawSearch = searchText.replaceAll("[^0-9]", "");
                String rawMoneyIn = moneyIn.replaceAll("[^0-9]", "");
                String rawMoneyOut = moneyOut.replaceAll("[^0-9]", "");

                boolean matchNote = note.contains(searchText);
                boolean matchMoneyIn = moneyIn.contains(searchText) || (!rawSearch.isEmpty() && rawMoneyIn.contains(rawSearch));
                boolean matchMoneyOut = moneyOut.contains(searchText) || (!rawSearch.isEmpty() && rawMoneyOut.contains(rawSearch));

                if (!matchNote && !matchMoneyIn && !matchMoneyOut) {
                    return false;
                }
            }

            LocalDateTime txTime = transaction.getRawTime();
            if (timeSelection.equals("Hôm nay")) {
                if (!txTime.toLocalDate().equals(LocalDate.now())) return false;
            } else if (timeSelection.equals("Tuần này")) {
                if (ChronoUnit.DAYS.between(txTime, now) > 7) return false;
            } else if (timeSelection.equals("Tháng này")) {
                if (txTime.getMonth() != now.getMonth() || txTime.getYear() != now.getYear()) return false;
            }

            if (flowFilterState == 1 && !"Nạp".equals(transaction.getType())) return false;
            if (flowFilterState == 2 && !"Rút".equals(transaction.getType())) return false;

            return true;
        });
    }

    @FXML
    public void onFilterInClicked() {
        if (flowFilterState == 1) {
            flowFilterState = 0;
            btnFilterIn.setStyle("-fx-background-color: transparent; -fx-border-color: #bdc3c7; -fx-text-fill: #7f8c8d; -fx-font-weight: bold;");
        } else {
            flowFilterState = 1;
            btnFilterIn.setStyle("-fx-background-color: #2ecc71; -fx-border-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold;");
            btnFilterOut.setStyle("-fx-background-color: transparent; -fx-border-color: #bdc3c7; -fx-text-fill: #7f8c8d; -fx-font-weight: bold;");
        }
        applyFilters();
    }

    @FXML
    public void onFilterOutClicked() {
        if (flowFilterState == 2) {
            flowFilterState = 0;
            btnFilterOut.setStyle("-fx-background-color: transparent; -fx-border-color: #bdc3c7; -fx-text-fill: #7f8c8d; -fx-font-weight: bold;");
        } else {
            flowFilterState = 2;
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

    @FXML public void onDepositButtonClicked() { handleWalletTransaction("Nạp", true); }
    @FXML public void onWithdrawButtonClicked() { handleWalletTransaction("Rút", false); }

    @FXML
    public void onOpenBankSettings() {
        AuctionManager.getInstance().requestSettingsTab(SETTINGS_BANK_TAB);
        SceneManager.getInstance().switchScene("/client/fxml/settings/settings.fxml");
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

        String note = txtNote.getText().trim();
        if (note.isEmpty()) {
            note = isDeposit ? "Nạp tiền vào ví" : "Rút tiền về tài khoản ngân hàng";
        }

        try {
            Long userId = AuctionManager.getInstance().getId();
            String urlString = "http://localhost:8080/api/user/" + userId + "/transaction"
                    + "?amount=" + amount
                    + "&type=" + type
                    + "&currentBalance=" + currentBalance
                    + "&note=" + URLEncoder.encode(note, StandardCharsets.UTF_8);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlString))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode transaction = new ObjectMapper().readTree(response.body());
                double newBalance = transaction.path("lastBalance").asDouble(currentBalance);
                AuctionManager.getInstance().setBalance(newBalance);
                updateDisplayedBalance(newBalance, 0.0, newBalance);
                refreshWalletBalance();

                String formattedAmount = formatCurrency(amount);
                String moneyInVal = isDeposit ? "+ " + formattedAmount : "";
                String moneyOutVal = !isDeposit ? "- " + formattedAmount : "";
                String strLastBalance = formatCurrency(newBalance);
                LocalDateTime timeNow = LocalDateTime.now();

                WALLET_TRANSACTIONS.add(0, new TransactionModel(
                        timeNow, timeNow.format(TIME_FORMAT), moneyInVal, moneyOutVal, strLastBalance, type, note
                ));

                txtAmount.clear();
                txtNote.clear();
                hideWalletError();
            } else {
                showWalletError("Lỗi từ máy chủ: " + response.body(), false);
            }
        } catch (Exception e) {
            showWalletError("Lỗi kết nối mạng: " + e.getMessage(), false);
            e.printStackTrace();
        }
    }

    private double parseWalletAmount() {
        try {
            return Double.parseDouble(txtAmount.getText().trim().replace(".", "").replace(",", ""));
        } catch (Exception e) {
            return -1;
        }
    }

    private void refreshWalletBalance() {
        Long userId = AuctionManager.getInstance().getId();
        if (userId == null) {
            updateDisplayedBalance(0.0, 0.0, 0.0);
            return;
        }

        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/user/" + userId + "/wallet"))
                        .GET()
                        .build();
                HttpResponse<String> response = HttpClient.newHttpClient()
                        .send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonNode summary = new ObjectMapper().readTree(response.body());
                    double totalBalance = summary.path("totalBalance").asDouble();
                    double heldBalance = summary.path("heldBalance").asDouble();
                    double availableBalance = summary.path("availableBalance").asDouble();
                    Platform.runLater(() -> {
                        AuctionManager.getInstance().setBalance(availableBalance);
                        updateDisplayedBalance(totalBalance, heldBalance, availableBalance);
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> updateDisplayedBalance(
                        AuctionManager.getInstance().getBalance(),
                        0.0,
                        AuctionManager.getInstance().getBalance()
                ));
            }
        }).start();
    }

    private void updateDisplayedBalance(double totalBalance, double heldBalance, double availableBalance) {
        lblBalance.setText(formatCurrency(availableBalance));
        lblTotalBalance.setText("Tổng số dư: " + formatCurrency(totalBalance));
        lblHeldBalance.setText("Đang giữ đấu giá: " + formatCurrency(heldBalance));
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
