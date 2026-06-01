package client.controller;

import javafx.fxml.FXML;

public class HomeController {

  @FXML
  public void onProductListButtonClicked() {
    // Chuyển tab qua danh sách sản phẩm thông qua class cha
    BidderDashboardController.getInstance().onProductListButtonClicked();
  }

  @FXML
  public void onSellerChannelButtonClicked() {
    BidderDashboardController.getInstance().onSellerChannelButtonClicked();
  }
}
