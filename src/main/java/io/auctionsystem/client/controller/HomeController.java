package io.auctionsystem.client.controller;

import javafx.fxml.FXML;

public class HomeController {

    @FXML
    public void onProductListButtonClicked() {
        // Chuyển tab qua danh sách sản phẩm thông qua class cha
        DashboardController.getInstance().onProductListButtonClicked();
    }

    @FXML
    public void onSellerChannelButtonClicked() {
        DashboardController.getInstance().onSellerChannelButtonClicked();
    }
}