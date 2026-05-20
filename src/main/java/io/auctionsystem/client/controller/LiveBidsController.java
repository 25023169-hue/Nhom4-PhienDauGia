package io.auctionsystem.client.controller;

import io.auctionsystem.client.pattern.AuctionManager;
import io.auctionsystem.client.pattern.WebSocketClientManager;
import io.auctionsystem.common.request.BidRequest;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.NumberFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LiveBidsController {

    @FXML private Label lblProductName;
    @FXML private Label lblCurrentPrice;
    @FXML private TextField txtBidAmount;
    @FXML private LineChart<String, Number> priceChart;

    @FXML private TextField txtAutoMaxPrice;
    @FXML private TextField txtAutoIncrement;

    public static Long selectedAuctionId = null;
    public static String selectedAuctionName = "";
    public static Double selectedInitialPrice = 0.0;

    private Long currentAuctionId = 1L;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));
    private XYChart.Series<String, Number> priceSeries;

    @FXML
    public void initialize() {
        if (selectedAuctionId != null) {
            currentAuctionId = selectedAuctionId;
            lblProductName.setText(selectedAuctionName);
            lblCurrentPrice.setText(currencyFormat.format(selectedInitialPrice));
        }

        if (priceChart != null) {
            priceSeries = new XYChart.Series<>();
            priceSeries.setName("Lịch sử giá");
            priceChart.getData().add(priceSeries);
            priceChart.setAnimated(false);
            loadInitialChartData();
        }

        // --- PHẦN THÊM MỚI 1: Bắt sự kiện nhấn phím ENTER để đặt giá siêu tốc ---
        if (txtBidAmount != null) {
            txtBidAmount.setOnKeyPressed(event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                    onPlaceBidClicked();
                }
            });
        }
        // -------------------------------------------------------------------------

        WebSocketClientManager.getInstance().connect();
        subscribeToAuctionUpdates();
    }

    @FXML
    public void onEnableAutoBidClicked() {
        try {
            double maxPrice = Double.parseDouble(txtAutoMaxPrice.getText().trim());
            double increment = Double.parseDouble(txtAutoIncrement.getText().trim());
            Long bidderId = AuctionManager.getInstance().getId();

            Map<String, Object> payload = new HashMap<>();
            payload.put("auctionId", currentAuctionId);
            payload.put("bidderId", bidderId);
            payload.put("maxAmount", maxPrice);
            payload.put("incrementAmount", increment);

            new Thread(() -> {
                try {
                    String jsonBody = objectMapper.writeValueAsString(payload);
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:8080/api/autobids/register"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                            .build();

                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    Platform.runLater(() -> showAlert(response.body()));
                } catch (Exception e) {
                    Platform.runLater(() -> showAlert("Lỗi kết nối Server khi bật Auto-Bid!"));
                }
            }).start();

        } catch (NumberFormatException e) {
            showAlert("Vui lòng nhập số tiền hợp lệ vào khung Auto-Bid!");
        }
    }

    @FXML
    public void onPlaceBidClicked() {
        if (currentAuctionId == null) return;
        try {
            double amount = Double.parseDouble(txtBidAmount.getText().trim());
            Long bidderId = AuctionManager.getInstance().getId();
            BidRequest bidRequest = new BidRequest(currentAuctionId, bidderId, amount);
            sendBidToServer(bidRequest);

            txtBidAmount.clear();
            // --- PHẦN THÊM MỚI 2: Tự động khóa trỏ chuột lại vào ô nhập sau khi đặt ---
            txtBidAmount.requestFocus();

        } catch (NumberFormatException e) {
            showAlert("Vui lòng nhập số tiền hợp lệ!");
        }
    }

    private void sendBidToServer(BidRequest requestDto) {
        new Thread(() -> {
            try {
                String jsonBody = objectMapper.writeValueAsString(requestDto);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/bids/place"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        // --- PHẦN THÊM MỚI 3: Hiển thị bảng báo đặt giá thành công ---
                        showAlert("Đặt giá thành công!");
                    } else {
                        showAlert("Lỗi: " + response.body());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Không thể kết nối đến máy chủ!"));
            }
        }).start();
    }

    private void loadInitialChartData() {
        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/bids/auction/" + currentAuctionId))
                        .GET()
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    JsonNode bids = objectMapper.readTree(response.body());
                    Platform.runLater(() -> {
                        for (JsonNode bid : bids) {
                            String timeStr = bid.get("bidTime").asText();
                            String shortTime = LocalTime.parse(timeStr.substring(11, 19)).format(timeFormatter);
                            double amount = bid.get("amount").asDouble();
                            priceSeries.getData().add(new XYChart.Data<>(shortTime, amount));
                        }
                    });
                }
            } catch (Exception e) {
                System.err.println("Không thể tải biểu đồ: " + e.getMessage());
            }
        }).start();
    }

    private void subscribeToAuctionUpdates() {
        StompSession session = WebSocketClientManager.getInstance().getSession();
        if (session != null && session.isConnected()) {
            String topicUrl = "/topic/bids/" + currentAuctionId;
            session.subscribe(topicUrl, new StompSessionHandlerAdapter() {
                @Override
                public Type getPayloadType(StompHeaders headers) { return String.class; }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    if (payload != null) {
                        try {
                            double newPrice = Double.parseDouble(payload.toString());
                            String formattedPrice = currencyFormat.format(newPrice);
                            String currentTime = LocalTime.now().format(timeFormatter);
                            Platform.runLater(() -> {
                                lblCurrentPrice.setText(formattedPrice);
                                if (priceChart != null) {
                                    priceSeries.getData().add(new XYChart.Data<>(currentTime, newPrice));
                                }
                            });
                        } catch (NumberFormatException e) { e.printStackTrace(); }
                    }
                }
            });
        }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}