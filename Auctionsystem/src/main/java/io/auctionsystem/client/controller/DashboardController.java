package io.auctionsystem.client.controller;

import io.auctionsystem.client.pattern.AuctionManager;
import io.auctionsystem.client.pattern.SceneManager;
import io.auctionsystem.common.dto.AuctionItemDTO; // Bạn cần tạo class này
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class DashboardController {

    @FXML private Label lblWelcome;

    // 1. Xác định rõ TableView sẽ chứa kiểu dữ liệu AuctionItemDTO
    @FXML private TableView<AuctionItemDTO> tableItems;
    @FXML private TableColumn<AuctionItemDTO, Long> colId;
    @FXML private TableColumn<AuctionItemDTO, String> colName;
    @FXML private TableColumn<AuctionItemDTO, Double> colCurrentPrice;
    @FXML private TableColumn<AuctionItemDTO, String> colEndTime;
    @FXML private TableColumn<AuctionItemDTO, String> colStatus;

    @FXML
    public void initialize() {
        // Hiển thị tên người dùng (Dùng bản Master AuctionManager)
        String username = AuctionManager.getInstance().getUsername();
        lblWelcome.setText("Xin chào, " + username);

        // 2. Cấu hình Mapping: Cột nào lấy dữ liệu từ biến nào trong AuctionItemDTO
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colEndTime.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // TODO: Gọi API lấy dữ liệu thật.
        // Tạm thời mình sẽ không thêm dữ liệu giả ở đây để tránh làm code rối.
    }

    @FXML
    public void onLogoutButtonClicked() {
        // Dùng hàm logout của bản Master để dọn dẹp sạch sẽ Session
        AuctionManager.getInstance().logout();
        SceneManager.getInstance().switchScene("/client/fxml/login.fxml");
    }
}