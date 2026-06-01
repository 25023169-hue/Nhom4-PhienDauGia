package io.auctionsystem.client.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.auctionsystem.client.pattern.AuctionManager;
import io.auctionsystem.client.pattern.ClientHttp;
import io.auctionsystem.client.pattern.SceneManager;
import io.auctionsystem.common.Constants;
import io.auctionsystem.common.dto.SellerProductDTO;
import io.auctionsystem.common.enums.ItemType;
import io.auctionsystem.common.request.SellerProductRequest;
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
import java.util.Objects;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class SellerAddProductController {

  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm");
  private static final DateTimeFormatter DISPLAY_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("HH:mm");
  private static final long SUGGESTED_AUCTION_DURATION_MINUTES = 5;
  private static ProductDraft savedDraft;
  private static SellerProductDTO editingProduct;

  @FXML private Label lblFormTitle;
  @FXML private TextField txtName;
  @FXML private TextArea txtDescription;
  @FXML private TextField txtStartingPrice;
  @FXML private TextField txtBuyNowPrice;
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
  @FXML private TextField txtCondition;

  @FXML private VBox vehicleFields;
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

  private final HttpClient httpClient = ClientHttp.client();
  private final ObjectMapper objectMapper = ClientHttp.mapper();

  public static void startCreating() {
    editingProduct = null;
  }

  public static void startEditing(SellerProductDTO product) {
    editingProduct = product;
  }

  @FXML
  public void initialize() {
    cbItemType.setItems(FXCollections.observableArrayList(ItemType.values()));
    cbItemType.getSelectionModel().select(ItemType.ART);

    LocalDateTime defaultStart = LocalDateTime.now().plusMinutes(5).truncatedTo(ChronoUnit.MINUTES);
    dpStartDate.setValue(defaultStart.toLocalDate());
    txtStartTime.setText(defaultStart.toLocalTime().format(DISPLAY_TIME_FORMATTER));
    updateSuggestedEndTime();

    cbItemType
        .valueProperty()
        .addListener((observable, oldValue, newValue) -> showFieldsForType(newValue));
    dpStartDate
        .valueProperty()
        .addListener((observable, oldValue, newValue) -> updateSuggestedEndTime());
    txtStartTime
        .textProperty()
        .addListener((observable, oldValue, newValue) -> updateSuggestedEndTime());
    if (isEditing()) {
      restoreEditingProduct();
      lblFormTitle.setText("Chỉnh sửa sản phẩm");
      btnStartAuction.setText("Lưu thay đổi");
    } else {
      restoreDraft();
    }
    showFieldsForType(cbItemType.getValue());
  }

  @FXML
  public void onBackClicked() {
    if (!isEditing()) {
      saveDraft();
    }
    editingProduct = null;
    navigateBack();
  }

  private void navigateBack() {
    SellerDashboardController controller = SellerDashboardController.getInstance();
    if (controller != null) {
      controller.onManageAuctionsClicked();
    } else {
      SceneManager.getInstance().switchScene("/client/fxml/user/seller/seller_dashboard.fxml");
    }
  }

  @FXML
  public void onStartAuctionClicked() {
    try {
      SellerProductRequest request = buildRequest();
      sendSaveRequest(request);
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
    request.setBuyNowPrice(parseOptionalPositiveDouble(txtBuyNowPrice, "Giá mua đứt"));
    request.setItemType(cbItemType.getValue());
    request.setStartTime(parseDateTime(dpStartDate, txtStartTime, "Thời gian bắt đầu"));
    request.setEndTime(parseDateTime(dpEndDate, txtEndTime, "Thời gian kết thúc"));

    if (request.getSellerId() == null) {
      throw new IllegalArgumentException("Seller hiện tại không tồn tại. Vui lòng đăng nhập lại.");
    }
    if (request.getItemType() == null) {
      throw new IllegalArgumentException("Vui lòng chọn loại sản phẩm.");
    }
    if (request.getStartTime().isBefore(LocalDateTime.now())) {
      throw new IllegalArgumentException("Thời gian bắt đầu không được ở quá khứ.");
    }
    if (!request.getEndTime().isAfter(request.getStartTime())) {
      throw new IllegalArgumentException("Thời gian kết thúc phải sau thời gian bắt đầu.");
    }
    if (request.getBuyNowPrice() != null
        && request.getBuyNowPrice() <= request.getStartingPrice()) {
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
        request.setCondition(optionalText(txtCondition));
      }
      case VEHICLE -> {
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

  private void sendSaveRequest(SellerProductRequest requestDto) {
    btnStartAuction.setDisable(true);

    new Thread(
            () -> {
              try {
                String jsonBody = objectMapper.writeValueAsString(requestDto);
                HttpRequest.Builder requestBuilder =
                    HttpRequest.newBuilder().header("Content-Type", "application/json");
                HttpRequest request;
                if (isEditing()) {
                  request =
                      requestBuilder
                          .uri(
                              URI.create(
                                  Constants.BASE_URL
                                      + "/seller/products/"
                                      + editingProduct.getItemId()))
                          .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                          .build();
                } else {
                  request =
                      requestBuilder
                          .uri(URI.create(Constants.BASE_URL + "/seller/products"))
                          .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                          .build();
                }

                HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                Platform.runLater(() -> handleSaveResponse(response));
              } catch (Exception e) {
                Platform.runLater(
                    () -> {
                      btnStartAuction.setDisable(false);
                      showAlert(
                          Alert.AlertType.ERROR, "Không thể kết nối đến server khi lưu sản phẩm.");
                    });
              }
            })
        .start();
  }

  private void handleSaveResponse(HttpResponse<String> response) {
    btnStartAuction.setDisable(false);
    String message = extractMessage(response.body());
    if (response.statusCode() >= 200 && response.statusCode() < 300) {
      savedDraft = null;
      SellerProductListController.rememberCreatedProduct(extractCreatedProduct(response.body()));
      editingProduct = null;
      showAlert(Alert.AlertType.INFORMATION, message);
      navigateBack();
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

  private void saveDraft() {
    ProductDraft draft = new ProductDraft();
    draft.sellerId = AuctionManager.getInstance().getId();
    draft.name = txtName.getText();
    draft.description = txtDescription.getText();
    draft.startingPrice = txtStartingPrice.getText();
    draft.buyNowPrice = txtBuyNowPrice.getText();
    draft.itemType = cbItemType.getValue();
    draft.startDate = dpStartDate.getValue();
    draft.startTime = txtStartTime.getText();
    draft.endDate = dpEndDate.getValue();
    draft.endTime = txtEndTime.getText();
    draft.artistName = txtArtistName.getText();
    draft.medium = txtMedium.getText();
    draft.dimensions = txtDimensions.getText();
    draft.creationYear = txtCreationYear.getText();
    draft.electronicsBrand = txtElectronicsBrand.getText();
    draft.electronicsModel = txtElectronicsModel.getText();
    draft.condition = txtCondition.getText();
    draft.manufactureYear = txtManufactureYear.getText();
    draft.fuelType = txtFuelType.getText();
    draft.fashionBrand = txtFashionBrand.getText();
    draft.fashionSize = txtFashionSize.getText();
    draft.fashionMaterial = txtFashionMaterial.getText();
    draft.fashionGender = txtFashionGender.getText();
    draft.jewelryMaterial = txtJewelryMaterial.getText();
    draft.weight = txtWeight.getText();
    draft.gemstone = txtGemstone.getText();
    savedDraft = draft;
  }

  private void restoreDraft() {
    ProductDraft draft = savedDraft;
    if (draft == null || !Objects.equals(draft.sellerId, AuctionManager.getInstance().getId())) {
      return;
    }

    txtName.setText(draft.name);
    txtDescription.setText(draft.description);
    txtStartingPrice.setText(draft.startingPrice);
    txtBuyNowPrice.setText(draft.buyNowPrice);
    cbItemType.setValue(draft.itemType);
    dpStartDate.setValue(draft.startDate);
    txtStartTime.setText(draft.startTime);
    dpEndDate.setValue(draft.endDate);
    txtEndTime.setText(draft.endTime);
    txtArtistName.setText(draft.artistName);
    txtMedium.setText(draft.medium);
    txtDimensions.setText(draft.dimensions);
    txtCreationYear.setText(draft.creationYear);
    txtElectronicsBrand.setText(draft.electronicsBrand);
    txtElectronicsModel.setText(draft.electronicsModel);
    txtCondition.setText(draft.condition);
    txtManufactureYear.setText(draft.manufactureYear);
    txtFuelType.setText(draft.fuelType);
    txtFashionBrand.setText(draft.fashionBrand);
    txtFashionSize.setText(draft.fashionSize);
    txtFashionMaterial.setText(draft.fashionMaterial);
    txtFashionGender.setText(draft.fashionGender);
    txtJewelryMaterial.setText(draft.jewelryMaterial);
    txtWeight.setText(draft.weight);
    txtGemstone.setText(draft.gemstone);
  }

  private void restoreEditingProduct() {
    SellerProductDTO product = editingProduct;
    if (product == null) {
      return;
    }

    txtName.setText(product.getItemName());
    txtDescription.setText(product.getDescription());
    txtStartingPrice.setText(formatNumber(product.getStartingPrice()));
    txtBuyNowPrice.setText(formatNumber(product.getBuyNowPrice()));
    cbItemType.setValue(product.getItemType());
    setDateTime(dpStartDate, txtStartTime, product.getStartTime());
    setDateTime(dpEndDate, txtEndTime, product.getEndTime());
    txtArtistName.setText(product.getArtistName());
    txtMedium.setText(product.getMedium());
    txtDimensions.setText(product.getDimensions());
    txtCreationYear.setText(formatNumber(product.getCreationYear()));
    txtElectronicsBrand.setText(product.getBrand());
    txtElectronicsModel.setText(product.getModel());
    txtCondition.setText(product.getCondition());
    txtManufactureYear.setText(formatNumber(product.getManufactureYear()));
    txtFuelType.setText(product.getFuelType());
    txtFashionBrand.setText(product.getBrand());
    txtFashionSize.setText(product.getSize());
    txtFashionMaterial.setText(product.getMaterial());
    txtFashionGender.setText(product.getGender());
    txtJewelryMaterial.setText(product.getMaterial());
    txtWeight.setText(formatNumber(product.getWeight()));
    txtGemstone.setText(product.getGemstone());
  }

  private void setDateTime(DatePicker datePicker, TextField timeField, String value) {
    if (value == null || value.isBlank()) {
      return;
    }
    LocalDateTime dateTime =
        LocalDateTime.parse(value, DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    datePicker.setValue(dateTime.toLocalDate());
    timeField.setText(dateTime.toLocalTime().format(DISPLAY_TIME_FORMATTER));
  }

  private String formatNumber(Number value) {
    return value == null ? "" : value.toString();
  }

  private boolean isEditing() {
    return editingProduct != null;
  }

  private void updateSuggestedEndTime() {
    LocalDate startDate = dpStartDate.getValue();
    String startTimeText = txtStartTime.getText();
    if (startDate == null || startTimeText == null || startTimeText.trim().isEmpty()) {
      return;
    }

    try {
      LocalTime startTime = LocalTime.parse(startTimeText.trim(), TIME_FORMATTER);
      LocalDateTime suggestedEnd =
          LocalDateTime.of(startDate, startTime).plusMinutes(SUGGESTED_AUCTION_DURATION_MINUTES);
      dpEndDate.setValue(suggestedEnd.toLocalDate());
      txtEndTime.setText(suggestedEnd.toLocalTime().format(DISPLAY_TIME_FORMATTER));
    } catch (DateTimeParseException ignored) {
      // Keep the current end time while the user is still typing.
    }
  }

  private LocalDateTime parseDateTime(
      DatePicker datePicker, TextField timeField, String fieldName) {
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
    return responseBody == null || responseBody.isBlank()
        ? "Server trả về kết quả không xác định."
        : responseBody;
  }

  private SellerProductDTO extractCreatedProduct(String responseBody) {
    try {
      JsonNode node = objectMapper.readTree(responseBody);
      JsonNode dataNode = node.get("data");
      if (dataNode != null && dataNode.isObject()) {
        return objectMapper.treeToValue(dataNode, SellerProductDTO.class);
      }
    } catch (Exception ignored) {
    }
    return null;
  }

  private void showAlert(Alert.AlertType type, String message) {
    Alert alert = new Alert(type);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }

  private static class ProductDraft {
    private Long sellerId;
    private String name;
    private String description;
    private String startingPrice;
    private String buyNowPrice;
    private ItemType itemType;
    private LocalDate startDate;
    private String startTime;
    private LocalDate endDate;
    private String endTime;
    private String artistName;
    private String medium;
    private String dimensions;
    private String creationYear;
    private String electronicsBrand;
    private String electronicsModel;
    private String condition;
    private String manufactureYear;
    private String fuelType;
    private String fashionBrand;
    private String fashionSize;
    private String fashionMaterial;
    private String fashionGender;
    private String jewelryMaterial;
    private String weight;
    private String gemstone;
  }
}
