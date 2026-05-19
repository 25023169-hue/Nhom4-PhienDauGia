package io.auctionsystem.client.controller;

import io.auctionsystem.client.pattern.AuctionManager;
import io.auctionsystem.client.pattern.SceneManager;
import io.auctionsystem.common.response.AuthResponse;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import java.util.Optional;

public class ProfileSettingController {
    @FXML private TextField txtUsername, txtLastName, txtFirstName;

    @FXML
    public void initialize() {
        AuthResponse user = AuctionManager.getInstance().getCurrentUser();
        if (user != null) {
            txtUsername.setText(user.getUsername() != null ? user.getUsername() : "");
            txtLastName.setText(user.getLastname() != null ? user.getLastname() : "");
            txtFirstName.setText(user.getFirstname() != null ? user.getFirstname() : "");
        }
    }

    @FXML
    public void saveProfile() {
        System.out.println("Lưu hồ sơ: " + txtLastName.getText() + " " + txtFirstName.getText());
    }

    @FXML
    public void deleteAccount() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận xóa tài khoản");
        alert.setHeaderText("Cảnh báo: Hành động này không thể hoàn tác!");
        alert.setContentText("Bạn có chắc chắn muốn xóa vĩnh viễn tài khoản này không?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            SceneManager.getInstance().switchScene("/client/fxml/login.fxml");
        }
    }
}