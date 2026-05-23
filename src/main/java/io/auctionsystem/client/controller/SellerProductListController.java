package io.auctionsystem.client.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.auctionsystem.client.pattern.AuctionManager;
import io.auctionsystem.common.Constants;
import io.auctionsystem.common.dto.SellerProductDTO;
import io.auctionsystem.common.enums.AuctionState;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class SellerProductListController {

    private static final String ALL_STATUS = "Tất cả trạng thái";

    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbStatus;
    @FXML private Label lblCount;
    @FXML private Label lblOpenCount;
    @FXML private Label lblFinishedCount;
    @FXML private TableView<SellerProductDTO> tableProducts;
    @FXML private TableColumn<SellerProductDTO, String> colItemName;
    @FXML private TableColumn<SellerProductDTO, Double> colStartingPrice;
    @FXML private TableColumn<SellerProductDTO, String> colStatus;
    @FXML private TableColumn<SellerProductDTO, Void> colAction;

    private final ObservableList<SellerProductDTO> products = FXCollections.observableArrayList();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));
    private final PauseTransition searchDelay = new PauseTransition(Duration.millis(350));

    @FXML
    public void initialize() {
        colItemName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colStartingPrice.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));
        colStatus.setCellValueFactory(cellData -> new SimpleStringProperty(formatStatus(cellData.getValue().getStatus())));

        colStartingPrice.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? null : currencyFormat.format(price));
            }
        });

        configureStatusFilter();
        configureActionColumn();
        configureSearch();

        tableProducts.setItems(products);
        loadProducts();
    }

    @FXML
    public void onAddProductClicked() {
        SellerDashboardController controller = SellerDashboardController.getInstance();
        if (controller != null) {
            controller.onAddAuctionClicked();
        }
    }

    private void configureSearch() {
        txtSearch.setOnAction(event -> loadProducts());
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            searchDelay.setOnFinished(event -> loadProducts());
            searchDelay.playFromStart();
        });
    }

    private void configureStatusFilter() {
        cbStatus.getItems().setAll(ALL_STATUS);
        for (AuctionState state : AuctionState.values()) {
            cbStatus.getItems().add(state.name());
        }
        cbStatus.getSelectionModel().select(ALL_STATUS);
        cbStatus.setOnAction(event -> loadProducts());
    }

    private void configureActionColumn() {
        colAction.setCellFactory(column -> new TableCell<>() {
            private final Button detailButton = new Button("Xem");

            {
                detailButton.setStyle("-fx-background-color: #2c7be5; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
                detailButton.setOnAction(event -> {
                    SellerProductDTO product = getTableView().getItems().get(getIndex());
                    showProductDetail(product);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : detailButton);
            }
        });
    }

    private void loadProducts() {
        Long sellerId = AuctionManager.getInstance().getId();
        if (sellerId == null) {
            showAlert(Alert.AlertType.ERROR, "Không tìm thấy seller hiện tại. Vui lòng đăng nhập lại.");
            return;
        }

        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(buildListUrl(sellerId)))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                Platform.runLater(() -> handleLoadResponse(response));
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Không thể kết nối đến server khi tải sản phẩm Seller."));
            }
        }).start();
    }

    private String buildListUrl(Long sellerId) {
        StringBuilder url = new StringBuilder(Constants.BASE_URL)
                .append("/seller/products?sellerId=")
                .append(sellerId);

        String keyword = txtSearch.getText();
        if (keyword != null && !keyword.trim().isEmpty()) {
            url.append("&keyword=").append(URLEncoder.encode(keyword.trim(), StandardCharsets.UTF_8));
        }

        String status = cbStatus.getValue();
        if (status != null && !ALL_STATUS.equals(status)) {
            url.append("&status=").append(URLEncoder.encode(status, StandardCharsets.UTF_8));
        }
        return url.toString();
    }

    private void handleLoadResponse(HttpResponse<String> response) {
        if (response.statusCode() == 200) {
            try {
                List<SellerProductDTO> loadedProducts = objectMapper.readValue(
                        response.body(),
                        new TypeReference<List<SellerProductDTO>>() {}
                );
                products.setAll(loadedProducts);
                updateSummary(loadedProducts);
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Không đọc được dữ liệu sản phẩm của Seller.");
            }
            return;
        }

        showAlert(Alert.AlertType.ERROR, extractMessage(response.body()));
    }

    private void showProductDetail(SellerProductDTO product) {
        String detail = "Tên sản phẩm: " + nullToEmpty(product.getItemName()) + "\n"
                + "Mã sản phẩm: " + product.getItemId() + "\n"
                + "Loại: " + nullToEmpty(product.getItemType()) + "\n"
                + "Giá khởi điểm: " + currencyFormat.format(product.getStartingPrice()) + "\n"
                + "Bước giá: " + currencyFormat.format(product.getBidIncrement()) + "\n"
                + "Giá mua đứt: " + (product.getBuyNowPrice() == null ? "Không có" : currencyFormat.format(product.getBuyNowPrice())) + "\n"
                + "Trạng thái: " + formatStatus(product.getStatus()) + "\n"
                + "Bắt đầu: " + nullToEmpty(product.getStartTime()) + "\n"
                + "Kết thúc: " + nullToEmpty(product.getEndTime());
        showAlert(Alert.AlertType.INFORMATION, detail);
    }

    private void updateSummary(List<SellerProductDTO> loadedProducts) {
        long openCount = loadedProducts.stream()
                .filter(product -> product.getStatus() == AuctionState.OPEN)
                .count();
        long finishedCount = loadedProducts.stream()
                .filter(product -> product.getStatus() == AuctionState.FINISHED)
                .count();

        lblCount.setText("(" + loadedProducts.size() + " items)");
        lblOpenCount.setText("OPEN: " + openCount);
        lblFinishedCount.setText("FINISHED: " + finishedCount);
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

    private String formatStatus(AuctionState status) {
        return status == null ? "UNKNOWN" : status.name();
    }

    private String nullToEmpty(Object value) {
        return value == null ? "" : value.toString();
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
