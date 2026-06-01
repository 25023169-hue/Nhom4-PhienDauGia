package io.auctionsystem.client.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.auctionsystem.client.model.TransactionModel;
import io.auctionsystem.client.pattern.AuctionManager;
import io.auctionsystem.client.pattern.ClientHttp;
import io.auctionsystem.common.Constants;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class SellerRevenueStatisticsController {

  @FXML private Label lblTotalRevenue;
  @FXML private Label lblMonthRevenue;
  @FXML private Label lblSoldOrders;
  @FXML private Label lblAverageOrderValue;
  @FXML private Label lblRevenueError;
  @FXML private BarChart<String, Number> chartRevenue;
  @FXML private TableView<TransactionModel> tableRecentSales;
  @FXML private TableColumn<TransactionModel, Long> colSaleId;
  @FXML private TableColumn<TransactionModel, String> colSaleTime;
  @FXML private TableColumn<TransactionModel, String> colSaleAmount;
  @FXML private TableColumn<TransactionModel, String> colSaleBalance;
  @FXML private TableColumn<TransactionModel, String> colSaleNote;

  private static final NumberFormat VND_FORMAT =
      NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));
  private static final DateTimeFormatter DISPLAY_TIME_FORMAT =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
  private final ObservableList<TransactionModel> recentSales = FXCollections.observableArrayList();
  private final HttpClient httpClient = ClientHttp.client();
  private final ObjectMapper objectMapper = ClientHttp.mapper();

  @FXML
  public void initialize() {
    colSaleId.setCellValueFactory(new PropertyValueFactory<>("id"));
    colSaleTime.setCellValueFactory(new PropertyValueFactory<>("time"));
    colSaleAmount.setCellValueFactory(new PropertyValueFactory<>("moneyIn"));
    colSaleBalance.setCellValueFactory(new PropertyValueFactory<>("lastBalance"));
    colSaleNote.setCellValueFactory(new PropertyValueFactory<>("note"));
    tableRecentSales.setItems(recentSales);

    chartRevenue.setAnimated(false);
    chartRevenue.setLegendVisible(false);
    loadRevenueStats();
  }

  private void loadRevenueStats() {
    Long sellerId = AuctionManager.getInstance().getId();
    if (sellerId == null) {
      showError("Không tìm thấy tài khoản seller hiện tại.");
      return;
    }

    new Thread(
            () -> {
              try {
                HttpRequest request =
                    HttpRequest.newBuilder()
                        .uri(
                            URI.create(Constants.BASE_URL + "/user/" + sellerId + "/revenue-stats"))
                        .GET()
                        .build();
                HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                Platform.runLater(
                    () -> {
                      if (response.statusCode() == 200) {
                        renderRevenueStats(response.body());
                      } else {
                        showError("Không thể tải thống kê doanh thu: " + response.body());
                      }
                    });
              } catch (Exception e) {
                Platform.runLater(() -> showError("Lỗi kết nối máy chủ: " + e.getMessage()));
              }
            })
        .start();
  }

  private void renderRevenueStats(String responseBody) {
    try {
      JsonNode root = objectMapper.readTree(responseBody);
      lblTotalRevenue.setText(formatCurrency(root.path("totalRevenue").asDouble(0.0)));
      lblMonthRevenue.setText(formatCurrency(root.path("monthRevenue").asDouble(0.0)));
      lblSoldOrders.setText(String.valueOf(root.path("soldOrders").asLong(0)));
      lblAverageOrderValue.setText(formatCurrency(root.path("averageOrderValue").asDouble(0.0)));

      renderChart(
          objectMapper.convertValue(
              root.path("monthlyRevenue"), new TypeReference<List<Map<String, Object>>>() {}));
      renderRecentSales(root.path("recentSales"));
      hideError();
    } catch (Exception e) {
      showError("Dữ liệu thống kê không hợp lệ: " + e.getMessage());
    }
  }

  private void renderChart(List<Map<String, Object>> points) {
    XYChart.Series<String, Number> series = new XYChart.Series<>();
    for (Map<String, Object> point : points) {
      String month = String.valueOf(point.getOrDefault("timestamp", ""));
      double revenue = ((Number) point.getOrDefault("price", 0.0)).doubleValue();
      series.getData().add(new XYChart.Data<>(month, revenue));
    }
    chartRevenue.getData().clear();
    chartRevenue.getData().add(series);
  }

  private void renderRecentSales(JsonNode salesNode) {
    recentSales.clear();
    if (salesNode == null || !salesNode.isArray()) {
      return;
    }

    for (JsonNode sale : salesNode) {
      LocalDateTime time = parseTime(sale.path("transactionTime").asText(null));
      double moneyIn = sale.path("moneyIn").asDouble(0.0);
      double balance = sale.path("lastBalance").asDouble(0.0);
      String note = sale.path("note").asText("");
      recentSales.add(
          new TransactionModel(
              sale.path("id").asLong(),
              time,
              time.format(DISPLAY_TIME_FORMAT),
              "+ " + formatCurrency(moneyIn),
              "",
              formatCurrency(balance),
              sale.path("type").asText("Thu nhập bán hàng"),
              note));
    }
  }

  private LocalDateTime parseTime(String value) {
    if (value == null || value.isBlank()) {
      return LocalDateTime.now();
    }
    return LocalDateTime.parse(value);
  }

  private String formatCurrency(double amount) {
    return VND_FORMAT.format(amount);
  }

  private void showError(String message) {
    lblRevenueError.setText(message);
    lblRevenueError.setVisible(true);
    lblRevenueError.setManaged(true);
  }

  private void hideError() {
    lblRevenueError.setVisible(false);
    lblRevenueError.setManaged(false);
  }
}
