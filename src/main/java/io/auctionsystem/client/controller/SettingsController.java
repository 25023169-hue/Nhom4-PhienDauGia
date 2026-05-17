package io.auctionsystem.client.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.auctionsystem.client.pattern.AuctionManager;
import io.auctionsystem.client.pattern.SceneManager;
import io.auctionsystem.common.dto.AddressRequest;
import io.auctionsystem.common.dto.AuthResponse;
import io.auctionsystem.common.dto.BankRequest;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

public class SettingsController {

    @FXML private VBox paneProfile, paneBank, paneAddress, panePassword;
    @FXML private Button btnProfile, btnBank, btnAddress, btnPassword;

    @FXML private TextField txtUsername, txtFirstName, txtLastName, txtAccountName, txtBankAccount, txtRecovery;

    // Đã đổi tên biến thành cbBankName
    @FXML private ComboBox<String> cbBankName;

    @FXML private TextArea txtAddressArea;
    @FXML private PasswordField txtOldPass, txtNewPass;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    // Khai báo danh sách ngân hàng làm biến toàn cục để dùng cho Autocomplete
    private final ObservableList<String> BANK_LIST = FXCollections.observableArrayList(
            "Vietcombank (VCB)", "MB Bank (MB)", "Techcombank (TCB)", "Agribank",
            "BIDV", "TP Bank", "ACB", "VP Bank", "Sacombank", "VietinBank"
    );

    @FXML
    public void initialize() {
        // --- THIẾT LẬP AUTOCOMPLETE CHO COMBOBOX ---
        cbBankName.setItems(BANK_LIST);

        // Bắt sự kiện mỗi khi người dùng gõ phím vào ô nhập
        cbBankName.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
            // Nếu người dùng chọn item bằng chuột (hoặc khởi tạo), giá trị cũ và mới sẽ giống nhau
            if (newValue == null || newValue.isEmpty()) {
                cbBankName.setItems(BANK_LIST);
                return;
            }

            // Chỉ lọc khi thực sự có thay đổi từ bàn phím
            ObservableList<String> filteredList = FXCollections.observableArrayList();
            for (String bank : BANK_LIST) {
                if (bank.toLowerCase().contains(newValue.toLowerCase())) {
                    filteredList.add(bank);
                }
            }
            cbBankName.setItems(filteredList);

            // Nếu không đang show thì show ra để người dùng dễ nhìn
            if (!cbBankName.isShowing()) {
                cbBankName.show();
            }
        });

        // --- ĐỔ DỮ LIỆU USER CŨ LÊN GIAO DIỆN ---
        AuthResponse user = AuctionManager.getInstance().getCurrentUser();
        if (user != null) {
            txtUsername.setText(user.getUsername() != null ? user.getUsername() : "");
            txtLastName.setText(user.getLastname() != null ? user.getLastname() : "");
            txtFirstName.setText(user.getFirstname() != null ? user.getFirstname() : "");

            // Hiển thị ngân hàng cũ lên ComboBox
            if (user.getBankName() != null && !user.getBankName().isEmpty()) {
                cbBankName.getEditor().setText(user.getBankName());
            }
            txtAccountName.setText(user.getAccountName() != null ? user.getAccountName() : "");
            txtBankAccount.setText(user.getBankAccount() != null ? user.getBankAccount() : "");
            txtAddressArea.setText(user.getAddress() != null ? user.getAddress() : "");
        }

        String requestedTab = AuctionManager.getInstance().consumeSettingsTabRequest();
        if ("BANK".equals(requestedTab)) {
            showBank();
        } else {
            showProfile();
        }
    }

    // --- HÀM CHUYỂN TAB ---
    public void showProfile() { switchTab(paneProfile, btnProfile); }
    public void showBank() { switchTab(paneBank, btnBank); }
    public void showAddress() { switchTab(paneAddress, btnAddress); }
    public void showPassword() { switchTab(panePassword, btnPassword); }

    private void switchTab(VBox activePane, Button activeBtn) {
        paneProfile.setVisible(false);
        paneBank.setVisible(false);
        paneAddress.setVisible(false);
        panePassword.setVisible(false);
        activePane.setVisible(true);

        String normalStyle = "-fx-background-color: transparent; -fx-text-fill: black; -fx-font-weight: normal;";
        btnProfile.setStyle(normalStyle);
        btnBank.setStyle(normalStyle);
        btnAddress.setStyle(normalStyle);
        btnPassword.setStyle(normalStyle);

        activeBtn.setStyle("-fx-background-color: #e9ecef; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
    }

    // --- HÀM XỬ LÝ LƯU ---
    public void saveProfile() {
        System.out.println("Lưu hồ sơ: " + txtLastName.getText() + " " + txtFirstName.getText());
    }

    public void saveBank() {
        // Lấy dữ liệu từ ComboBox Editable
        final String bank = cbBankName.getEditor().getText().trim();
        final String accountname = txtAccountName.getText().trim();
        final String bankaccount = txtBankAccount.getText().trim();

        if (bank.isEmpty() || accountname.isEmpty() || bankaccount.isEmpty()) {
            showInfo("Vui lòng nhập đủ thông tin ngân hàng.");
            return;
        }
        if (!bankaccount.matches("\\d+")) {
            showInfo("Số tài khoản chỉ được chứa chữ số.");
            return;
        }

        AuthResponse user = AuctionManager.getInstance().getCurrentUser();
        if (user == null) return;

        Long userId = user.getUserId();
        BankRequest requestDto = new BankRequest(bank, accountname, bankaccount);

        new Thread(() -> {
            try {
                String jsonBody = objectMapper.writeValueAsString(requestDto);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/user/" + userId + "/bank"))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        user.setBankName(bank);
                        user.setAccountName(accountname);
                        user.setBankAccount(bankaccount);

                        System.out.println("Lưu Bank: " + bank + " | Chủ thẻ: " + accountname.toUpperCase() + " | STK: " + bankaccount);
                        System.out.println(AuctionManager.getInstance().hasBankInfo());
                        showInfo("Đã lưu thông tin ngân hàng thành công vào hệ thống.");
                    } else {
                        showInfo("Lỗi cập nhật từ Server: " + response.body());
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showInfo("Không thể kết nối đến server Spring Boot."));
            }
        }).start();
    }

    public void saveAddress() {
        final String address = txtAddressArea.getText().trim();
        if (address.isEmpty()) {
            showInfo("Vui lòng nhập địa chỉ mới.");
            return;
        }

        AuthResponse user = AuctionManager.getInstance().getCurrentUser();
        if (user == null) return;

        Long userId = user.getUserId();
        AddressRequest requestDto = new AddressRequest(address);

        new Thread(() -> {
            try {
                String jsonBody = objectMapper.writeValueAsString(requestDto);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/user/" + userId + "/address"))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        user.setAddress(address);
                        System.out.println("Địa chỉ mới: " + address);
                        showInfo("Đã lưu địa chỉ thành công!");
                    } else {
                        showInfo("Lỗi cập nhật từ Server: " + response.body());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showInfo("Không thể kết nối đến server Spring Boot."));
            }
        }).start();
    }

    public void savePassword() {
        System.out.println("Đổi mật khẩu: Old=" + txtOldPass.getText() + " | New=" + txtNewPass.getText());
    }

    public void deleteAccount() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận xóa tài khoản");
        alert.setHeaderText("Cảnh báo: Hành động này không thể hoàn tác!");
        alert.setContentText("Bạn có chắc chắn muốn xóa vĩnh viễn tài khoản này không?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            System.out.println("Thực hiện logic xóa tài khoản tại đây...");
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setContentText("Tài khoản của bạn đã được xóa.");
            info.showAndWait();
            SceneManager.getInstance().switchScene("/client/fxml/login.fxml");
        }
    }

    public void onBack() {
        SceneManager.getInstance().switchScene("/client/fxml/dashboard.fxml");
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}