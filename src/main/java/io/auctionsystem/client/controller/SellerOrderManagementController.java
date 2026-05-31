package io.auctionsystem.client.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.auctionsystem.client.pattern.AuctionManager;
import io.auctionsystem.client.pattern.WebSocketClientManager;
import io.auctionsystem.common.Constants;
import io.auctionsystem.common.dto.AuctionItemDTO;
import io.auctionsystem.common.dto.AuctionPriceUpdateDTO;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;

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

public class SellerOrderManagementController {

    @FXML private VBox rootPane;
    @FXML private Label lblEmptyState;
    @FXML private TableView<AuctionItemDTO> tableOrders;
    @FXML private TableColumn<AuctionItemDTO, String> colProductName;
    @FXML private TableColumn<AuctionItemDTO, Double> colCurrentPrice;
    @FXML private TableColumn<AuctionItemDTO, String> colEndTime;
    @FXML private VBox detailPane;
    @FXML private Label lblProductName;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblTimeRemaining;
    @FXML private LineChart<String, Number> priceChart;

    private final ObservableList<AuctionItemDTO> runningOrders = FXCollections.observableArrayList();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));

    private Long currentAuctionId;
    private LocalDateTime auctionEndTime;
    private Timeline countdownTimeline;
    private XYChart.Series<String, Number> priceSeries;
    private StompSession.Subscription listPriceSubscription;
    private StompSession.Subscription listChangedSubscription;
    private StompSession.Subscription priceSubscription;
    private StompSession.Subscription statusSubscription;
    private StompSession.Subscription extendedSubscription;

    @FXML
    public void initialize() {
        configureTable();
        configureChart();
        setDetailVisible(false);

        WebSocketClientManager.getInstance().connect();
        subscribeToListUpdates();
        loadOrders();

        rootPane.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene == null) {
                stopRealtimeUpdates();
            }
        });
    }

    private void configureTable() {
        colProductName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colEndTime.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        colCurrentPrice.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? null : currencyFormat.format(price));
            }
        });
        tableOrders.setItems(runningOrders);
        tableOrders.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, selected) -> {
                    if (selected != null) {
                        openOrderDetail(selected);
                    }
                }
        );
    }

    private void configureChart() {
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Lịch sử giá");
        priceChart.getData().clear();
        priceChart.getData().add(priceSeries);
        priceChart.setAnimated(false);
    }

    private void loadOrders() {
        Long sellerId = AuctionManager.getInstance().getId();
        if (sellerId == null) {
            runningOrders.clear();
            updateEmptyState();
            return;
        }

        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(Constants.BASE_URL + "/auctions/running/seller/" + sellerId))
                        .GET()
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    List<AuctionItemDTO> orders = objectMapper.readValue(
                            response.body(),
                            new TypeReference<List<AuctionItemDTO>>() {}
                    );
                    Platform.runLater(() -> {
                        runningOrders.setAll(orders);
                        updateEmptyState();
                    });
                }
            } catch (Exception e) {
                System.err.println("Không thể tải danh sách đơn hàng seller: " + e.getMessage());
            }
        }).start();
    }

    private void updateEmptyState() {
        boolean empty = runningOrders.isEmpty();
        lblEmptyState.setVisible(empty);
        lblEmptyState.setManaged(empty);
    }

    private void openOrderDetail(AuctionItemDTO auction) {
        stopAuctionRealtimeUpdates();
        currentAuctionId = auction.getId();
        auctionEndTime = null;
        priceSeries.getData().clear();
        lblProductName.setText(auction.getName());
        lblCurrentPrice.setText(currencyFormat.format(auction.getCurrentPrice() == null ? 0.0 : auction.getCurrentPrice()));
        lblTimeRemaining.setText("Thời gian còn lại: --:--:--");
        setDetailVisible(true);
        parseAuctionEndTime(auction.getEndTime());
        startCountdown();
        loadInitialChartData();
        subscribeToAuctionUpdates();
    }

    private void setDetailVisible(boolean visible) {
        detailPane.setVisible(visible);
        detailPane.setManaged(visible);
    }

    private void loadInitialChartData() {
        Long auctionId = currentAuctionId;
        if (auctionId == null) {
            return;
        }

        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(Constants.BASE_URL + "/bids/auction/" + auctionId))
                        .GET()
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    JsonNode bids = objectMapper.readTree(response.body());
                    Platform.runLater(() -> {
                        if (!auctionId.equals(currentAuctionId)) {
                            return;
                        }
                        for (JsonNode bid : bids) {
                            String time = LocalDateTime.parse(bid.get("bidTime").asText())
                                    .toLocalTime()
                                    .format(timeFormatter);
                            priceSeries.getData().add(new XYChart.Data<>(time, bid.get("amount").asDouble()));
                        }
                    });
                }
            } catch (Exception e) {
                System.err.println("Không thể tải biểu đồ seller: " + e.getMessage());
            }
        }).start();
    }

    private void subscribeToListUpdates() {
        StompSession session = WebSocketClientManager.getInstance().getSession();
        if (session == null || !session.isConnected()) {
            return;
        }

        listPriceSubscription = session.subscribe("/topic/auctions/prices", new StompSessionHandlerAdapter() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return AuctionPriceUpdateDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                if (payload instanceof AuctionPriceUpdateDTO update) {
                    Platform.runLater(() -> applyListPriceUpdate(update));
                }
            }
        });

        listChangedSubscription = session.subscribe("/topic/auctions/changed", new StompSessionHandlerAdapter() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                loadOrders();
            }
        });
    }

    private void applyListPriceUpdate(AuctionPriceUpdateDTO update) {
        if (update.getAuctionId() == null || update.getCurrentPrice() == null) {
            return;
        }
        for (AuctionItemDTO order : runningOrders) {
            if (update.getAuctionId().equals(order.getId())) {
                order.setCurrentPrice(update.getCurrentPrice());
                tableOrders.refresh();
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

        priceSubscription = session.subscribe("/topic/bids/" + auctionId, new StompSessionHandlerAdapter() {
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
                Platform.runLater(() -> {
                    lblCurrentPrice.setText(currencyFormat.format(newPrice));
                    priceSeries.getData().add(new XYChart.Data<>(LocalTime.now().format(timeFormatter), newPrice));
                });
            }
        });

        statusSubscription = session.subscribe("/topic/auctions/" + auctionId + "/status", new StompSessionHandlerAdapter() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                if (payload != null && payload.toString().contains("CLOSED") && auctionId.equals(currentAuctionId)) {
                    Platform.runLater(() -> {
                        stopAuctionRealtimeUpdates();
                        currentAuctionId = null;
                        setDetailVisible(false);
                        loadOrders();
                    });
                }
            }
        });

        extendedSubscription = session.subscribe("/topic/auctions/" + auctionId + "/extended", new StompSessionHandlerAdapter() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                if (payload != null && auctionId.equals(currentAuctionId)) {
                    Platform.runLater(() -> parseAuctionEndTime(payload.toString().replace("\"", "")));
                }
            }
        });
    }

    private void parseAuctionEndTime(String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        try {
            auctionEndTime = value.contains("T")
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
            return;
        }

        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;
        lblTimeRemaining.setText(String.format("Thời gian còn lại: %02d:%02d:%02d", hours, minutes, remainingSeconds));
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
}
