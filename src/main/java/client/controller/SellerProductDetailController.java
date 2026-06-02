package client.controller;

import server.exception.ServerConnectionException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import client.pattern.AuctionManager;
import client.pattern.ClientHttp;
import common.Constants;
import common.dto.SellerProductDTO;
import common.enums.AuctionState;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.NumberFormat;
import java.util.Locale;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;

public class SellerProductDetailController {

  private static SellerProductDTO selectedProduct;

  @FXML private Label lblItemName;
  @FXML private Label lblItemId;
  @FXML private Label lblItemType;
  @FXML private Label lblDescription;
  @FXML private Label lblStartingPrice;
  @FXML private Label lblSoldPrice;
  @FXML private Label lblBuyNowPrice;
  @FXML private Label lblStatus;
  @FXML private Label lblStartTime;
  @FXML private Label lblEndTime;
  @FXML private Button btnStart;
  @FXML private Button btnEdit;
  @FXML private Button btnDelete;

  private final NumberFormat currencyFormat =
      NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));

  private final ObjectMapper objectMapper = ClientHttp.mapper();

  public static void setProduct(SellerProductDTO product) {
    selectedProduct = product;
  }

  @FXML
  public void initialize() {
    SellerProductDTO product = selectedProduct;
    if (product == null) {
      lblItemName.setText("Không tìm thấy sản phẩm");
      btnStart.setVisible(false);
      btnStart.setManaged(false);
      btnEdit.setVisible(false);
      btnEdit.setManaged(false);
      btnDelete.setVisible(false);
      btnDelete.setManaged(false);
      return;
    }

    boolean editable = product.getStatus() == AuctionState.OPEN;
    btnStart.setVisible(editable);
    btnStart.setManaged(editable);
    btnEdit.setVisible(editable);
    btnEdit.setManaged(editable);
    lblItemName.setText(valueOrDash(product.getItemName()));
    lblItemId.setText(valueOrDash(product.getItemId()));
    lblItemType.setText(valueOrDash(product.getItemType()));
    lblDescription.setText(valueOrDash(product.getDescription()));
    lblStartingPrice.setText(formatPrice(product.getStartingPrice(), "Chưa cài đặt"));
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
  public void onStartClicked() {
    SellerProductDTO product = selectedProduct;
    Long sellerId = AuctionManager.getInstance().getId();
    if (product == null
        || product.getItemId() == null
        || sellerId == null
        || product.getStatus() != AuctionState.OPEN) {
      return;
    }

    Alert confirmation =
        new Alert(
            Alert.AlertType.CONFIRMATION,
            "Bắt đầu phiên ngay bây giờ? Thời gian kết thúc sẽ được tính lại theo thời lượng đã cài đặt.",
            ButtonType.YES,
            ButtonType.NO);
    confirmation.setHeaderText("Xác nhận bắt đầu phiên");
    if (confirmation.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
      return;
    }

    btnStart.setDisable(true);
    new Thread(
            () -> {
              try {
                HttpRequest request =
                    HttpRequest.newBuilder()
                        .uri(
                            URI.create(
                                Constants.BASE_URL
                                    + "/seller/products/"
                                    + product.getItemId()
                                    + "/start?sellerId="
                                    + sellerId))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();
                HttpResponse<String> response =
                    ClientHttp.send(request);
                Platform.runLater(() -> handleStartResponse(response));
              } catch (Exception e) {
                Platform.runLater(
                    () -> {
                      btnStart.setDisable(false);
                      showAlert(
                          Alert.AlertType.ERROR, ServerConnectionException.MESSAGE);
                    });
              }
            })
        .start();
  }

  @FXML
  public void onDeleteClicked() {
    SellerProductDTO product = selectedProduct;
    Long sellerId = AuctionManager.getInstance().getId();
    if (product == null || product.getItemId() == null || sellerId == null) {
      return;
    }

    Alert confirmation =
        new Alert(
            Alert.AlertType.CONFIRMATION,
            "Bạn có chắc chắn muốn xóa sản phẩm này khỏi danh sách quản lý?",
            ButtonType.YES,
            ButtonType.NO);
    confirmation.setHeaderText("Xác nhận xóa sản phẩm");
    if (confirmation.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
      return;
    }

    btnDelete.setDisable(true);
    new Thread(
            () -> {
              try {
                HttpRequest request =
                    HttpRequest.newBuilder()
                        .uri(
                            URI.create(
                                Constants.BASE_URL
                                    + "/seller/products/"
                                    + product.getItemId()
                                    + "?sellerId="
                                    + sellerId))
                        .DELETE()
                        .build();
                HttpResponse<String> response =
                    ClientHttp.send(request);
                Platform.runLater(() -> handleDeleteResponse(response));
              } catch (Exception e) {
                Platform.runLater(
                    () -> {
                      btnDelete.setDisable(false);
                      showAlert(
                          Alert.AlertType.ERROR, ServerConnectionException.MESSAGE);
                    });
              }
            })
        .start();
  }

  private void handleStartResponse(HttpResponse<String> response) {
    btnStart.setDisable(false);
    String message = extractMessage(response.body());
    if (response.statusCode() >= 200 && response.statusCode() < 300) {
      showAlert(Alert.AlertType.INFORMATION, message);
      onBackClicked();
      return;
    }
    showAlert(Alert.AlertType.ERROR, message);
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
