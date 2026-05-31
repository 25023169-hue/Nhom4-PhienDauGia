package io.auctionsystem.client.controller;

import io.auctionsystem.common.dto.SellerProductDTO;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

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

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));

    public static void setProduct(SellerProductDTO product) {
        selectedProduct = product;
    }

    @FXML
    public void initialize() {
        SellerProductDTO product = selectedProduct;
        if (product == null) {
            lblItemName.setText("Không tìm thấy sản phẩm");
            return;
        }

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

    private String formatPrice(Double price, String fallback) {
        return price == null ? fallback : currencyFormat.format(price);
    }

    private String valueOrDash(Object value) {
        return value == null || value.toString().isBlank() ? "-" : value.toString();
    }
}
