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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SellerProductListController {

    private static final List<SellerProductDTO> pendingCreatedProducts = new ArrayList<>();

    private static final String ALL_STATUS = "Tất cả trạng thái";

    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbStatus;
    @FXML private Label lblCount;
    @FXML private Label lblOpenCount;
    @FXML private Label lblRunningCount;
    @FXML private Label lblFinishedCount;
    @FXML private Label lblPaidCount;
    @FXML private Label lblCancelledCount;
    @FXML private TableView<SellerProductDTO> tableProducts;
    @FXML private TableColumn<SellerProductDTO, String> colItemName;
    @FXML private TableColumn<SellerProductDTO, Double> colStartingPrice;
    @FXML private TableColumn<SellerProductDTO, String> colStatus;

    private final ObservableList<SellerProductDTO> products = FXCollections.observableArrayList();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));
    private final PauseTransition searchDelay = new PauseTransition(Duration.millis(350));

    public static void rememberCreatedProduct(SellerProductDTO product) {
        if (product == null) {
            return;
        }

        synchronized (pendingCreatedProducts) {
            pendingCreatedProducts.removeIf(pending -> sameProduct(pending, product));
            pendingCreatedProducts.add(0, product);
        }
    }

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
        configureProductSelection();
        configureSearch();

        tableProducts.setItems(products);
        showPendingCreatedProducts();
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

    private void configureProductSelection() {
        tableProducts.setRowFactory(table -> {
            TableRow<SellerProductDTO> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty()) {
                    SellerDashboardController controller = SellerDashboardController.getInstance();
                    if (controller != null) {
                        controller.onProductClicked(row.getItem());
                    }
                }
            });
            return row;
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
                applyLoadedProducts(loadedProducts);
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Không đọc được dữ liệu sản phẩm của Seller.");
            }
            return;
        }

        showAlert(Alert.AlertType.ERROR, extractMessage(response.body()));
    }

    private void updateSummary(List<SellerProductDTO> loadedProducts) {
        long openCount = loadedProducts.stream()
                .filter(product -> product.getStatus() == AuctionState.OPEN)
                .count();
        long runningCount = loadedProducts.stream()
                .filter(product -> product.getStatus() == AuctionState.RUNNING)
                .count();
        long finishedCount = loadedProducts.stream()
                .filter(product -> product.getStatus() == AuctionState.FINISHED)
                .count();
        long paidCount = loadedProducts.stream()
                .filter(product -> product.getStatus() == AuctionState.PAID)
                .count();
        long cancelledCount = loadedProducts.stream()
                .filter(product -> product.getStatus() == AuctionState.CANCELLED)
                .count();

        lblCount.setText("(" + loadedProducts.size() + " items)");
        lblOpenCount.setText("OPEN: " + openCount);
        lblRunningCount.setText("RUNNING: " + runningCount);
        lblFinishedCount.setText("FINISHED: " + finishedCount);
        lblPaidCount.setText("PAID: " + paidCount);
        lblCancelledCount.setText("CANCELLED: " + cancelledCount);
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

    private void showPendingCreatedProducts() {
        List<SellerProductDTO> pendingProducts = pendingSnapshot();
        pendingProducts.removeIf(product -> !matchesCurrentFilters(product));
        if (!pendingProducts.isEmpty()) {
            products.setAll(pendingProducts);
            updateSummary(products);
        }
    }

    private void applyLoadedProducts(List<SellerProductDTO> loadedProducts) {
        List<SellerProductDTO> mergedProducts = new ArrayList<>(loadedProducts);

        synchronized (pendingCreatedProducts) {
            pendingCreatedProducts.removeIf(pending ->
                    loadedProducts.stream().anyMatch(loaded -> sameProduct(loaded, pending)));
            for (int i = pendingCreatedProducts.size() - 1; i >= 0; i--) {
                SellerProductDTO pending = pendingCreatedProducts.get(i);
                if (matchesCurrentFilters(pending)
                        && mergedProducts.stream().noneMatch(loaded -> sameProduct(loaded, pending))) {
                    mergedProducts.add(0, pending);
                }
            }
        }

        products.setAll(mergedProducts);
        updateSummary(mergedProducts);
    }

    private List<SellerProductDTO> pendingSnapshot() {
        synchronized (pendingCreatedProducts) {
            return new ArrayList<>(pendingCreatedProducts);
        }
    }

    private boolean matchesCurrentFilters(SellerProductDTO product) {
        String status = cbStatus.getValue();
        if (status != null && !ALL_STATUS.equals(status)
                && (product.getStatus() == null || !status.equals(product.getStatus().name()))) {
            return false;
        }

        String keyword = txtSearch.getText();
        if (keyword == null || keyword.trim().isEmpty()) {
            return true;
        }

        String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
        String itemName = product.getItemName() == null ? "" : product.getItemName().toLowerCase(Locale.ROOT);
        String itemId = product.getItemId() == null ? "" : product.getItemId().toString();
        String listingId = product.getListingId() == null ? "" : product.getListingId().toString();

        return itemName.contains(normalizedKeyword)
                || itemId.contains(normalizedKeyword)
                || listingId.contains(normalizedKeyword);
    }

    private static boolean sameProduct(SellerProductDTO first, SellerProductDTO second) {
        if (first == null || second == null) {
            return false;
        }
        if (first.getListingId() != null && second.getListingId() != null) {
            return first.getListingId().equals(second.getListingId());
        }
        return first.getItemId() != null && first.getItemId().equals(second.getItemId());
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
