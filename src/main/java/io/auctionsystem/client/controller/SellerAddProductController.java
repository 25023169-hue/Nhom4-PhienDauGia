package io.auctionsystem.client.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.auctionsystem.client.pattern.AuctionManager;
import io.auctionsystem.client.pattern.SceneManager;
import io.auctionsystem.common.Constants;
import io.auctionsystem.common.enums.ItemType;
import io.auctionsystem.common.request.SellerProductRequest;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

public class SellerAddProductController {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm");

    @FXML private TextField txtName;
    @FXML private TextArea txtDescription;
    @FXML private TextField txtStartingPrice;
    @FXML private TextField txtBidIncrement;
    @FXML private TextField txtBuyNowPrice;
    @FXML private TextField txtImageUrl;
    @FXML private ComboBox<ItemType> cbItemType;
    @FXML private DatePicker dpStartDate;
    @FXML private TextField txtStartTime;
    @FXML private DatePicker dpEndDate;
    @FXML private TextField txtEndTime;

    @FXML private VBox artFields;
    @FXML private TextField txtArtistName;
    @FXML private TextField txtMedium;
    @FXML private TextField txtDimensions;
    @FXML private TextField txtCreationYear;

    @FXML private VBox electronicsFields;
    @FXML private TextField txtElectronicsBrand;
    @FXML private TextField txtElectronicsModel;
    @FXML private TextField txtWarrantyMonths;

    @FXML private VBox vehicleFields;
    @FXML private TextField txtVinCode;
    @FXML private TextField txtManufactureYear;
    @FXML private TextField txtFuelType;

    @FXML private VBox fashionFields;
    @FXML private TextField txtFashionBrand;
    @FXML private TextField txtFashionSize;
    @FXML private TextField txtFashionMaterial;
    @FXML private TextField txtFashionGender;

    @FXML private VBox jewelryFields;
    @FXML private TextField txtJewelryMaterial;
    @FXML private TextField txtWeight;
    @FXML private TextField txtGemstone;

    @FXML private Button btnStartAuction;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @FXML
    public void initialize() {
        cbItemType.setItems(FXCollections.observableArrayList(ItemType.values()));
        cbItemType.getSelectionModel().select(ItemType.ART);

        LocalDateTime defaultStart = LocalDateTime.now().plusMinutes(5).truncatedTo(ChronoUnit.MINUTES);
        dpStartDate.setValue(defaultStart.toLocalDate());
        txtStartTime.setText(defaultStart.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        dpEndDate.setValue(defaultStart.plusDays(1).toLocalDate());
        txtEndTime.setText(defaultStart.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));

        cbItemType.valueProperty().addListener((observable, oldValue, newValue) -> showFieldsForType(newValue));
        showFieldsForType(cbItemType.getValue());
    }

    @FXML
    public void onBackClicked() {
        SellerDashboardController controller = SellerDashboardController.getInstance();
        if (controller != null) {
            controller.onManageAuctionsClicked();
        } else {
            SceneManager.getInstance().switchScene("/client/fxml/seller_dashboard.fxml");
        }
    }

    @FXML
    public void onStartAuctionClicked() {
        try {
            SellerProductRequest request = buildRequest();
            sendCreateRequest(request);
        } catch (IllegalArgumentException e) {
            showAlert(Alert.AlertType.ERROR, e.getMessage());
        }
    }

    private SellerProductRequest buildRequest() {
        SellerProductRequest request = new SellerProductRequest();
        request.setSellerId(AuctionManager.getInstance().getId());
        request.setName(requiredText(txtName, "Tên sản phẩm"));
        request.setDescription(optionalText(txtDescription));
        request.setStartingPrice(parseRequiredPositiveDouble(txtStartingPrice, "Giá khởi điểm"));
        request.setBidIncrement(parseRequiredPositiveDouble(txtBidIncrement, "Bước giá"));
        request.setBuyNowPrice(parseOptionalPositiveDouble(txtBuyNowPrice, "Giá mua đứt"));
        request.setImageUrl(optionalText(txtImageUrl));
        request.setItemType(cbItemType.getValue());
        request.setStartTime(parseDateTime(dpStartDate, txtStartTime, "Thời gian bắt đầu"));
        request.setEndTime(parseDateTime(dpEndDate, txtEndTime, "Thời gian kết thúc"));

        if (request.getSellerId() == null) {
            throw new IllegalArgumentException("Seller hiện tại không tồn tại. Vui lòng đăng nhập lại.");
        }
        if (request.getItemType() == null) {
            throw new IllegalArgumentException("Vui lòng chọn loại sản phẩm.");
        }
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new IllegalArgumentException("Thời gian kết thúc phải sau thời gian bắt đầu.");
        }
        if (request.getBuyNowPrice() != null && request.getBuyNowPrice() <= request.getStartingPrice()) {
            throw new IllegalArgumentException("Giá mua đứt phải lớn hơn giá khởi điểm.");
        }

        fillTypeSpecificFields(request);
        return request;
    }

    private void fillTypeSpecificFields(SellerProductRequest request) {
        switch (request.getItemType()) {
            case ART -> {
                request.setArtistName(optionalText(txtArtistName));
                request.setMedium(optionalText(txtMedium));
                request.setDimensions(optionalText(txtDimensions));
                request.setCreationYear(parseOptionalInteger(txtCreationYear, "Năm sáng tác"));
            }
            case ELECTRONICS -> {
                request.setBrand(optionalText(txtElectronicsBrand));
                request.setModel(optionalText(txtElectronicsModel));
                request.setWarrantyMonths(parseOptionalInteger(txtWarrantyMonths, "Tháng bảo hành"));
            }
            case VEHICLE -> {
                request.setVinCode(optionalText(txtVinCode));
                request.setManufactureYear(parseOptionalInteger(txtManufactureYear, "Năm sản xuất"));
                request.setFuelType(optionalText(txtFuelType));
            }
            case FASHION -> {
                request.setBrand(optionalText(txtFashionBrand));
                request.setSize(optionalText(txtFashionSize));
                request.setMaterial(optionalText(txtFashionMaterial));
                request.setGender(optionalText(txtFashionGender));
            }
            case JEWELRY -> {
                request.setMaterial(optionalText(txtJewelryMaterial));
                request.setWeight(parseOptionalPositiveDouble(txtWeight, "Trọng lượng"));
                request.setGemstone(optionalText(txtGemstone));
            }
        }
    }

    private void sendCreateRequest(SellerProductRequest requestDto) {
        btnStartAuction.setDisable(true);

        new Thread(() -> {
            try {
                String jsonBody = objectMapper.writeValueAsString(requestDto);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(Constants.BASE_URL + "/seller/products"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                Platform.runLater(() -> handleCreateResponse(response));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    btnStartAuction.setDisable(false);
                    showAlert(Alert.AlertType.ERROR, "Không thể kết nối đến server khi lưu sản phẩm.");
                });
            }
        }).start();
    }

    private void handleCreateResponse(HttpResponse<String> response) {
        btnStartAuction.setDisable(false);
        String message = extractMessage(response.body());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            showAlert(Alert.AlertType.INFORMATION, message);
            onBackClicked();
            return;
        }

        showAlert(Alert.AlertType.ERROR, message);
    }

    private void showFieldsForType(ItemType type) {
        setVisible(artFields, type == ItemType.ART);
        setVisible(electronicsFields, type == ItemType.ELECTRONICS);
        setVisible(vehicleFields, type == ItemType.VEHICLE);
        setVisible(fashionFields, type == ItemType.FASHION);
        setVisible(jewelryFields, type == ItemType.JEWELRY);
    }

    private void setVisible(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private LocalDateTime parseDateTime(DatePicker datePicker, TextField timeField, String fieldName) {
        LocalDate date = datePicker.getValue();
        if (date == null) {
            throw new IllegalArgumentException(fieldName + " phải có ngày.");
        }

        try {
            LocalTime time = LocalTime.parse(requiredText(timeField, fieldName), TIME_FORMATTER);
            return LocalDateTime.of(date, time);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(fieldName + " phải có giờ đúng định dạng HH:mm.");
        }
    }

    private String requiredText(TextField field, String fieldName) {
        String value = field.getText();
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " không được để trống.");
        }
        return value.trim();
    }

    private String optionalText(TextField field) {
        if (field == null || field.getText() == null || field.getText().trim().isEmpty()) {
            return null;
        }
        return field.getText().trim();
    }

    private String optionalText(TextArea area) {
        if (area == null || area.getText() == null || area.getText().trim().isEmpty()) {
            return null;
        }
        return area.getText().trim();
    }

    private double parseRequiredPositiveDouble(TextField field, String fieldName) {
        String value = requiredText(field, fieldName);
        double number = parseDouble(value, fieldName);
        if (number <= 0) {
            throw new IllegalArgumentException(fieldName + " phải lớn hơn 0.");
        }
        return number;
    }

    private Double parseOptionalPositiveDouble(TextField field, String fieldName) {
        String value = field.getText();
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        double number = parseDouble(value, fieldName);
        if (number <= 0) {
            throw new IllegalArgumentException(fieldName + " phải lớn hơn 0.");
        }
        return number;
    }

    private Integer parseOptionalInteger(TextField field, String fieldName) {
        String value = field.getText();
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " phải là số nguyên.");
        }
    }

    private double parseDouble(String value, String fieldName) {
        try {
            return Double.parseDouble(normalizeNumber(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " phải là số hợp lệ.");
        }
    }

    private String normalizeNumber(String value) {
        String normalized = value.trim().replace(" ", "");
        if (normalized.contains(",") && normalized.contains(".")) {
            return normalized.replace(".", "").replace(",", ".");
        }
        if (normalized.contains(",")) {
            int digitsAfterComma = normalized.length() - normalized.lastIndexOf(',') - 1;
            return digitsAfterComma == 3 ? normalized.replace(",", "") : normalized.replace(",", ".");
        }
        if (normalized.contains(".")) {
            int digitsAfterDot = normalized.length() - normalized.lastIndexOf('.') - 1;
            return digitsAfterDot == 3 ? normalized.replace(".", "") : normalized;
        }
        return normalized;
    }

    private String extractMessage(String responseBody) {
        try {
            JsonNode node = objectMapper.readTree(responseBody);
            if (node.hasNonNull("message")) {
                return node.get("message").asText();
            }
        } catch (Exception ignored) {
        }
        return responseBody == null || responseBody.isBlank() ? "Server trả về kết quả không xác định." : responseBody;
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
