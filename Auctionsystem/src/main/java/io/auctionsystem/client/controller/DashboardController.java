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
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javafx.stage.StageStyle;
import javafx.stage.Window;

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
        // 1. Tạo thông báo (Noti) loại Xác nhận (Confirmation)
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);

        // Vì ta sẽ ẩn thanh tiêu đề, nên dùng HeaderText để làm tiêu đề chính
        alert.setHeaderText("Xác nhận đăng xuất");
        alert.setContentText("Bạn có chắc chắn muốn đăng xuất khỏi hệ thống?");

        // 2. Không tạo cửa sổ mới trên Taskbar & Căn giữa màn hình hiện tại:
        // Lấy cửa sổ (Window) đang chứa lblWelcome để làm "Chủ" (Owner) cho Noti này.
        Window ownerWindow = lblWelcome.getScene().getWindow();
        alert.initOwner(ownerWindow);

        // 3. Không cho phép di chuyển:
        // Đặt style UNDECORATED sẽ ẩn thanh viền của Windows (nơi dùng để nắm kéo thả)
        alert.initStyle(StageStyle.UNDECORATED);

        // 4. Sửa nút OK / Cancel tiếng Anh thành Có / Không
        ButtonType btnYes = new ButtonType("Có", ButtonBar.ButtonData.YES);
        ButtonType btnNo = new ButtonType("Không", ButtonBar.ButtonData.NO);
        alert.getButtonTypes().setAll(btnYes, btnNo);

        // 5. Hiển thị Noti lên và lắng nghe hành động
        alert.showAndWait().ifPresent(response -> {
            if (response == btnYes) {
                // Nếu bấm "Có" -> Thực hiện xóa session và chuyển màn hình
                AuctionManager.getInstance().logout();
                io.auctionsystem.client.pattern.SceneManager.getInstance().switchScene("/client/fxml/login.fxml");
            }
            // Nếu bấm "Không" -> Thông báo tự đóng, không làm gì cả
        });
    }
}