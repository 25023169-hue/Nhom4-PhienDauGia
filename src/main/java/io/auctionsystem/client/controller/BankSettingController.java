package io.auctionsystem.client.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.auctionsystem.client.pattern.AuctionManager;
import io.auctionsystem.client.pattern.SceneManager;
import io.auctionsystem.common.request.BankRequest;
import io.auctionsystem.common.response.AuthResponse;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class BankSettingController {
  @FXML private ComboBox<String> cbBankName;
  @FXML private TextField txtAccountName, txtBankAccount;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final HttpClient httpClient = HttpClient.newHttpClient();
  private String savedBankName = "";
  private boolean updatingBankEditor = false;

  private final List<String> BANK_LIST =
      List.of(
          "Vietcombank (VCB)",
          "MB Bank (MB)",
          "Techcombank (TCB)",
          "Agribank",
          "BIDV",
          "TP Bank",
          "ACB",
          "VP Bank",
          "Sacombank",
          "VietinBank");

  // Sử dụng List thường thay vì FilteredList để tránh bug UI của JavaFX
  private final ObservableList<String> currentBanks = FXCollections.observableArrayList(BANK_LIST);

  @FXML
  public void initialize() {
    cbBankName.setItems(currentBanks);

    // 1. XỬ LÝ LỖI DẤU CÁCH: Chặn tuyệt đối ComboBox tự động chọn khi bấm Space
    cbBankName.addEventFilter(
        KeyEvent.ANY,
        event -> {
          boolean isSpace = event.getCode() == KeyCode.SPACE || " ".equals(event.getCharacter());
          if (isSpace) {
            if (event.getEventType() == KeyEvent.KEY_PRESSED) {
              event.consume();
              TextField editor = cbBankName.getEditor();
              int caret = editor.getCaretPosition();
              String currentText = editor.getText();
              editor.setText(currentText.substring(0, caret) + " " + currentText.substring(caret));
              editor.positionCaret(caret + 1);
            } else if (event.getEventType() == KeyEvent.KEY_RELEASED
                || event.getEventType() == KeyEvent.KEY_TYPED) {
              event.consume();
            }
          }
        });

    // 2. XỬ LÝ LỌC DỮ LIỆU VÀ CHẶN LỖI SNAP-BACK (TỰ ĐIỀN LẠI CHỮ CŨ)
    cbBankName
        .getEditor()
        .textProperty()
        .addListener(
            (observable, oldValue, newValue) -> {
              if (updatingBankEditor) return;

              String keyword = newValue == null ? "" : newValue.trim().toLowerCase();

              List<String> filtered =
                  BANK_LIST.stream()
                      .filter(bank -> keyword.isEmpty() || bank.toLowerCase().contains(keyword))
                      .collect(Collectors.toList());

              if (!currentBanks.equals(filtered)) {
                currentBanks.setAll(filtered);

                if (cbBankName.getEditor().isFocused()) {
                  cbBankName.hide();
                  cbBankName.setVisibleRowCount(Math.min(10, Math.max(1, currentBanks.size())));

                  boolean exactMatch =
                      BANK_LIST.stream().anyMatch(bank -> bank.equalsIgnoreCase(keyword));
                  if (!exactMatch && !currentBanks.isEmpty()) {
                    cbBankName.show();
                  }
                }
              }

              // ĐỒNG BỘ LẠI TEXT CHÍNH XÁC: Đè bệt mọi hành vi tự động điền lại của JavaFX
              final String textToRestore = newValue;
              Platform.runLater(
                  () -> {
                    updatingBankEditor = true;
                    cbBankName.getEditor().setText(textToRestore);
                    cbBankName.getEditor().positionCaret(textToRestore.length());
                    updatingBankEditor = false;
                  });
            });

    // 3. MỞ DROPDOWN KHI CLICK CHUỘT
    cbBankName
        .getEditor()
        .setOnMouseClicked(
            e -> {
              if (cbBankName.getEditor().isFocused()
                  && !currentBanks.isEmpty()
                  && !cbBankName.isShowing()) {
                cbBankName.setVisibleRowCount(Math.min(10, Math.max(1, currentBanks.size())));
                cbBankName.show();
              }
            });

    // 4. THU GỌN BOX SAU KHI CHỌN KHỎI DANH SÁCH
    cbBankName
        .valueProperty()
        .addListener(
            (observable, oldValue, newValue) -> {
              if (newValue != null && BANK_LIST.contains(newValue)) {
                updatingBankEditor = true;
                cbBankName.getEditor().setText(newValue);
                updatingBankEditor = false;
                cbBankName.hide();

                Platform.runLater(
                    () -> {
                      cbBankName.getEditor().positionCaret(newValue.length());
                    });
              }
            });

    loadUserData();
  }

  private void loadUserData() {
    AuthResponse user = AuctionManager.getInstance().getCurrentUser();
    if (user != null) {
      txtAccountName.setText(user.getAccountName() != null ? user.getAccountName() : "");
      txtBankAccount.setText(user.getBankAccount() != null ? user.getBankAccount() : "");
      if (user.getBankName() != null && !user.getBankName().isEmpty()) {
        savedBankName = user.getBankName();
        showBankName(savedBankName);
      }
    }
  }

  @FXML
  public void saveBank() {
    final String bank = cbBankName.getEditor().getText().trim();
    final String accountname = txtAccountName.getText().trim();
    final String bankaccount = txtBankAccount.getText().trim();

    if (bank.isEmpty() || accountname.isEmpty() || bankaccount.isEmpty()) {
      showAlert("Vui lòng nhập đủ thông tin ngân hàng.");
      return;
    }

    AuthResponse user = AuctionManager.getInstance().getCurrentUser();
    if (user == null) return;

    new Thread(
            () -> {
              try {
                BankRequest requestDto = new BankRequest(bank, accountname, bankaccount);
                String jsonBody = objectMapper.writeValueAsString(requestDto);
                HttpRequest request =
                    HttpRequest.newBuilder()
                        .uri(
                            URI.create(
                                "http://localhost:8080/api/user/" + user.getUserId() + "/bank"))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                Platform.runLater(
                    () -> {
                      if (response.statusCode() == 200) {
                        user.setBankName(bank);
                        user.setAccountName(accountname);
                        user.setBankAccount(bankaccount);
                        savedBankName = bank;
                        showAlert("Đã lưu thông tin ngân hàng thành công.");
                        navigateToWallet();
                      }
                    });
              } catch (Exception e) {
                Platform.runLater(() -> showAlert("Lỗi kết nối tới Server."));
              }
            })
        .start();
  }

  private void showBankName(String bankName) {
    updatingBankEditor = true;
    cbBankName.setValue(bankName);
    cbBankName.getEditor().setText(bankName);
    updatingBankEditor = false;
  }

  private void navigateToWallet() {
    String dashboardPath =
        SettingsController.isSellerChannel
            ? "/client/fxml/user/seller/seller_dashboard.fxml"
            : "/client/fxml/user/bidder/bidder_dashboard.fxml";

    SceneManager.getInstance()
        .switchScene(
            dashboardPath,
            controller -> {
              if (controller instanceof SellerDashboardController sellerDashboard) {
                sellerDashboard.onWalletButtonClicked();
              } else if (controller instanceof BidderDashboardController bidderDashboard) {
                bidderDashboard.onWalletButtonClicked();
              }
            });
  }

  private void showAlert(String msg) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setHeaderText(null);
    alert.setContentText(msg);
    alert.showAndWait();
  }
}
