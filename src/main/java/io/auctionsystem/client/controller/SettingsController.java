package io.auctionsystem.client.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.auctionsystem.client.pattern.AuctionManager;
import io.auctionsystem.client.pattern.SceneManager;
import io.auctionsystem.common.dto.AddressRequest;
import io.auctionsystem.common.dto.AuthResponse;
import io.auctionsystem.common.dto.BankRequest;
import javafx.application.Platform;
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
    @FXML private ComboBox<String> BankList;
    @FXML private TextArea txtAddressArea;
    @FXML private PasswordField txtOldPass, txtNewPass;

    // Thêm các công cụ gọi HTTP và Parse JSON
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @FXML
    public void initialize() {
        // 1. Load danh sách ngân hàng phổ biến (Kết hợp với editable=true trong FXML)
        BankList.getItems().addAll(
                "Vietcombank", "MB Bank", "Techcombank", "Agribank", "BIDV",
                "TPBank", "ACB", "VPBank", "Sacombank", "VietinBank"
        );

        // 2. Đổ dữ liệu user hiện tại
        AuthResponse user = AuctionManager.getInstance().getCurrentUser();
        if (user != null) {
            txtUsername.setText(user.getUsername() != null ? user.getUsername() : "");
            txtLastName.setText(user.getLastname() != null ? user.getLastname() : "");
            txtFirstName.setText(user.getFirstname() != null ? user.getFirstname() : "");

            // Đổ thêm thông tin Bank và Address lên giao diện
            if (user.getBankName() != null && !user.getBankName().isEmpty()) {
                BankList.setValue(user.getBankName());
            }
            txtBankAccount.setText(user.getBankAccount() != null ? user.getBankAccount() : "");
            txtAccountName.setText(user.getAccountName() != null ? user.getAccountName() : "");
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

        // Reset style các nút
        String normalStyle = "-fx-background-color: transparent; -fx-text-fill: black; -fx-font-weight: normal;";
        btnProfile.setStyle(normalStyle);
        btnBank.setStyle(normalStyle);
        btnAddress.setStyle(normalStyle);
        btnPassword.setStyle(normalStyle);

        // Highlight nút đang chọn (Màu đỏ nhạt giống Dashboard hoặc màu tùy chọn)
        activeBtn.setStyle("-fx-background-color: #e9ecef; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
    }

    // --- HÀM XỬ LÝ LƯU ---
    public void saveProfile() {
        System.out.println("Lưu hồ sơ: " + txtLastName.getText() + " " + txtFirstName.getText());
    }

    public void saveBank() {
        // Lấy text từ editor để hỗ trợ trường hợp người dùng tự gõ vào ComboBox
        String bankValue = BankList.getValue();
        final String bank = (bankValue == null || bankValue.trim().isEmpty())
                ? BankList.getEditor().getText().trim()
                : bankValue.trim();

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
        BankRequest requestDto = new BankRequest(bank, bankaccount, accountname);

        // GỌI API XUỐNG SERVER
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
                        // Sửa lỗi gán sai biến lúc trước
                        user.setBankName(bank);
                        user.setAccountName(accountname);
                        user.setBankAccount(bankaccount);

                        System.out.println("Lưu Bank: " + bank + " | Chủ thẻ: " + accountname.toUpperCase() + " | STK: " + bankaccount);
                        // Giữ nguyên dòng in check trạng thái Bank theo yêu cầu
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

    // --- XÓA TÀI KHOẢN (CẤP ĐỘ 2: XÁC NHẬN) ---
    public void deleteAccount() {
        // Tạo Alert xác nhận
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận xóa tài khoản");
        alert.setHeaderText("Cảnh báo: Hành động này không thể hoàn tác!");
        alert.setContentText("Bạn có chắc chắn muốn xóa vĩnh viễn tài khoản này không?");

        // Hiển thị và xử lý lựa chọn
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            System.out.println("Thực hiện logic xóa tài khoản tại đây...");

            // Ví dụ: Thông báo thành công trước khi thoát
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setContentText("Tài khoản của bạn đã được xóa.");
            info.showAndWait();

            // Quay lại màn hình đăng nhập
            SceneManager.getInstance().switchScene("/client/fxml/login.fxml");
        } else {
            System.out.println("Hủy bỏ yêu cầu xóa.");
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