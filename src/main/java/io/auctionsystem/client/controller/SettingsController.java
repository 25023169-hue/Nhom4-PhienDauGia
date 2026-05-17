package io.auctionsystem.client.controller;

import io.auctionsystem.client.pattern.AuctionManager;
import io.auctionsystem.client.pattern.SceneManager;
import io.auctionsystem.common.dto.AuthResponse;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.Optional;

public class SettingsController {

    @FXML private VBox paneProfile, paneBank, paneAddress, panePassword;
    @FXML private Button btnProfile, btnBank, btnAddress, btnPassword;

    @FXML private TextField txtUsername, txtFirstName, txtLastName, txtBankAccount, txtRecovery;
    @FXML private TextField txtAccountHolder;
    @FXML private ComboBox<String> cbBankName;
    @FXML private TextArea txtAddressArea;
    @FXML private PasswordField txtOldPass, txtNewPass;

    @FXML
    public void initialize() {
        // 1. Load danh sách ngân hàng phổ biến (Kết hợp với editable=true trong FXML)
        cbBankName.getItems().addAll(
                "Vietcombank", "MB Bank", "Techcombank", "Agribank", "BIDV",
                "TPBank", "ACB", "VPBank", "Sacombank", "VietinBank"
        );

        // 2. Đổ dữ liệu user hiện tại
        AuthResponse user = AuctionManager.getInstance().getCurrentUser();
        if (user != null) {
            txtUsername.setText(user.getUsername());
            txtLastName.setText(user.getLastname());
            txtFirstName.setText(user.getFirstname());
            if (user.getBankName() != null) {
                cbBankName.getEditor().setText(user.getBankName());
            }
            if (user.getBankAccount() != null && !user.getBankAccount().trim().isEmpty()) {
                txtBankAccount.setText(user.getBankAccount());
            }
            txtAccountHolder.setText((user.getLastname() + " " + user.getFirstname()).trim().toUpperCase());
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
        System.out.println("Lưu hồ sơ: " + txtLastName.getText() + txtFirstName.getText());
    }

    public void saveBank() {
        // Lấy text từ editor để hỗ trợ trường hợp người dùng tự gõ vào ComboBox
        String bank = cbBankName.getEditor().getText().trim();
        String holder = txtAccountHolder.getText().trim();
        String bankacc = txtBankAccount.getText().trim();

        if (bank.isEmpty() || holder.isEmpty() || bankacc.isEmpty()) {
            showInfo("Vui lòng nhập đủ thông tin ngân hàng.");
            return;
        }
        if (!bankacc.matches("\\d+")) {
            showInfo("Số tài khoản chỉ được chứa chữ số.");
            return;
        }

        AuthResponse user = AuctionManager.getInstance().getCurrentUser();
        if (user != null) {
            user.setBankName(bank);
            user.setBankAccount(bankacc);
        }

        System.out.println("Lưu Bank: " + bank + " | Chủ thẻ: " + holder.toUpperCase() + " | STK: " + bankacc);
        System.out.println(AuctionManager.getInstance().hasBankInfo());
        showInfo("Đã lưu thông tin ngân hàng trong phiên đăng nhập hiện tại.");
    }

    public void saveAddress() {
        System.out.println("Địa chỉ mới: " + txtAddressArea.getText());
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
