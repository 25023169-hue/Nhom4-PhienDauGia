package io.auctionsystem.client.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.auctionsystem.client.pattern.AuctionManager;
import io.auctionsystem.common.Constants;
import io.auctionsystem.common.dto.SellerProductDTO;
import io.auctionsystem.common.enums.AuctionState;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.NumberFormat;
import java.util.Locale;

public class SellerProductDetailController {

    private static SellerProductDTO selectedProduct;

    @FXML private Label lblItemName;
    @FXML private Label lblItemId;
    @FXML private Label lblItemType;
    @FXML private Label lblDescription;
    @FXML private Label lblStartingPrice;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblSoldPrice;
    @FXML private Label lblBuyNowPrice;
    @FXML private Label lblStatus;
    @FXML private Label lblStartTime;
    @FXML private Label lblEndTime;
    @FXML private Button btnEdit;
    @FXML private Button btnDelete;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public static void setProduct(SellerProductDTO product) {
        selectedProduct = product;
    }

    @FXML
    public void initialize() {
        SellerProductDTO product = selectedProduct;
        if (product == null) {
            lblItemName.setText("Không tìm thấy sản phẩm");
            btnEdit.setVisible(false);
            btnEdit.setManaged(false);
            btnDelete.setVisible(false);
            btnDelete.setManaged(false);
            return;
        }

        boolean editable = product.getStatus() == AuctionState.OPEN;
        btnEdit.setVisible(editable);
        btnEdit.setManaged(editable);
        lblItemName.setText(valueOrDash(product.getItemName()));
        lblItemId.setText(valueOrDash(product.getItemId()));
        lblItemType.setText(valueOrDash(product.getItemType()));
        lblDescription.setText(valueOrDash(product.getDescription()));
        lblStartingPrice.setText(formatPrice(product.getStartingPrice(), "Chưa cài đặt"));
        lblCurrentPrice.setText(formatPrice(product.getCurrentPrice(), "Chưa có"));
        lblSoldPrice.setText(formatPrice(product.getSoldPrice(), "Chưa bán"));
        lblBuyNowPrice.setText(formatPrice(product.getBuyNowPrice(), "Không có"));
        lblStatus.setText(valueOrDash(product.getStatus()));
        lblStartTime.setText(valueOrDash(product.getStartTime()));
        lblEndTime.setText(valueOrDash(product.getEndTime()));
    }

    @FXML
    public void onBackClicked() {
        SellerDashboardController controller = SellerDashboardController.getInstance();
        if (controller != null) {
            controller.onManageAuctionsClicked();
        }
    }

    @FXML
    public void onEditClicked() {
        SellerProductDTO product = selectedProduct;
        SellerDashboardController controller = SellerDashboardController.getInstance();
        if (product != null && product.getStatus() == AuctionState.OPEN && controller != null) {
            controller.onEditProductClicked(product);
        }
    }

    @FXML
    public void onDeleteClicked() {
        SellerProductDTO product = selectedProduct;
        Long sellerId = AuctionManager.getInstance().getId();
        if (product == null || product.getItemId() == null || sellerId == null) {
            return;
        }

        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Bạn có chắc chắn muốn xóa sản phẩm này khỏi danh sách quản lý?",
                ButtonType.YES,
                ButtonType.NO
        );
        confirmation.setHeaderText("Xác nhận xóa sản phẩm");
        if (confirmation.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }

        btnDelete.setDisable(true);
        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(Constants.BASE_URL + "/seller/products/" + product.getItemId()
                                + "?sellerId=" + sellerId))
                        .DELETE()
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                Platform.runLater(() -> handleDeleteResponse(response));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    btnDelete.setDisable(false);
                    showAlert(Alert.AlertType.ERROR, "Không thể kết nối đến server khi xóa sản phẩm.");
                });
            }
        }).start();
    }

    private void handleDeleteResponse(HttpResponse<String> response) {
        btnDelete.setDisable(false);
        String message = extractMessage(response.body());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            SellerProductListController.forgetProduct(selectedProduct);
            showAlert(Alert.AlertType.INFORMATION, message);
            onBackClicked();
            return;
        }
        showAlert(Alert.AlertType.ERROR, message);
    }

    private String extractMessage(String body) {
        try {
            JsonNode node = objectMapper.readTree(body);
            if (node.hasNonNull("message")) {
                return node.get("message").asText();
            }
        } catch (Exception ignored) {
        }
        return body == null || body.isBlank() ? "Server trả về lỗi không xác định." : body;
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String formatPrice(Double price, String fallback) {
        return price == null ? fallback : currencyFormat.format(price);
    }

    private String valueOrDash(Object value) {
        return value == null || value.toString().isBlank() ? "-" : value.toString();
    }
}
