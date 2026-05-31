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

    @FXML private TextField txtSearch;
    @FXML private Label lblCount;
    @FXML private Button btnOpenStatus;
    @FXML private Button btnRunningStatus;
    @FXML private Button btnFinishedStatus;
    @FXML private Button btnCancelledStatus;
    @FXML private TableView<SellerProductDTO> tableProducts;
    @FXML private TableColumn<SellerProductDTO, String> colItemName;
    @FXML private TableColumn<SellerProductDTO, Double> colStartingPrice;
    @FXML private TableColumn<SellerProductDTO, String> colStatus;

    private final ObservableList<SellerProductDTO> products = FXCollections.observableArrayList();
    private final List<SellerProductDTO> currentProducts = new ArrayList<>();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));
    private final PauseTransition searchDelay = new PauseTransition(Duration.millis(350));
    private AuctionState selectedStatus;

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

        configureStatusButtons();
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

    private void configureStatusButtons() {
        btnOpenStatus.setOnAction(event -> toggleStatusFilter(AuctionState.OPEN));
        btnRunningStatus.setOnAction(event -> toggleStatusFilter(AuctionState.RUNNING));
        btnFinishedStatus.setOnAction(event -> toggleStatusFilter(AuctionState.FINISHED));
        btnCancelledStatus.setOnAction(event -> toggleStatusFilter(AuctionState.CANCELLED));
        updateStatusButtonStyles();
    }

    private void toggleStatusFilter(AuctionState status) {
        selectedStatus = status == selectedStatus ? null : status;
        applyVisibleProducts();
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
        long cancelledCount = loadedProducts.stream()
                .filter(product -> product.getStatus() == AuctionState.CANCELLED)
                .count();

        lblCount.setText("(" + products.size() + " items)");
        btnOpenStatus.setText("OPEN: " + openCount);
        btnRunningStatus.setText("RUNNING: " + runningCount);
        btnFinishedStatus.setText("FINISHED: " + finishedCount);
        btnCancelledStatus.setText("CANCELLED: " + cancelledCount);
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
        pendingProducts.removeIf(product -> !matchesSearch(product));
        if (!pendingProducts.isEmpty()) {
            currentProducts.clear();
            currentProducts.addAll(pendingProducts);
            applyVisibleProducts();
        }
    }

    private void applyLoadedProducts(List<SellerProductDTO> loadedProducts) {
        List<SellerProductDTO> mergedProducts = new ArrayList<>(loadedProducts);

        synchronized (pendingCreatedProducts) {
            pendingCreatedProducts.removeIf(pending ->
                    loadedProducts.stream().anyMatch(loaded -> sameProduct(loaded, pending)));
            for (int i = pendingCreatedProducts.size() - 1; i >= 0; i--) {
                SellerProductDTO pending = pendingCreatedProducts.get(i);
                if (matchesSearch(pending)
                        && mergedProducts.stream().noneMatch(loaded -> sameProduct(loaded, pending))) {
                    mergedProducts.add(0, pending);
                }
            }
        }

        currentProducts.clear();
        currentProducts.addAll(mergedProducts);
        applyVisibleProducts();
    }

    private List<SellerProductDTO> pendingSnapshot() {
        synchronized (pendingCreatedProducts) {
            return new ArrayList<>(pendingCreatedProducts);
        }
    }

    private boolean matchesSearch(SellerProductDTO product) {
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

    private void applyVisibleProducts() {
        products.setAll(currentProducts.stream()
                .filter(product -> selectedStatus == null || product.getStatus() == selectedStatus)
                .toList());
        updateSummary(currentProducts);
        updateStatusButtonStyles();
    }

    private void updateStatusButtonStyles() {
        styleStatusButton(btnOpenStatus, AuctionState.OPEN, "#2563eb");
        styleStatusButton(btnRunningStatus, AuctionState.RUNNING, "#f59e0b");
        styleStatusButton(btnFinishedStatus, AuctionState.FINISHED, "#10b981");
        styleStatusButton(btnCancelledStatus, AuctionState.CANCELLED, "#ef4444");
    }

    private void styleStatusButton(Button button, AuctionState status, String color) {
        boolean selected = selectedStatus == status;
        button.setStyle("-fx-background-color: " + (selected ? color : "white") + ";"
                + "-fx-text-fill: " + (selected ? "white" : color) + ";"
                + "-fx-font-weight: bold;"
                + "-fx-cursor: hand;"
                + "-fx-background-radius: 8;"
                + "-fx-border-color: " + color + ";"
                + "-fx-border-width: " + (selected ? "2" : "1") + ";"
                + "-fx-border-radius: 8;"
                + "-fx-padding: 10 14;");
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
