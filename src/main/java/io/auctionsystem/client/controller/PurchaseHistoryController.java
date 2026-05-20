package io.auctionsystem.client.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.auctionsystem.client.pattern.AuctionManager;
import io.auctionsystem.server.model.Bid;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class PurchaseHistoryController {
    @FXML private TableView<Bid> tableHistory;
    @FXML private TableColumn<Bid, Long> colAuctionId;
    @FXML private TableColumn<Bid, Double> colAmount;

    // Đổi kiểu String sang LocalDateTime để format cho chuẩn
    @FXML private TableColumn<Bid, LocalDateTime> colTime;

    private final ObservableList<Bid> historyList = FXCollections.observableArrayList();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // --- PHẦN THÊM MỚI: Bộ định dạng Tiền tệ và Thời gian ---
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss - dd/MM/yyyy");

    @FXML
    public void initialize() {
        objectMapper.registerModule(new JavaTimeModule());

        colAuctionId.setCellValueFactory(new PropertyValueFactory<>("auctionId"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("bidTime"));

        // --- PHẦN THÊM MỚI 1: Format hiển thị Tiền VNĐ ---
        colAmount.setCellFactory(tc -> new TableCell<Bid, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(currencyFormat.format(price));
                }
            }
        });

        // --- PHẦN THÊM MỚI 2: Format hiển thị Thời gian thân thiện ---
        colTime.setCellFactory(tc -> new TableCell<Bid, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime time, boolean empty) {
                super.updateItem(time, empty);
                if (empty || time == null) {
                    setText(null);
                } else {
                    setText(timeFormatter.format(time));
                }
            }
        });

        tableHistory.setItems(historyList);
        loadHistory();
    }

    private void loadHistory() {
        Long bidderId = AuctionManager.getInstance().getId();
        if (bidderId == null) return;

        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/bids/history/" + bidderId))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        try {
                            List<Bid> bids = objectMapper.readValue(response.body(), new TypeReference<List<Bid>>() {});
                            historyList.setAll(bids);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Lỗi kết nối máy chủ");
                    alert.show();
                });
            }
        }).start();
    }
}