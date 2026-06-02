package client.controller;

import server.exception.ServerConnectionException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import client.pattern.AuctionManager;
import client.pattern.ClientHttp;
import client.pattern.SceneManager;
import client.TransactionViewModel;
import common.Constants;
import common.enums.TransactionType;
import java.net.URI;
import java.net.URLEncoder;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

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

  @FXML private TableView<TransactionViewModel> tableWalletHistory;
  @FXML private TableColumn<TransactionViewModel, Long> colTransactionId;
  @FXML private TableColumn<TransactionViewModel, String> colTransactionTime;
  @FXML private TableColumn<TransactionViewModel, String> colTransactionType;
  @FXML private TableColumn<TransactionViewModel, String> colMoneyIn;
  @FXML private TableColumn<TransactionViewModel, String> colMoneyOut;
  @FXML private TableColumn<TransactionViewModel, String> colLastBalance;
  @FXML private TableColumn<TransactionViewModel, String> colTransactionNote;

  private static final ObservableList<TransactionViewModel> WALLET_TRANSACTIONS =
      FXCollections.observableArrayList();
  private FilteredList<TransactionViewModel> filteredTransactions;

  private static final NumberFormat VND_FORMAT =
      NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));
  private static final DateTimeFormatter TIME_FORMAT =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
  private static final String SETTINGS_BANK_TAB = "BANK";

  private static final ObjectMapper OBJECT_MAPPER = ClientHttp.mapper();

  private int flowFilterState = 0;

  @FXML
  public void initialize() {
    colTransactionId.setCellValueFactory(new PropertyValueFactory<>("id"));
    colTransactionTime.setCellValueFactory(new PropertyValueFactory<>("time"));
    colTransactionType.setCellValueFactory(new PropertyValueFactory<>("type"));
    colMoneyIn.setCellValueFactory(new PropertyValueFactory<>("moneyIn"));
    colMoneyOut.setCellValueFactory(new PropertyValueFactory<>("moneyOut"));
    colLastBalance.setCellValueFactory(new PropertyValueFactory<>("lastBalance"));
    colTransactionNote.setCellValueFactory(new PropertyValueFactory<>("note"));

    filteredTransactions = new FilteredList<>(WALLET_TRANSACTIONS, p -> true);
    tableWalletHistory.setItems(filteredTransactions);

    cbTimeFilter.setItems(
        FXCollections.observableArrayList("Tất cả thời gian", "Hôm nay", "Tuần này", "Tháng này"));
    cbTimeFilter.setValue("Tất cả thời gian");

    txtSearch.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
    cbTimeFilter.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());

    refreshWalletBalance();

    loadTransactionHistoryFromServer();

    if (!AuctionManager.getInstance().hasBankInfo()) {
      showWalletError("Bạn chưa có thông tin tài khoản ngân hàng.", true);
    } else {
      hideWalletError();
    }
  }

  private void loadTransactionHistoryFromServer() {
    Long userId = AuctionManager.getInstance().getId();
    if (userId == null) return;

    new Thread(
            () -> {
              try {
                String urlString = Constants.BASE_URL + "/user/" + userId + "/transactions";

                HttpRequest request =
                    HttpRequest.newBuilder().uri(URI.create(urlString)).GET().build();

                HttpResponse<String> response =
                    ClientHttp.send(request);

                Platform.runLater(
                    () -> {
                      if (response.statusCode() == 200) {
                        try {
                          WALLET_TRANSACTIONS.clear();
                          List<Map<String, Object>> transactions =
                              OBJECT_MAPPER.readValue(
                                  response.body(),
                                  new TypeReference<List<Map<String, Object>>>() {});

                          for (Map<String, Object> tx : transactions) {
                            Long id = ((Number) tx.get("id")).longValue();
                            String type = (String) tx.get("type");
                            double moneyIn =
                                ((Number) tx.getOrDefault("moneyIn", 0.0)).doubleValue();
                            double moneyOut =
                                ((Number) tx.getOrDefault("moneyOut", 0.0)).doubleValue();
                            double lastBalance =
                                ((Number) tx.getOrDefault("lastBalance", 0.0)).doubleValue();
                            String note = (String) tx.get("note");
                            String timeStr = (String) tx.get("time");

                            String formattedAmount =
                                formatCurrency(moneyIn > 0 ? moneyIn : moneyOut);
                            String moneyInVal = moneyIn > 0 ? "+ " + formattedAmount : "";
                            String moneyOutVal = moneyOut > 0 ? "- " + formattedAmount : "";
                            String strLastBalance = formatCurrency(lastBalance);

                            WALLET_TRANSACTIONS.add(
                                new TransactionViewModel(
                                    id,
                                    parseTransactionTime(timeStr),
                                    timeStr,
                                    moneyInVal,
                                    moneyOutVal,
                                    strLastBalance,
                                    type,
                                    note));
                          }
                        } catch (Exception e) {
                          e.printStackTrace();
                        }
                      }
                    });

              } catch (Exception e) {
                e.printStackTrace();
              }
            })
        .start();
  }

  private void applyFilters() {
    String searchText = txtSearch.getText() == null ? "" : txtSearch.getText().toLowerCase().trim();
    String timeSelection =
        cbTimeFilter.getValue() == null ? "Tất cả thời gian" : cbTimeFilter.getValue();
    LocalDateTime now = LocalDateTime.now();

    filteredTransactions.setPredicate(
        transaction -> {
          if (!searchText.isEmpty()) {
            String note =
                transaction.getNote() == null ? "" : transaction.getNote().toLowerCase().trim();
            String moneyIn =
                transaction.getMoneyIn() == null
                    ? ""
                    : transaction.getMoneyIn().toLowerCase().trim();
            String moneyOut =
                transaction.getMoneyOut() == null
                    ? ""
                    : transaction.getMoneyOut().toLowerCase().trim();

            String rawSearch = searchText.replaceAll("[^0-9]", "");
            String rawMoneyIn = moneyIn.replaceAll("[^0-9]", "");
            String rawMoneyOut = moneyOut.replaceAll("[^0-9]", "");

            boolean matchNote = note.contains(searchText);
            boolean matchMoneyIn =
                moneyIn.contains(searchText)
                    || (!rawSearch.isEmpty() && rawMoneyIn.contains(rawSearch));
            boolean matchMoneyOut =
                moneyOut.contains(searchText)
                    || (!rawSearch.isEmpty() && rawMoneyOut.contains(rawSearch));

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
            if (txTime.getMonth() != now.getMonth() || txTime.getYear() != now.getYear())
              return false;
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
      btnFilterIn.setStyle(
          "-fx-background-color: transparent; -fx-border-color: #bdc3c7; -fx-text-fill: #7f8c8d; -fx-font-weight: bold;");
    } else {
      flowFilterState = 1;
      btnFilterIn.setStyle(
          "-fx-background-color: #2ecc71; -fx-border-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold;");
      btnFilterOut.setStyle(
          "-fx-background-color: transparent; -fx-border-color: #bdc3c7; -fx-text-fill: #7f8c8d; -fx-font-weight: bold;");
    }
    applyFilters();
  }

  @FXML
  public void onFilterOutClicked() {
    if (flowFilterState == 2) {
      flowFilterState = 0;
      btnFilterOut.setStyle(
          "-fx-background-color: transparent; -fx-border-color: #bdc3c7; -fx-text-fill: #7f8c8d; -fx-font-weight: bold;");
    } else {
      flowFilterState = 2;
      btnFilterOut.setStyle(
          "-fx-background-color: #e67e22; -fx-border-color: #e67e22; -fx-text-fill: white; -fx-font-weight: bold;");
      btnFilterIn.setStyle(
          "-fx-background-color: transparent; -fx-border-color: #bdc3c7; -fx-text-fill: #7f8c8d; -fx-font-weight: bold;");
    }
    applyFilters();
  }

  @FXML
  public void onClearFilterClicked() {
    txtSearch.clear();
    cbTimeFilter.setValue("Tất cả thời gian");
    flowFilterState = 0;
    btnFilterIn.setStyle(
        "-fx-background-color: transparent; -fx-border-color: #bdc3c7; -fx-text-fill: #7f8c8d; -fx-font-weight: bold;");
    btnFilterOut.setStyle(
        "-fx-background-color: transparent; -fx-border-color: #bdc3c7; -fx-text-fill: #7f8c8d; -fx-font-weight: bold;");
    applyFilters();
  }

  @FXML
  public void onDepositButtonClicked() {
    handleWalletTransaction(TransactionType.DEPOSIT, true);
  }

  @FXML
  public void onWithdrawButtonClicked() {
    handleWalletTransaction(TransactionType.WITHDRAWAL, false);
  }

  @FXML
  public void onOpenBankSettings() {
    AuctionManager.getInstance().requestSettingsTab(SETTINGS_BANK_TAB);
    SceneManager.getInstance().switchScene("/client/settings/settings.fxml");
  }

  private void handleWalletTransaction(TransactionType type, boolean isDeposit) {
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

    Long userId = AuctionManager.getInstance().getId();
    String transactionNote = note;
    txtAmount.setDisable(true);
    new Thread(
            () -> {
              try {
                String urlString =
                    Constants.BASE_URL
                        + "/user/"
                        + userId
                        + "/transaction"
                        + "?amount="
                        + amount
                        + "&type="
                        + URLEncoder.encode(type.name(), StandardCharsets.UTF_8)
                        + "&note="
                        + URLEncoder.encode(transactionNote, StandardCharsets.UTF_8);
                HttpRequest request =
                    HttpRequest.newBuilder()
                        .uri(URI.create(urlString))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();
                HttpResponse<String> response =
                    ClientHttp.send(request);
                JsonNode transaction =
                    response.statusCode() == 200 ? OBJECT_MAPPER.readTree(response.body()) : null;
                Platform.runLater(
                    () ->
                        handleWalletTransactionResponse(
                            response, transaction, amount, type, transactionNote, isDeposit));
              } catch (Exception e) {
                Platform.runLater(
                    () -> {
                      txtAmount.setDisable(false);
                      showWalletError(ServerConnectionException.MESSAGE, false);
                    });
              }
            })
        .start();
  }

  private void handleWalletTransactionResponse(
      HttpResponse<String> response,
      JsonNode transaction,
      double amount,
      TransactionType type,
      String note,
      boolean isDeposit) {
    txtAmount.setDisable(false);
    if (response.statusCode() != 200 || transaction == null) {
      showWalletError("Lỗi từ máy chủ: " + response.body(), false);
      return;
    }

    Long transactionId = transaction.path("id").asLong();
    double newBalance = transaction.path("lastBalance").asDouble();
    AuctionManager.getInstance().setBalance(newBalance);
    refreshWalletBalance();

    String formattedAmount = formatCurrency(amount);
    String moneyInValue = isDeposit ? "+ " + formattedAmount : "";
    String moneyOutValue = isDeposit ? "" : "- " + formattedAmount;
    LocalDateTime time = LocalDateTime.now();
    WALLET_TRANSACTIONS.add(
        0,
        new TransactionViewModel(
            transactionId,
            time,
            time.format(TIME_FORMAT),
            moneyInValue,
            moneyOutValue,
            formatCurrency(newBalance),
            type.getDisplayName(),
            note));
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
    Long userId = AuctionManager.getInstance().getId();
    if (userId == null) {
      updateDisplayedBalance(0.0, 0.0, 0.0);
      return;
    }

    new Thread(
            () -> {
              try {
                HttpRequest request =
                    HttpRequest.newBuilder()
                        .uri(URI.create(Constants.BASE_URL + "/user/" + userId + "/wallet"))
                        .GET()
                        .build();
                HttpResponse<String> response =
                    ClientHttp.send(request);

                if (response.statusCode() == 200) {
                  JsonNode summary = OBJECT_MAPPER.readTree(response.body());
                  double totalBalance = summary.path("totalBalance").asDouble();
                  double heldBalance = summary.path("heldBalance").asDouble();
                  double availableBalance = summary.path("availableBalance").asDouble();
                  Platform.runLater(
                      () -> {
                        AuctionManager.getInstance().setBalance(availableBalance);
                        updateDisplayedBalance(totalBalance, heldBalance, availableBalance);
                      });
                }
              } catch (Exception e) {
                Platform.runLater(
                    () ->
                        updateDisplayedBalance(
                            AuctionManager.getInstance().getBalance(),
                            0.0,
                            AuctionManager.getInstance().getBalance()));
              }
            })
        .start();
  }

  private void updateDisplayedBalance(
      double totalBalance, double heldBalance, double availableBalance) {
    lblBalance.setText(formatCurrency(availableBalance));
    lblTotalBalance.setText("Tổng số dư: " + formatCurrency(totalBalance));
    lblHeldBalance.setText("Đang giữ đấu giá: " + formatCurrency(heldBalance));
  }

  private String formatCurrency(double amount) {
    return VND_FORMAT.format(amount);
  }

  private LocalDateTime parseTransactionTime(String value) {
    try {
      return LocalDateTime.parse(value, TIME_FORMAT);
    } catch (Exception e) {
      return LocalDateTime.now();
    }
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
