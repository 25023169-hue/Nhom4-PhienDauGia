package io.auctionsystem.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class PasswordSettingController {
    @FXML private PasswordField txtOldPass, txtNewPass;
    @FXML private TextField txtRecovery;

    @FXML
    public void savePassword() {
        System.out.println("Đổi mật khẩu: Old=" + txtOldPass.getText() + " | New=" + txtNewPass.getText());
    }
}