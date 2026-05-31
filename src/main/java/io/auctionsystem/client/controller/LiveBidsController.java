package io.auctionsystem.client.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.auctionsystem.client.pattern.AuctionManager;
import io.auctionsystem.client.pattern.WebSocketClientManager;
import io.auctionsystem.common.Constants;
import io.auctionsystem.common.dto.AuctionItemDTO;
import io.auctionsystem.common.dto.AuctionPriceUpdateDTO;
import io.auctionsystem.common.request.BidRequest;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;

public class LiveBidsController {

  @FXML private VBox rootPane;
  @FXML private Label lblEmptyState;
  @FXML private TableView<AuctionItemDTO> tableParticipating;
  @FXML private TableColumn<AuctionItemDTO, String> colProductName;
  @FXML private TableColumn<AuctionItemDTO, Double> colCurrentPrice;
  @FXML private TableColumn<AuctionItemDTO, String> colEndTime;
  @FXML private VBox participatingListPane;
  @FXML private VBox detailPane;
  @FXML private Label lblProductName;
  @FXML private Label lblCurrentPrice;
  @FXML private Label lblTimeRemaining;
  @FXML private TextField txtBidAmount;
  @FXML private LineChart<String, Number> priceChart;

  public static Long selectedAuctionId;
  public static String selectedAuctionName = "";
  public static Double selectedInitialPrice = 0.0;
  public static String selectedEndTime = "";

  private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final HttpClient httpClient = HttpClient.newHttpClient();
  private final NumberFormat currencyFormat =
      NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));
  private final ObservableList<AuctionItemDTO> participatingAuctions =
      FXCollections.observableArrayList();

  private Long currentAuctionId;
  private XYChart.Series<String, Number> priceSeries;
  private LocalDateTime auctionEndTime;
  private Timeline countdownTimeline;
  private StompSession.Subscription priceSubscription;
  private StompSession.Subscription statusSubscription;
  private StompSession.Subscription extendedSubscription;
  private StompSession.Subscription listPriceSubscription;
  private StompSession.Subscription listChangedSubscription;

  @FXML
  public void initialize() {
    configureParticipatingTable();
    configureChart();
    configureBidInput();
    setDetailVisible(false);

    WebSocketClientManager.getInstance().connect();
    subscribeToParticipatingListUpdates();
    loadParticipatingAuctions();
    openRequestedAuction();

    rootPane
        .sceneProperty()
        .addListener(
            (observable, oldScene, newScene) -> {
              if (newScene == null) {
                stopRealtimeUpdates();
              }
            });
  }

  private void configureParticipatingTable() {
    colProductName.setCellValueFactory(new PropertyValueFactory<>("name"));
    colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
    colEndTime.setCellValueFactory(new PropertyValueFactory<>("endTime"));
    colCurrentPrice.setCellFactory(
        column ->
            new TableCell<>() {
              @Override
              protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? null : currencyFormat.format(price));
              }
            });
    tableParticipating.setItems(participatingAuctions);
    tableParticipating
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (observable, oldValue, selected) -> {
              if (selected != null) {
                openAuctionDetail(selected);
              }
            });
  }

  private void configureChart() {
    priceSeries = new XYChart.Series<>();
    priceSeries.setName("Lịch sử giá");
    priceChart.getData().clear();
    priceChart.getData().add(priceSeries);
    priceChart.setAnimated(false);
  }

  private void configureBidInput() {
    txtBidAmount.setOnKeyPressed(
        event -> {
          if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
            onPlaceBidClicked();
          }
        });
  }

  private void openRequestedAuction() {
    if (selectedAuctionId == null) {
      return;
    }

    AuctionItemDTO requestedAuction = new AuctionItemDTO();
    requestedAuction.setId(selectedAuctionId);
    requestedAuction.setName(selectedAuctionName);
    requestedAuction.setCurrentPrice(selectedInitialPrice);
    requestedAuction.setEndTime(selectedEndTime);
    openAuctionDetail(requestedAuction);

    selectedAuctionId = null;
    selectedAuctionName = "";
    selectedInitialPrice = 0.0;
    selectedEndTime = "";
  }

  private void loadParticipatingAuctions() {
    Long bidderId = AuctionManager.getInstance().getId();
    if (bidderId == null) {
      participatingAuctions.clear();
      updateEmptyState();
      return;
    }

    new Thread(
            () -> {
              try {
                HttpRequest request =
                    HttpRequest.newBuilder()
                        .uri(URI.create(Constants.BASE_URL + "/auctions/participating/" + bidderId))
                        .GET()
                        .build();
                HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                  List<AuctionItemDTO> auctions =
                      objectMapper.readValue(
                          response.body(), new TypeReference<List<AuctionItemDTO>>() {});
                  Platform.runLater(
                      () -> {
                        participatingAuctions.setAll(auctions);
                        updateEmptyState();
                      });
                }
              } catch (Exception e) {
                System.err.println(
                    "Không thể tải danh sách phiên đang tham gia: " + e.getMessage());
              }
            })
        .start();
  }

  private void updateEmptyState() {
    boolean empty = participatingAuctions.isEmpty();
    lblEmptyState.setVisible(empty);
    lblEmptyState.setManaged(empty);
  }

  private void openAuctionDetail(AuctionItemDTO auction) {
    stopAuctionRealtimeUpdates();
    currentAuctionId = auction.getId();
    auctionEndTime = null;
    priceSeries.getData().clear();
    lblProductName.setText(auction.getName());
    lblCurrentPrice.setText(
        currencyFormat.format(auction.getCurrentPrice() == null ? 0.0 : auction.getCurrentPrice()));
    lblTimeRemaining.setText("Thời gian còn lại: --:--:--");
    txtBidAmount.setDisable(false);
    txtBidAmount.clear();
    setDetailVisible(true);
    parseAuctionEndTime(auction.getEndTime());
    startCountdown();
    loadInitialChartData();
    subscribeToAuctionUpdates();
  }

  private void setDetailVisible(boolean visible) {
    participatingListPane.setVisible(!visible);
    participatingListPane.setManaged(!visible);
    detailPane.setVisible(visible);
    detailPane.setManaged(visible);
  }

  @FXML
  public void onPlaceBidClicked() {
    if (currentAuctionId == null) {
      return;
    }
    try {
      double amount = Double.parseDouble(txtBidAmount.getText().trim());
      Long bidderId = AuctionManager.getInstance().getId();
      sendBidToServer(new BidRequest(currentAuctionId, bidderId, amount));
      txtBidAmount.clear();
      txtBidAmount.requestFocus();
    } catch (NumberFormatException e) {
      showAlert("Vui lòng nhập số tiền hợp lệ!");
    }
  }

  private void sendBidToServer(BidRequest requestDto) {
    new Thread(
            () -> {
              try {
                String jsonBody = objectMapper.writeValueAsString(requestDto);
                HttpRequest request =
                    HttpRequest.newBuilder()
                        .uri(URI.create(Constants.BASE_URL + "/bids/place"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                Platform.runLater(
                    () -> {
                      if (response.statusCode() == 200) {
                        loadParticipatingAuctions();
                        showAlert("Đặt giá thành công!");
                      } else {
                        showAlert("Lỗi: " + response.body());
                      }
                    });
              } catch (Exception e) {
                Platform.runLater(() -> showAlert("Không thể kết nối đến máy chủ!"));
              }
            })
        .start();
  }

  private void loadInitialChartData() {
    Long auctionId = currentAuctionId;
    if (auctionId == null) {
      return;
    }

    new Thread(
            () -> {
              try {
                HttpRequest request =
                    HttpRequest.newBuilder()
                        .uri(URI.create(Constants.BASE_URL + "/bids/auction/" + auctionId))
                        .GET()
                        .build();
                HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                  JsonNode bids = objectMapper.readTree(response.body());
                  Platform.runLater(
                      () -> {
                        if (!auctionId.equals(currentAuctionId)) {
                          return;
                        }
                        for (JsonNode bid : bids) {
                          String time =
                              LocalDateTime.parse(bid.get("bidTime").asText())
                                  .toLocalTime()
                                  .format(timeFormatter);
                          priceSeries
                              .getData()
                              .add(new XYChart.Data<>(time, bid.get("amount").asDouble()));
                        }
                      });
                }
              } catch (Exception e) {
                System.err.println("Không thể tải biểu đồ: " + e.getMessage());
              }
            })
        .start();
  }

  private void subscribeToParticipatingListUpdates() {
    StompSession session = WebSocketClientManager.getInstance().getSession();
    if (session == null || !session.isConnected()) {
      return;
    }

    listPriceSubscription =
        session.subscribe(
            "/topic/auctions/prices",
            new StompSessionHandlerAdapter() {
              @Override
              public Type getPayloadType(StompHeaders headers) {
                return AuctionPriceUpdateDTO.class;
              }

              @Override
              public void handleFrame(StompHeaders headers, Object payload) {
                if (payload instanceof AuctionPriceUpdateDTO update) {
                  Platform.runLater(() -> applyParticipatingPriceUpdate(update));
                }
              }
            });

    listChangedSubscription =
        session.subscribe(
            "/topic/auctions/changed",
            new StompSessionHandlerAdapter() {
              @Override
              public Type getPayloadType(StompHeaders headers) {
                return String.class;
              }

              @Override
              public void handleFrame(StompHeaders headers, Object payload) {
                loadParticipatingAuctions();
              }
            });
  }

  private void applyParticipatingPriceUpdate(AuctionPriceUpdateDTO update) {
    if (update.getAuctionId() == null || update.getCurrentPrice() == null) {
      return;
    }
    for (AuctionItemDTO auction : participatingAuctions) {
      if (update.getAuctionId().equals(auction.getId())) {
        auction.setCurrentPrice(update.getCurrentPrice());
        tableParticipating.refresh();
        return;
      }
    }
  }

  private void subscribeToAuctionUpdates() {
    Long auctionId = currentAuctionId;
    StompSession session = WebSocketClientManager.getInstance().getSession();
    if (auctionId == null || session == null || !session.isConnected()) {
      return;
    }

    priceSubscription =
        session.subscribe(
            "/topic/bids/" + auctionId,
            new StompSessionHandlerAdapter() {
              @Override
              public Type getPayloadType(StompHeaders headers) {
                return Double.class;
              }

              @Override
              public void handleFrame(StompHeaders headers, Object payload) {
                if (!(payload instanceof Number price) || !auctionId.equals(currentAuctionId)) {
                  return;
                }

                double newPrice = price.doubleValue();
                String currentTime = LocalTime.now().format(timeFormatter);
                Platform.runLater(
                    () -> {
                      lblCurrentPrice.setText(currencyFormat.format(newPrice));
                      priceSeries.getData().add(new XYChart.Data<>(currentTime, newPrice));
                    });
              }
            });

    statusSubscription =
        session.subscribe(
            "/topic/auctions/" + auctionId + "/status",
            new StompSessionHandlerAdapter() {
              @Override
              public Type getPayloadType(StompHeaders headers) {
                return String.class;
              }

              @Override
              public void handleFrame(StompHeaders headers, Object payload) {
                if (payload != null
                    && payload.toString().contains("CLOSED")
                    && auctionId.equals(currentAuctionId)) {
                  Platform.runLater(
                      () -> {
                        lblTimeRemaining.setText("Phiên đấu giá đã kết thúc");
                        txtBidAmount.setDisable(true);
                        if (countdownTimeline != null) {
                          countdownTimeline.stop();
                        }
                        returnToProductList();
                      });
                }
              }
            });

    extendedSubscription =
        session.subscribe(
            "/topic/auctions/" + auctionId + "/extended",
            new StompSessionHandlerAdapter() {
              @Override
              public Type getPayloadType(StompHeaders headers) {
                return String.class;
              }

              @Override
              public void handleFrame(StompHeaders headers, Object payload) {
                if (payload != null && auctionId.equals(currentAuctionId)) {
                  Platform.runLater(
                      () -> parseAuctionEndTime(payload.toString().replace("\"", "")));
                }
              }
            });
  }

  private void stopRealtimeUpdates() {
    stopAuctionRealtimeUpdates();
    if (listPriceSubscription != null) {
      listPriceSubscription.unsubscribe();
      listPriceSubscription = null;
    }
    if (listChangedSubscription != null) {
      listChangedSubscription.unsubscribe();
      listChangedSubscription = null;
    }
  }

  private void returnToProductList() {
    currentAuctionId = null;
    BidderDashboardController dashboard = BidderDashboardController.getInstance();
    if (dashboard != null) {
      dashboard.onProductListButtonClicked();
    }
  }

  private void stopAuctionRealtimeUpdates() {
    if (priceSubscription != null) {
      priceSubscription.unsubscribe();
      priceSubscription = null;
    }
    if (statusSubscription != null) {
      statusSubscription.unsubscribe();
      statusSubscription = null;
    }
    if (extendedSubscription != null) {
      extendedSubscription.unsubscribe();
      extendedSubscription = null;
    }
    if (countdownTimeline != null) {
      countdownTimeline.stop();
      countdownTimeline = null;
    }
  }

  private void parseAuctionEndTime(String value) {
    if (value == null || value.isBlank()) {
      return;
    }
    try {
      auctionEndTime =
          value.contains("T")
              ? LocalDateTime.parse(value)
              : LocalDateTime.parse(value, DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
      updateCountdown();
    } catch (Exception ignored) {
      lblTimeRemaining.setText("Thời gian còn lại: --:--:--");
    }
  }

  private void startCountdown() {
    if (countdownTimeline != null) {
      countdownTimeline.stop();
    }
    countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> updateCountdown()));
    countdownTimeline.setCycleCount(Timeline.INDEFINITE);
    countdownTimeline.play();
    updateCountdown();
  }

  private void updateCountdown() {
    if (auctionEndTime == null) {
      return;
    }
    long seconds = java.time.Duration.between(LocalDateTime.now(), auctionEndTime).getSeconds();
    if (seconds <= 0) {
      lblTimeRemaining.setText("Đang chờ hệ thống kết toán...");
      txtBidAmount.setDisable(true);
      return;
    }

    long hours = seconds / 3600;
    long minutes = (seconds % 3600) / 60;
    long remainingSeconds = seconds % 60;
    lblTimeRemaining.setText(
        String.format("Thời gian còn lại: %02d:%02d:%02d", hours, minutes, remainingSeconds));
  }

  private void showAlert(String message) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }
}
