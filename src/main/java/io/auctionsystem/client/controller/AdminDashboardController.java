package io.auctionsystem.client.controller;

import io.auctionsystem.client.pattern.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.TabPane;

public class AdminDashboardController {

    // Khai báo để điều khiển cái TabPane ở giữa giao diện
    @FXML
    private TabPane tabPane;

    @FXML
    public void initialize() {
        System.out.println("Đã tải xong Dashboard Admin!");
    }

    @FXML
    public void onDashboardClicked() {
        System.out.println("Menu: Bấm Tổng quan");
        tabPane.getSelectionModel().select(0); // Chuyển sang Tab đầu tiên (Tổng quan)
    }

    @FXML
    public void onManageUsersClicked() {
        System.out.println("Menu: Bấm Người dùng");
        tabPane.getSelectionModel().select(1); // Chuyển sang Tab thứ 2 (Người dùng)
    }

    @FXML
    public void onApproveAuctionsClicked() {
        System.out.println("Menu: Bấm Phiên đấu giá");
        tabPane.getSelectionModel().select(2); // Chuyển sang Tab thứ 3 (Phiên đấu giá)
    }

    @FXML
    public void onSettingsClicked() {
        System.out.println("Menu: Bấm Cài đặt");
    }

    @FXML
    public void onLogoutClicked() {
        System.out.println("Admin đang đăng xuất...");
        SceneManager.getInstance().switchScene("/client/fxml/login.fxml");
    }
}