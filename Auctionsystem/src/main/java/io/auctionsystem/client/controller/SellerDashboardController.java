package io.auctionsystem.client.controller;

import io.auctionsystem.client.pattern.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class SellerDashboardController {

    @FXML private TableView<?> tableMyProducts; // Tạm thời dùng <?> chờ bạn định nghĩa DTO Sản phẩm
    @FXML private TableColumn<?, Long> colId;
    @FXML private TableColumn<?, String> colName;
    @FXML private TableColumn<?, Double> colPrice;
    @FXML private TableColumn<?, String> colStatus;

    @FXML
    public void initialize() {
        // Sau này bạn sẽ code logic gọi API lấy danh sách sản phẩm của Seller ở đây
        System.out.println("Giao diện Kênh người bán đã tải thành công!");
    }

    @FXML
    public void onAddProductClicked() {
        // Nút thêm sản phẩm ở góc phải
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText("Tính năng Thêm sản phẩm đang được phát triển!");
        alert.showAndWait();

        // Sau này thay bằng:
        // SceneManager.getInstance().switchScene("/client/fxml/add_product.fxml");
    }

    @FXML
    public void onBackToBuyerChannelClicked() {
        // Nút quay lại màn hình chính của người mua
        SceneManager.getInstance().switchScene("/client/fxml/dashboard.fxml");
    }
}