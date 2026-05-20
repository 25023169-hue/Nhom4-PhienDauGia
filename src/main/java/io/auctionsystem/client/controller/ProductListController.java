package io.auctionsystem.client.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.auctionsystem.common.dto.AuctionItemDTO;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ProductListController {

    @FXML private TableView<AuctionItemDTO> tableItems;
    @FXML private TableColumn<AuctionItemDTO, Long> colId;
    @FXML private TableColumn<AuctionItemDTO, String> colName;
    @FXML private TableColumn<AuctionItemDTO, Double> colCurrentPrice;
    @FXML private TableColumn<AuctionItemDTO, String> colEndTime;

    // --- PHẦN KHAI BÁO THÊM MỚI ---
    private final ObservableList<AuctionItemDTO> auctionList = FXCollections.observableArrayList();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));

    @FXML
    public void initialize() {
        // 1. Gắn dữ liệu vào các cột (Code cũ giữ nguyên)
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colEndTime.setCellValueFactory(new PropertyValueFactory<>("endTime"));

        // 2. Định dạng cột giá tiền sang chuẩn VNĐ (Code thêm mới)
        colCurrentPrice.setCellFactory(tc -> new TableCell<AuctionItemDTO, Double>() {
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

        // 3. Xử lý sự kiện nháy đúp chuột (Double-click) để vào phòng đấu giá (Code thêm mới)
        tableItems.setOnMouseClicked((MouseEvent event) -> {
            if (event.getClickCount() == 2) {
                AuctionItemDTO selected = tableItems.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    // Truyền dữ liệu sang màn hình LiveBids
                    LiveBidsController.selectedAuctionId = selected.getId();
                    LiveBidsController.selectedAuctionName = selected.getName();
                    LiveBidsController.selectedInitialPrice = selected.getCurrentPrice();

                    // Chuyển tab
                    BidderDashboardController.getInstance().onLiveBidsClicked();
                }
            }
        });

        // 4. Load dữ liệu
        tableItems.setItems(auctionList);
        loadProductsFromServer();
    }

    // --- CÁC HÀM XỬ LÝ API THÊM MỚI ---
    private void loadProductsFromServer() {
        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/auctions/running"))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        try {
                            List<AuctionItemDTO> items = objectMapper.readValue(
                                    response.body(),
                                    new TypeReference<List<AuctionItemDTO>>() {}
                            );
                            auctionList.setAll(items);
                        } catch (Exception e) {
                            System.err.println("Lỗi Parse JSON danh sách sản phẩm.");
                        }
                    } else {
                        showAlert("Không thể tải danh sách sản phẩm lúc này!");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Lỗi kết nối đến máy chủ khi tải sản phẩm!"));
            }
        }).start();
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}