package client.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import client.TransactionViewModel;
import client.pattern.AuctionManager;
import client.pattern.WebSocketClientManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import common.dto.AuctionItemDTO;
import common.dto.AuctionPriceUpdateDTO;
import common.dto.BidDTO;
import common.dto.NotificationDTO;
import common.dto.SellerProductDTO;
import common.enums.AuctionState;
import common.enums.ItemType;
import common.enums.NotificationType;
import common.enums.TransactionType;
import common.request.SellerProductRequest;
import common.response.AuthResponse;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompSession;

class ClientControllerLocalLogicTest {

  @BeforeAll
  static void startJavaFxToolkit() throws InterruptedException {
    CountDownLatch started = new CountDownLatch(1);
    try {
      Platform.startup(started::countDown);
    } catch (IllegalStateException alreadyStarted) {
      started.countDown();
    }
    assertTrue(started.await(5, TimeUnit.SECONDS));
  }

  @AfterEach
  void tearDown() {
    AuctionManager.getInstance().isLoggedOut();
    SellerAddProductController.startCreating();
    SellerProductDetailController.setProduct(null);
    field(WebSocketClientManager.getInstance(), "stompSession", null);
  }

  @Test
  void sellerAddProduct_BuildsRequestsForEveryItemType() {
    SellerAddProductController controller = sellerAddController();
    login(1L);

    for (ItemType type : ItemType.values()) {
      ComboBox<ItemType> comboBox = get(controller, "cbItemType");
      when(comboBox.getValue()).thenReturn(type);

      SellerProductRequest request = invoke(controller, "buildRequest");

      assertEquals(1L, request.getSellerId());
      assertEquals(type, request.getItemType());
      assertEquals("Product", request.getName());
      assertEquals(1000.0, request.getStartingPrice());
      assertEquals(2000.0, request.getBuyNowPrice());
    }
  }

  @Test
  void sellerAddProduct_ValidatesAndNormalizesValues() {
    SellerAddProductController controller = sellerAddController();
    TextField field = mock(TextField.class);

    assertEquals("1234.5", invoke(controller, "normalizeNumber", types(String.class), "1.234,5"));
    assertEquals("1234", invoke(controller, "normalizeNumber", types(String.class), "1,234"));
    assertEquals("12.5", invoke(controller, "normalizeNumber", types(String.class), "12,5"));
    assertEquals("1234", invoke(controller, "normalizeNumber", types(String.class), "1.234"));
    assertEquals("12.5", invoke(controller, "normalizeNumber", types(String.class), "12.5"));
    assertEquals("message", invoke(controller, "extractMessage", types(String.class), "{\"message\":\"message\"}"));
    assertEquals("fallback", invoke(controller, "extractMessage", types(String.class), "fallback"));
    assertEquals(
        "Server trả về kết quả không xác định.",
        invoke(controller, "extractMessage", types(String.class), ""));

    when(field.getText()).thenReturn(" ");
    assertThrows(IllegalArgumentException.class, () -> invoke(controller, "requiredText", types(TextField.class, String.class), field, "Field"));
    assertNull(invoke(controller, "optionalText", types(TextField.class), field));
    assertNull(invoke(controller, "parseOptionalPositiveDouble", types(TextField.class, String.class), field, "Field"));
    assertNull(invoke(controller, "parseOptionalInteger", types(TextField.class, String.class), field, "Field"));

    when(field.getText()).thenReturn("-1");
    assertThrows(IllegalArgumentException.class, () -> invoke(controller, "parseRequiredPositiveDouble", types(TextField.class, String.class), field, "Field"));
    assertThrows(IllegalArgumentException.class, () -> invoke(controller, "parseOptionalPositiveDouble", types(TextField.class, String.class), field, "Field"));

    when(field.getText()).thenReturn("invalid");
    assertThrows(IllegalArgumentException.class, () -> invoke(controller, "parseOptionalInteger", types(TextField.class, String.class), field, "Field"));
    assertThrows(IllegalArgumentException.class, () -> invoke(controller, "parseDouble", types(String.class, String.class), "invalid", "Field"));
  }

  @Test
  void sellerAddProduct_SavesRestoresDraftAndEditingProduct() {
    SellerAddProductController controller = sellerAddController();
    login(1L);

    invoke(controller, "saveDraft");
    invoke(controller, "restoreDraft");

    SellerProductDTO product = product(11L, 21L, "Edited", AuctionState.OPEN);
    product.setDescription("description");
    product.setItemType(ItemType.FASHION);
    product.setStartingPrice(100.0);
    product.setBuyNowPrice(200.0);
    product.setStartTime("02/06/2026 10:00");
    product.setEndTime("02/06/2026 11:00");
    product.setBrand("brand");
    product.setSize("M");
    product.setMaterial("material");
    product.setGender("gender");
    product.setWeight(2.0);
    SellerAddProductController.startEditing(product);

    invoke(controller, "restoreEditingProduct");

    TextField name = get(controller, "txtName");
    verify(name).setText("Edited");
    assertTrue((boolean) invoke(controller, "isEditing"));
    SellerAddProductController.startCreating();
    assertFalse((boolean) invoke(controller, "isEditing"));
  }

  @Test
  void sellerAddProduct_UpdatesSuggestedEndTimeAndVisibility() {
    SellerAddProductController controller = sellerAddController();
    DatePicker startDate = get(controller, "dpStartDate");
    TextField startTime = get(controller, "txtStartTime");
    DatePicker endDate = get(controller, "dpEndDate");
    TextField endTime = get(controller, "txtEndTime");
    when(startDate.getValue()).thenReturn(LocalDate.of(2026, 6, 2));
    when(startTime.getText()).thenReturn("10:15");

    invoke(controller, "updateSuggestedEndTime");
    verify(endDate).setValue(LocalDate.of(2026, 6, 2));
    verify(endTime).setText("10:20");

    invoke(controller, "showFieldsForType", types(ItemType.class), ItemType.JEWELRY);
    VBox jewelry = get(controller, "jewelryFields");
    verify(jewelry).setVisible(true);
    verify(jewelry).setManaged(true);
  }

  @Test
  void sellerProductList_FiltersMergesAndSummarizesProducts() {
    SellerProductListController controller = new SellerProductListController();
    TextField search = set(controller, "txtSearch", mock(TextField.class));
    Label count = set(controller, "lblCount", mock(Label.class));
    Button open = set(controller, "btnOpenStatus", mock(Button.class));
    Button running = set(controller, "btnRunningStatus", mock(Button.class));
    Button finished = set(controller, "btnFinishedStatus", mock(Button.class));
    Button cancelled = set(controller, "btnCancelledStatus", mock(Button.class));
    when(search.getText()).thenReturn("");

    SellerProductDTO first = product(1L, 10L, "Laptop", AuctionState.OPEN);
    SellerProductDTO second = product(2L, 20L, "Phone", AuctionState.RUNNING);
    SellerProductDTO third = product(3L, 30L, "Watch", AuctionState.FINISHED);
    SellerProductDTO fourth = product(4L, 40L, "Bag", AuctionState.CANCELLED);

    invoke(controller, "applyLoadedProducts", types(List.class), List.of(first, second, third, fourth));
    verify(count).setText("(4 items)");
    verify(open).setText("OPEN: 1");
    verify(running).setText("RUNNING: 1");
    verify(finished).setText("FINISHED: 1");
    verify(cancelled).setText("CANCELLED: 1");

    invoke(controller, "toggleStatusFilter", types(AuctionState.class), AuctionState.OPEN);
    ObservableList<SellerProductDTO> products = get(controller, "products");
    assertEquals(List.of(first), products);
    invoke(controller, "toggleStatusFilter", types(AuctionState.class), AuctionState.OPEN);
    assertEquals(4, products.size());

    when(search.getText()).thenReturn("lap");
    assertTrue((boolean) invoke(controller, "matchesSearch", types(SellerProductDTO.class), first));
    assertFalse((boolean) invoke(controller, "matchesSearch", types(SellerProductDTO.class), second));
    when(search.getText()).thenReturn("20");
    assertTrue((boolean) invoke(controller, "matchesSearch", types(SellerProductDTO.class), second));
    assertEquals("OPEN", invoke(controller, "formatStatus", types(AuctionState.class), AuctionState.OPEN));
    assertEquals("UNKNOWN", invoke(controller, "formatStatus", types(AuctionState.class), new Object[] {null}));
    assertEquals("message", invoke(controller, "extractMessage", types(String.class), "{\"message\":\"message\"}"));
  }

  @Test
  void sellerProductList_TracksPendingProductsAndBuildsEncodedUrl() {
    SellerProductListController controller = new SellerProductListController();
    TextField search = set(controller, "txtSearch", mock(TextField.class));
    set(controller, "lblCount", mock(Label.class));
    set(controller, "btnOpenStatus", mock(Button.class));
    set(controller, "btnRunningStatus", mock(Button.class));
    set(controller, "btnFinishedStatus", mock(Button.class));
    set(controller, "btnCancelledStatus", mock(Button.class));
    SellerProductDTO product = product(1L, 10L, "Laptop", AuctionState.OPEN);
    when(search.getText()).thenReturn(" máy tính ");

    SellerProductListController.rememberCreatedProduct(product);
    SellerProductListController.rememberCreatedProduct(product);
    assertTrue(((String) invoke(controller, "buildListUrl", types(Long.class), 1L)).contains("keyword=m%C3%A1y+t%C3%ADnh"));
    SellerProductListController.forgetProduct(product);
  }

  @Test
  void wallet_FiltersFormatsAndRendersErrors() {
    WalletController controller = new WalletController();
    Label balance = set(controller, "lblBalance", mock(Label.class));
    Label total = set(controller, "lblTotalBalance", mock(Label.class));
    Label held = set(controller, "lblHeldBalance", mock(Label.class));
    Label error = set(controller, "lblTransactionError", mock(Label.class));
    HBox errorBox = set(controller, "bankErrorBox", mock(HBox.class));
    Button setup = set(controller, "btnSetupBank", mock(Button.class));
    TextField amount = set(controller, "txtAmount", mock(TextField.class));
    TextField search = set(controller, "txtSearch", mock(TextField.class));
    ComboBox<String> timeFilter = set(controller, "cbTimeFilter", mock(ComboBox.class));
    Button filterIn = set(controller, "btnFilterIn", mock(Button.class));
    Button filterOut = set(controller, "btnFilterOut", mock(Button.class));
    when(search.getText()).thenReturn("");
    when(timeFilter.getValue()).thenReturn("Tất cả thời gian");

    ObservableList<TransactionViewModel> list =
        FXCollections.observableArrayList(
            new TransactionViewModel(1L, LocalDateTime.now(), "now", "+ 100", "", "100", "Nạp", "salary"),
            new TransactionViewModel(2L, LocalDateTime.now().minusMonths(2), "old", "", "- 50", "50", "Rút", null));
    FilteredList<TransactionViewModel> filtered = new FilteredList<>(list, item -> true);
    field(controller, "filteredTransactions", filtered);

    invoke(controller, "applyFilters");
    assertEquals(2, filtered.size());
    when(search.getText()).thenReturn("salary");
    invoke(controller, "applyFilters");
    assertEquals(1, filtered.size());
    when(search.getText()).thenReturn("100");
    invoke(controller, "applyFilters");
    assertEquals(1, filtered.size());

    controller.onFilterInClicked();
    controller.onFilterInClicked();
    controller.onFilterOutClicked();
    controller.onFilterOutClicked();
    controller.onClearFilterClicked();

    when(amount.getText()).thenReturn("1.234,00");
    assertEquals(123400.0, (double) invoke(controller, "parseWalletAmount"));
    when(amount.getText()).thenReturn("invalid");
    assertEquals(-1.0, (double) invoke(controller, "parseWalletAmount"));
    invoke(controller, "updateDisplayedBalance", types(double.class, double.class, double.class), 100.0, 20.0, 80.0);
    verify(balance).setText(anyString());
    verify(total).setText(anyString());
    verify(held).setText(anyString());

    invoke(controller, "showWalletError", types(String.class, boolean.class), "error", true);
    verify(error).setText("error");
    verify(setup).setVisible(true);
    verify(errorBox).setVisible(true);
    invoke(controller, "hideWalletError");
    verify(errorBox).setVisible(false);
  }

  @Test
  void wallet_HandlesTransactionResponses() throws Exception {
    WalletController controller = new WalletController();
    set(controller, "lblBalance", mock(Label.class));
    set(controller, "lblTotalBalance", mock(Label.class));
    set(controller, "lblHeldBalance", mock(Label.class));
    set(controller, "lblTransactionError", mock(Label.class));
    set(controller, "bankErrorBox", mock(HBox.class));
    set(controller, "btnSetupBank", mock(Button.class));
    TextField amount = set(controller, "txtAmount", mock(TextField.class));
    TextField note = set(controller, "txtNote", mock(TextField.class));
    HttpResponse<String> response = response(200, "{\"id\":1,\"lastBalance\":500}");
    var json = new ObjectMapper().readTree(response.body());

    invoke(
        controller,
        "handleWalletTransactionResponse",
        types(
            HttpResponse.class,
            com.fasterxml.jackson.databind.JsonNode.class,
            double.class,
            TransactionType.class,
            String.class,
            boolean.class),
        response,
        json,
        100.0,
        TransactionType.DEPOSIT,
        "note",
        true);

    verify(amount).clear();
    verify(note).clear();
  }

  @Test
  void liveBids_UpdatesStatePricesAndCountdown() {
    LiveBidsController controller = new LiveBidsController();
    Label empty = set(controller, "lblEmptyState", mock(Label.class));
    VBox listPane = set(controller, "participatingListPane", mock(VBox.class));
    VBox detail = set(controller, "detailPane", mock(VBox.class));
    Label remaining = set(controller, "lblTimeRemaining", mock(Label.class));
    TextField bid = set(controller, "txtBidAmount", mock(TextField.class));
    @SuppressWarnings("unchecked")
    javafx.scene.control.TableView<AuctionItemDTO> table = set(controller, "tableParticipating", mock(javafx.scene.control.TableView.class));
    ObservableList<AuctionItemDTO> auctions = get(controller, "participatingAuctions");

    invoke(controller, "updateEmptyState");
    verify(empty).setVisible(true);
    invoke(controller, "setDetailVisible", types(boolean.class), true);
    verify(listPane).setVisible(false);
    verify(detail).setVisible(true);

    AuctionItemDTO auction = auction(1L, 10L, "Laptop", 100.0);
    auctions.add(auction);
    invoke(controller, "applyParticipatingPriceUpdate", types(AuctionPriceUpdateDTO.class), new AuctionPriceUpdateDTO(1L, 200.0));
    assertEquals(200.0, auction.getCurrentPrice());
    verify(table).refresh();
    invoke(controller, "applyParticipatingPriceUpdate", types(AuctionPriceUpdateDTO.class), new AuctionPriceUpdateDTO(null, 200.0));

    field(controller, "auctionEndTime", LocalDateTime.now().minusSeconds(1));
    invoke(controller, "updateCountdown");
    verify(remaining).setText("Đang chờ hệ thống kết toán...");
    verify(bid).setDisable(true);
    field(controller, "auctionEndTime", LocalDateTime.now().plusHours(1));
    invoke(controller, "updateCountdown");
    verify(remaining, Mockito.atLeastOnce()).setText(Mockito.startsWith("Thời gian còn lại: "));
    invoke(controller, "parseAuctionEndTime", types(String.class), "invalid");
    verify(remaining).setText("Thời gian còn lại: --:--:--");
  }

  @Test
  void sellerOrders_UpdatesStatePricesAndCountdown() {
    SellerOrderManagementController controller = new SellerOrderManagementController();
    Label empty = set(controller, "lblEmptyState", mock(Label.class));
    VBox detail = set(controller, "detailPane", mock(VBox.class));
    Label remaining = set(controller, "lblTimeRemaining", mock(Label.class));
    @SuppressWarnings("unchecked")
    javafx.scene.control.TableView<AuctionItemDTO> table = set(controller, "tableOrders", mock(javafx.scene.control.TableView.class));
    ObservableList<AuctionItemDTO> orders = get(controller, "runningOrders");

    invoke(controller, "updateEmptyState");
    verify(empty).setVisible(true);
    invoke(controller, "setDetailVisible", types(boolean.class), true);
    verify(detail).setVisible(true);

    AuctionItemDTO auction = auction(1L, 10L, "Laptop", 100.0);
    orders.add(auction);
    invoke(controller, "applyListPriceUpdate", types(AuctionPriceUpdateDTO.class), new AuctionPriceUpdateDTO(1L, 200.0));
    assertEquals(200.0, auction.getCurrentPrice());
    verify(table).refresh();

    field(controller, "auctionEndTime", LocalDateTime.now().minusSeconds(1));
    invoke(controller, "updateCountdown");
    verify(remaining).setText("Đang chờ hệ thống kết toán...");
    field(controller, "auctionEndTime", LocalDateTime.now().plusHours(1));
    invoke(controller, "updateCountdown");
    verify(remaining, Mockito.atLeastOnce()).setText(Mockito.startsWith("Thời gian còn lại: "));
    invoke(controller, "parseAuctionEndTime", types(String.class), "invalid");
    verify(remaining).setText("Thời gian còn lại: --:--:--");
  }

  @Test
  void revenueStatistics_RendersResponseAndErrors() {
    SellerRevenueStatisticsController controller = new SellerRevenueStatisticsController();
    Label total = set(controller, "lblTotalRevenue", mock(Label.class));
    Label month = set(controller, "lblMonthRevenue", mock(Label.class));
    Label sold = set(controller, "lblSoldOrders", mock(Label.class));
    Label average = set(controller, "lblAverageOrderValue", mock(Label.class));
    Label error = set(controller, "lblRevenueError", mock(Label.class));
    @SuppressWarnings("unchecked")
    BarChart<String, Number> chart = set(controller, "chartRevenue", mock(BarChart.class));
    ObservableList<XYChart.Series<String, Number>> chartData = FXCollections.observableArrayList();
    when(chart.getData()).thenReturn(chartData);

    invoke(
        controller,
        "renderRevenueStats",
        types(String.class),
        "{\"totalRevenue\":100,\"monthRevenue\":50,\"soldOrders\":2,\"averageOrderValue\":50,"
            + "\"monthlyRevenue\":[{\"timestamp\":\"06/2026\",\"price\":100}],"
            + "\"recentSales\":[{\"id\":1,\"transactionTime\":\"2026-06-02T10:00:00\",\"moneyIn\":100,\"lastBalance\":200,\"type\":\"Thu nhập\",\"note\":\"sale\"}]}");

    verify(total).setText(anyString());
    verify(month).setText(anyString());
    verify(sold).setText("2");
    verify(average).setText(anyString());
    assertEquals(1, chartData.size());
    invoke(controller, "showError", types(String.class), "error");
    verify(error).setVisible(true);
    invoke(controller, "hideError");
    verify(error, Mockito.atLeastOnce()).setVisible(false);
    assertTrue(((LocalDateTime) invoke(controller, "parseTime", types(String.class), new Object[] {null})).isBefore(LocalDateTime.now().plusSeconds(1)));
  }

  @Test
  void sellerProductDetail_InitializesMissingAndPresentProducts() {
    SellerProductDetailController controller = new SellerProductDetailController();
    Label name = set(controller, "lblItemName", mock(Label.class));
    set(controller, "lblItemId", mock(Label.class));
    set(controller, "lblItemType", mock(Label.class));
    set(controller, "lblDescription", mock(Label.class));
    set(controller, "lblStartingPrice", mock(Label.class));
    set(controller, "lblSoldPrice", mock(Label.class));
    set(controller, "lblBuyNowPrice", mock(Label.class));
    set(controller, "lblStatus", mock(Label.class));
    set(controller, "lblStartTime", mock(Label.class));
    set(controller, "lblEndTime", mock(Label.class));
    Button start = set(controller, "btnStart", mock(Button.class));
    set(controller, "btnEdit", mock(Button.class));
    set(controller, "btnDelete", mock(Button.class));

    SellerProductDetailController.setProduct(null);
    controller.initialize();
    verify(name).setText("Không tìm thấy sản phẩm");
    verify(start).setVisible(false);

    SellerProductDTO product = product(1L, 2L, "Laptop", AuctionState.OPEN);
    product.setItemType(ItemType.ELECTRONICS);
    product.setStartingPrice(100.0);
    SellerProductDetailController.setProduct(product);
    controller.initialize();
    verify(name).setText("Laptop");
    verify(start).setVisible(true);
    assertEquals("-", invoke(controller, "valueOrDash", types(Object.class), new Object[] {null}));
    assertEquals("fallback", invoke(controller, "formatPrice", types(Double.class, String.class), null, "fallback"));
    assertEquals("message", invoke(controller, "extractMessage", types(String.class), "{\"message\":\"message\"}"));
  }

  @Test
  void adminDashboard_FormatsCountsAndPaneVisibility() {
    AdminDashboardController controller = new AdminDashboardController();
    Pane dashboard = set(controller, "dashboardPane", mock(Pane.class));
    Pane users = set(controller, "usersPane", mock(Pane.class));
    Pane auctions = set(controller, "auctionsPane", mock(Pane.class));
    AuctionItemDTO open = auction(1L, 10L, "Laptop", 100.0);
    open.setStatus(AuctionState.OPEN);
    AuctionItemDTO running = auction(2L, 20L, "Phone", 200.0);
    running.setStatus(AuctionState.RUNNING);

    invoke(controller, "showPane", types(Pane.class), users);
    verify(dashboard).setVisible(false);
    verify(users).setVisible(true);
    verify(auctions).setVisible(false);
    assertEquals(
        1L,
        (long)
            invoke(
                controller,
                "countStatus",
                types(List.class, AuctionState.class),
                List.of(open, running),
                AuctionState.OPEN));
    assertTrue((boolean) invoke(controller, "canDelete", types(AuctionItemDTO.class), open));
    assertEquals(
        "RUNNING",
        invoke(controller, "displayStatus", types(AuctionState.class), AuctionState.RUNNING));
    assertEquals("-", invoke(controller, "joinName", types(String.class, String.class), null, null));
    assertEquals("Nguyen An", invoke(controller, "joinName", types(String.class, String.class), "Nguyen", "An"));
  }

  @Test
  void adminDashboard_SetsUpTablesAndRendersLoadedLists() {
    AdminDashboardController controller = new AdminDashboardController();
    set(controller, "colUserId", mock(TableColumn.class));
    set(controller, "colUsername", mock(TableColumn.class));
    set(controller, "colFullname", mock(TableColumn.class));
    set(controller, "colRole", mock(TableColumn.class));
    set(controller, "colAuctionId", mock(TableColumn.class));
    set(controller, "colItem", mock(TableColumn.class));
    set(controller, "colStatus", mock(TableColumn.class));
    set(controller, "colStartTime", mock(TableColumn.class));
    set(controller, "colEndTime", mock(TableColumn.class));
    set(controller, "colAuctionAction", mock(TableColumn.class));
    TableView<AuthResponse> usersTable = set(controller, "usersTable", mock(TableView.class));
    TableView<AuctionItemDTO> auctionsTable = set(controller, "auctionsTable", mock(TableView.class));
    Label users = set(controller, "statUsers", mock(Label.class));
    Label open = set(controller, "statOpenAuctions", mock(Label.class));
    Label active = set(controller, "statActiveAuctions", mock(Label.class));
    Label finished = set(controller, "statFinishedAuctions", mock(Label.class));

    invoke(controller, "setupUsersTable");
    invoke(controller, "setupAuctionsTable");

    AuthResponse user = new AuthResponse();
    user.setLastname("Nguyen");
    user.setFirstname("An");
    TableColumn.CellDataFeatures<AuthResponse, String> features =
        new TableColumn.CellDataFeatures<>(usersTable, mock(TableColumn.class), user);
    @SuppressWarnings("unchecked")
    javafx.beans.value.ObservableValue<String> fullName =
        invoke(
            controller,
            "lambda$setupUsersTable$0",
            types(TableColumn.CellDataFeatures.class),
            features);
    assertEquals(
        "Nguyen An",
        fullName.getValue());

    AuctionItemDTO openAuction = auction(1L, 10L, "Laptop", 100.0);
    openAuction.setStatus(AuctionState.OPEN);
    AuctionItemDTO runningAuction = auction(2L, 20L, "Phone", 200.0);
    runningAuction.setStatus(AuctionState.RUNNING);
    AuctionItemDTO finishedAuction = auction(3L, 30L, "Art", 300.0);
    finishedAuction.setStatus(AuctionState.FINISHED);
    invoke(controller, "lambda$refreshUsers$3", types(List.class), List.of(user));
    invoke(
        controller,
        "lambda$refreshAuctions$5",
        types(List.class),
        List.of(openAuction, runningAuction, finishedAuction));

    verify(usersTable).setItems(any());
    verify(auctionsTable).setItems(any());
    verify(users).setText("1");
    verify(open).setText("1");
    verify(active).setText("1");
    verify(finished).setText("1");
  }

  @Test
  void inventoryPurchaseHistoryAndNotifications_RenderServerResponses() {
    InventoryController inventory = new InventoryController();
    invoke(
        inventory,
        "lambda$loadWonItems$1",
        types(HttpResponse.class),
        response(
            200,
            "[{\"id\":1,\"name\":\"Laptop\",\"currentPrice\":100,\"status\":\"FINISHED\"}]"));
    ObservableList<AuctionItemDTO> wonItems = get(inventory, "wonItemList");
    assertEquals(1, wonItems.size());
    invoke(inventory, "loadWonItems");

    PurchaseHistoryController history = new PurchaseHistoryController();
    invoke(
        history,
        "lambda$loadHistory$2",
        types(HttpResponse.class),
        response(
            200,
            "[{\"id\":1,\"auctionId\":2,\"bidderId\":3,\"amount\":100,\"bidTime\":\"2026-06-02T10:00:00\"}]"));
    ObservableList<BidDTO> bids = get(history, "historyList");
    assertEquals(1, bids.size());
    invoke(history, "loadHistory");

    NotificationController notifications = new NotificationController();
    invoke(
        notifications,
        "handleNotificationsResponse",
        types(HttpResponse.class),
        response(
            200,
            "[{\"notiId\":1,\"message\":\"message\",\"createdAt\":\"2026-06-02T10:00:00\",\"read\":false,\"type\":\"BID_WON\"}]"));
    ObservableList<NotificationDTO> notificationList = get(notifications, "notiList");
    assertEquals(1, notificationList.size());
    invoke(notifications, "loadNotifications");
  }

  @Test
  void productList_UpdatesPricesAndUnsubscribes() {
    ProductListController controller = new ProductListController();
    @SuppressWarnings("unchecked")
    TableView<AuctionItemDTO> table = set(controller, "tableItems", mock(TableView.class));
    ObservableList<AuctionItemDTO> items = get(controller, "auctionList");
    AuctionItemDTO item = auction(1L, 10L, "Laptop", 100.0);
    items.add(item);

    invoke(controller, "applyPriceUpdate", types(AuctionPriceUpdateDTO.class), new AuctionPriceUpdateDTO(1L, 200.0));
    assertEquals(200.0, item.getCurrentPrice());
    verify(table).refresh();
    invoke(controller, "applyPriceUpdate", types(AuctionPriceUpdateDTO.class), new AuctionPriceUpdateDTO(null, 200.0));

    var first = mock(org.springframework.messaging.simp.stomp.StompSession.Subscription.class);
    var second = mock(org.springframework.messaging.simp.stomp.StompSession.Subscription.class);
    field(controller, "priceSubscription", first);
    field(controller, "listChangedSubscription", second);
    invoke(controller, "unsubscribeRealtimeUpdates");
    verify(first).unsubscribe();
    verify(second).unsubscribe();
  }

  @Test
  void liveBidsAndSellerOrders_RenderLoadedListsAndCharts() throws Exception {
    LiveBidsController live = new LiveBidsController();
    set(live, "lblEmptyState", mock(Label.class));
    ObservableList<AuctionItemDTO> liveItems = get(live, "participatingAuctions");
    invoke(
        live,
        "lambda$loadParticipatingAuctions$4",
        types(List.class),
        List.of(auction(1L, 10L, "Laptop", 100.0)));
    assertEquals(1, liveItems.size());
    XYChart.Series<String, Number> liveSeries = new XYChart.Series<>();
    field(live, "priceSeries", liveSeries);
    field(live, "currentAuctionId", 1L);
    invoke(
        live,
        "lambda$loadInitialChartData$9",
        types(Long.class, com.fasterxml.jackson.databind.JsonNode.class),
        1L,
        new ObjectMapper()
            .readTree("[{\"bidTime\":\"2026-06-02T10:00:00\",\"amount\":200}]"));
    assertEquals(1, liveSeries.getData().size());

    SellerOrderManagementController orders = new SellerOrderManagementController();
    set(orders, "lblEmptyState", mock(Label.class));
    ObservableList<AuctionItemDTO> orderItems = get(orders, "runningOrders");
    invoke(
        orders,
        "lambda$loadOrders$3",
        types(List.class),
        List.of(auction(1L, 10L, "Laptop", 100.0)));
    assertEquals(1, orderItems.size());
    XYChart.Series<String, Number> orderSeries = new XYChart.Series<>();
    field(orders, "priceSeries", orderSeries);
    field(orders, "currentAuctionId", 1L);
    invoke(
        orders,
        "lambda$loadInitialChartData$8",
        types(Long.class, com.fasterxml.jackson.databind.JsonNode.class),
        1L,
        new ObjectMapper()
            .readTree("[{\"bidTime\":\"2026-06-02T10:00:00\",\"amount\":200}]"));
    assertEquals(1, orderSeries.getData().size());
  }

  @Test
  void liveBidsAndSellerOrders_StopSubscriptions() {
    LiveBidsController live = new LiveBidsController();
    var livePrice = mock(org.springframework.messaging.simp.stomp.StompSession.Subscription.class);
    var liveStatus = mock(org.springframework.messaging.simp.stomp.StompSession.Subscription.class);
    var liveExtended = mock(org.springframework.messaging.simp.stomp.StompSession.Subscription.class);
    var liveListPrice = mock(org.springframework.messaging.simp.stomp.StompSession.Subscription.class);
    var liveListChanged = mock(org.springframework.messaging.simp.stomp.StompSession.Subscription.class);
    field(live, "priceSubscription", livePrice);
    field(live, "statusSubscription", liveStatus);
    field(live, "extendedSubscription", liveExtended);
    field(live, "listPriceSubscription", liveListPrice);
    field(live, "listChangedSubscription", liveListChanged);
    invoke(live, "stopRealtimeUpdates");
    verify(livePrice).unsubscribe();
    verify(liveStatus).unsubscribe();
    verify(liveExtended).unsubscribe();
    verify(liveListPrice).unsubscribe();
    verify(liveListChanged).unsubscribe();

    SellerOrderManagementController orders = new SellerOrderManagementController();
    var orderPrice = mock(org.springframework.messaging.simp.stomp.StompSession.Subscription.class);
    var orderStatus = mock(org.springframework.messaging.simp.stomp.StompSession.Subscription.class);
    var orderExtended = mock(org.springframework.messaging.simp.stomp.StompSession.Subscription.class);
    var orderListPrice = mock(org.springframework.messaging.simp.stomp.StompSession.Subscription.class);
    var orderListChanged = mock(org.springframework.messaging.simp.stomp.StompSession.Subscription.class);
    field(orders, "priceSubscription", orderPrice);
    field(orders, "statusSubscription", orderStatus);
    field(orders, "extendedSubscription", orderExtended);
    field(orders, "listPriceSubscription", orderListPrice);
    field(orders, "listChangedSubscription", orderListChanged);
    invoke(orders, "stopRealtimeUpdates");
    verify(orderPrice).unsubscribe();
    verify(orderStatus).unsubscribe();
    verify(orderExtended).unsubscribe();
    verify(orderListPrice).unsubscribe();
    verify(orderListChanged).unsubscribe();
  }

  @Test
  void bankProfileAndAddressControllers_LoadCurrentUser() {
    AuthResponse user = new AuthResponse();
    user.setUsername("user");
    user.setFirstname("An");
    user.setLastname("Nguyen");
    user.setBankName("BIDV");
    user.setAccountName("An Nguyen");
    user.setBankAccount("123");
    user.setAddress("Address");
    AuctionManager.getInstance().setCurrentUser(user);

    BankSettingController bank = new BankSettingController();
    @SuppressWarnings("unchecked")
    ComboBox<String> bankName = set(bank, "cbBankName", mock(ComboBox.class));
    TextField bankEditor = mock(TextField.class);
    when(bankName.getEditor()).thenReturn(bankEditor);
    TextField accountName = set(bank, "txtAccountName", mock(TextField.class));
    TextField bankAccount = set(bank, "txtBankAccount", mock(TextField.class));
    invoke(bank, "loadUserData");
    verify(accountName).setText("An Nguyen");
    verify(bankAccount).setText("123");
    verify(bankName).setValue("BIDV");
    verify(bankEditor).setText("BIDV");

    ProfileSettingController profile = new ProfileSettingController();
    TextField username = set(profile, "txtUsername", mock(TextField.class));
    TextField lastname = set(profile, "txtLastName", mock(TextField.class));
    TextField firstname = set(profile, "txtFirstName", mock(TextField.class));
    profile.initialize();
    verify(username).setText("user");
    verify(lastname).setText("Nguyen");
    verify(firstname).setText("An");

    AddressSettingController address = new AddressSettingController();
    TextArea textArea = set(address, "txtAddress", mock(TextArea.class));
    address.initialize();
    verify(textArea).setText("Address");
  }

  @Test
  void bankController_InitializesFilteringAndSelectionListeners() throws Exception {
    BankSettingController bank = new BankSettingController();
    @SuppressWarnings("unchecked")
    ComboBox<String> bankName = set(bank, "cbBankName", mock(ComboBox.class));
    TextField editor = mock(TextField.class);
    SimpleStringProperty editorText = new SimpleStringProperty("");
    SimpleObjectProperty<String> selectedBank = new SimpleObjectProperty<>();
    when(bankName.getEditor()).thenReturn(editor);
    when(bankName.valueProperty()).thenReturn(selectedBank);
    when(editor.textProperty()).thenReturn(editorText);
    when(editor.isFocused()).thenReturn(true);
    set(bank, "txtAccountName", mock(TextField.class));
    set(bank, "txtBankAccount", mock(TextField.class));

    bank.initialize();
    editorText.set("mb");
    selectedBank.set("BIDV");
    Thread.sleep(100);

    verify(bankName).setItems(any());
    verify(bankName, Mockito.atLeastOnce()).hide();
    verify(bankName).show();
    verify(editor, Mockito.atLeastOnce()).setText(anyString());
  }

  @Test
  void registerController_InitializesAndRejectsInvalidFields() throws Exception {
    RegisterController register = new RegisterController();
    invoke(register, "onRegisterButtonClicked");

    TextField firstname = set(register, "txtfirstname", mock(TextField.class));
    TextField lastname = set(register, "txtlastname", mock(TextField.class));
    TextField username = set(register, "txtUsername", mock(TextField.class));
    PasswordField password = set(register, "txtPassword", mock(PasswordField.class));
    Label status = set(register, "lblStatus", mock(Label.class));
    when(firstname.getText()).thenReturn("");
    when(lastname.getText()).thenReturn("Nguyen");
    when(username.getText()).thenReturn("user");
    when(password.getText()).thenReturn("password");
    invoke(register, "onRegisterButtonClicked");
    invoke(register, "lambda$showError$3", types(String.class), "error");

    when(firstname.getText()).thenReturn("An");
    when(username.getText()).thenReturn("nguyễn");
    invoke(register, "onRegisterButtonClicked");
    register.initialize();
    Thread.sleep(100);

    verify(status, Mockito.atLeastOnce()).setText(anyString());
    verify(status, Mockito.atLeastOnce()).setVisible(true);
    verify(status, Mockito.atLeastOnce()).setManaged(true);
    verify(username).setOnKeyPressed(any());
    verify(password).setOnKeyPressed(any());
  }

  @Test
  void loginAndRegistrationControllers_ValidateLocally() {
    LoginController login = new LoginController();
    TextField username = set(login, "txtUsername", mock(TextField.class));
    PasswordField password = set(login, "txtPassword", mock(PasswordField.class));
    Label usernameError = set(login, "lblUserError", mock(Label.class));
    Label passwordError = set(login, "lblPassError", mock(Label.class));
    Label generalError = set(login, "lblGeneralError", mock(Label.class));
    when(username.getText()).thenReturn("");
    when(password.getText()).thenReturn("");
    login.initialize();
    login.onLoginButtonClicked();
    verify(username).setOnKeyPressed(any());
    verify(password).setOnKeyPressed(any());
    verify(usernameError).setVisible(true);
    verify(passwordError).setVisible(true);
    login.setUsername("user");
    verify(username).setText("user");
    verify(password).requestFocus();
    invoke(login, "hideAllErrors");
    verify(generalError, Mockito.atLeastOnce()).setVisible(false);

    SellerRegistrationController registration = new SellerRegistrationController();
    VBox intro = set(registration, "introPane", mock(VBox.class));
    VBox form = set(registration, "formPane", mock(VBox.class));
    TextField storeName = set(registration, "txtStoreName", mock(TextField.class));
    Label error = set(registration, "lblError", mock(Label.class));
    registration.initialize();
    verify(intro).setVisible(true);
    verify(form).setVisible(false);
    registration.onStartRegistrationButtonClicked();
    verify(form).setVisible(true);
    when(storeName.getText()).thenReturn("");
    registration.onConfirmButtonClicked();
    verify(error).setText("Vui lòng nhập tên Cửa hàng/Shop!");
  }

  @Test
  void settingsControllers_StopCleanlyWithoutLoggedInUser() {
    BankSettingController bank = new BankSettingController();
    @SuppressWarnings("unchecked")
    ComboBox<String> bankName = set(bank, "cbBankName", mock(ComboBox.class));
    TextField editor = mock(TextField.class);
    when(bankName.getEditor()).thenReturn(editor);
    when(editor.getText()).thenReturn("BIDV");
    text(bank, "txtAccountName", "An Nguyen");
    text(bank, "txtBankAccount", "123");
    bank.saveBank();

    ProfileSettingController profile = new ProfileSettingController();
    text(profile, "txtFirstName", "An");
    text(profile, "txtLastName", "Nguyen");
    profile.saveProfile();

    AddressSettingController address = new AddressSettingController();
    TextArea textArea = set(address, "txtAddress", mock(TextArea.class));
    when(textArea.getText()).thenReturn("Address");
    address.onUpdateAddress();
    address.onDeleteAddress();
    verify(textArea).clear();

    SellerRegistrationController registration = new SellerRegistrationController();
    set(registration, "lblError", mock(Label.class));
    text(registration, "txtStoreName", "Shop");
    registration.onConfirmButtonClicked();

    AuthResponse currentUser = new AuthResponse();
    AuctionManager.getInstance().setCurrentUser(currentUser);
    registration.onConfirmButtonClicked();
  }

  @Test
  void baseDashboard_HandlesLocalMenuAndMissingContentArea() {
    TestBaseDashboard dashboard = new TestBaseDashboard();
    Button active = mock(Button.class);
    Button inactive = mock(Button.class);

    dashboard.updateMenu(active, inactive, null);
    dashboard.onHomeButtonClicked();
    dashboard.onWalletButtonClicked();
    dashboard.onNotificationsClicked();

    verify(active).setStyle(anyString());
    verify(inactive).setStyle(anyString());
  }

  @Test
  void dashboardsAndSettings_UpdateLocalStateWithoutLoadingFxml() {
    AuthResponse user = new AuthResponse();
    user.setFirstname("An");
    user.setUsername("user");
    user.setStoreName("Shop");
    AuctionManager.getInstance().setCurrentUser(user);

    TestSellerDashboard seller = new TestSellerDashboard();
    set(seller, "lblSellerName", mock(Label.class));
    set(seller, "lblSellerRole", mock(Label.class));
    set(seller, "lblWelcome", mock(Label.class));
    set(seller, "btnProducts", mock(Button.class));
    set(seller, "btnOrders", mock(Button.class));
    set(seller, "btnRevenue", mock(Button.class));
    set(seller, "btnWallet", mock(Button.class));
    set(seller, "btnNotifications", mock(Button.class));
    seller.initialize();
    assertEquals("/client/user/seller/manage_auctions_view.fxml", seller.loadedPath);
    seller.onAddAuctionClicked();
    seller.onEditProductClicked(product(1L, 2L, "Product", AuctionState.OPEN));
    seller.onProductClicked(product(1L, 2L, "Product", AuctionState.OPEN));
    seller.onManageOrdersClicked();
    seller.onRevenueStatisticsClicked();
    seller.onWalletButtonClicked();
    seller.onNotificationsClicked();

    TestBidderDashboard bidder = new TestBidderDashboard();
    set(bidder, "lblWelcome", mock(Label.class));
    set(bidder, "btnHome", mock(Button.class));
    set(bidder, "btnProductList", mock(Button.class));
    set(bidder, "btnWallet", mock(Button.class));
    bidder.initialize();
    bidder.onProductListButtonClicked();
    bidder.onLiveBidsClicked();
    bidder.onPurchaseHistoryClicked();
    bidder.onInventoryClicked();
    bidder.onWalletButtonClicked();
    assertEquals("/client/user/wallet_view.fxml", bidder.loadedPath);

    SettingsController settings = new SettingsController();
    Button profile = set(settings, "btnProfile", mock(Button.class));
    Button bank = set(settings, "btnBank", mock(Button.class));
    Button address = set(settings, "btnAddress", mock(Button.class));
    invoke(settings, "updateButtonStyles", types(Button.class), bank);
    verify(profile).setStyle(anyString());
    verify(bank, Mockito.times(2)).setStyle(anyString());
    verify(address).setStyle(anyString());
  }

  @Test
  void realtimeControllers_RegisterWebSocketSubscriptionsAndHandlePayloads() throws Exception {
    StompSession session = mock(StompSession.class);
    StompSession.Subscription subscription = mock(StompSession.Subscription.class);
    List<StompFrameHandler> handlers = new ArrayList<>();
    when(session.isConnected()).thenReturn(true);
    when(session.subscribe(anyString(), any(StompFrameHandler.class)))
        .thenAnswer(
            invocation -> {
              handlers.add(invocation.getArgument(1));
              return subscription;
            });
    field(WebSocketClientManager.getInstance(), "stompSession", session);

    LiveBidsController live = new LiveBidsController();
    @SuppressWarnings("unchecked")
    TableView<AuctionItemDTO> liveTable = set(live, "tableParticipating", mock(TableView.class));
    set(live, "lblCurrentPrice", mock(Label.class));
    set(live, "lblTimeRemaining", mock(Label.class));
    set(live, "txtBidAmount", mock(TextField.class));
    field(live, "priceSeries", new XYChart.Series<String, Number>());
    field(live, "currentAuctionId", 1L);
    ObservableList<AuctionItemDTO> participatingAuctions = get(live, "participatingAuctions");
    participatingAuctions.add(auction(1L, 10L, "Laptop", 100.0));

    invoke(live, "subscribeToParticipatingListUpdates");
    assertEquals(2, handlers.size());
    handlers.get(0).getPayloadType(null);
    handlers.get(0).handleFrame(null, new AuctionPriceUpdateDTO(1L, 200.0));
    handlers.get(1).getPayloadType(null);

    handlers.clear();
    invoke(live, "subscribeToAuctionUpdates");
    assertEquals(3, handlers.size());
    handlers.get(0).getPayloadType(null);
    handlers.get(0).handleFrame(null, 250.0);
    handlers.get(1).getPayloadType(null);
    handlers.get(1).handleFrame(null, AuctionState.OPEN);
    handlers.get(2).getPayloadType(null);
    handlers.get(2).handleFrame(null, "\"2026-06-02T10:00:00\"");

    SellerOrderManagementController orders = new SellerOrderManagementController();
    @SuppressWarnings("unchecked")
    TableView<AuctionItemDTO> orderTable = set(orders, "tableOrders", mock(TableView.class));
    set(orders, "lblCurrentPrice", mock(Label.class));
    set(orders, "lblTimeRemaining", mock(Label.class));
    set(orders, "detailPane", mock(VBox.class));
    field(orders, "priceSeries", new XYChart.Series<String, Number>());
    field(orders, "currentAuctionId", 1L);
    ObservableList<AuctionItemDTO> runningOrders = get(orders, "runningOrders");
    runningOrders.add(auction(1L, 10L, "Laptop", 100.0));

    handlers.clear();
    invoke(orders, "subscribeToListUpdates");
    assertEquals(2, handlers.size());
    handlers.get(0).getPayloadType(null);
    handlers.get(0).handleFrame(null, new AuctionPriceUpdateDTO(1L, 300.0));
    handlers.get(1).getPayloadType(null);

    handlers.clear();
    invoke(orders, "subscribeToAuctionUpdates");
    assertEquals(3, handlers.size());
    handlers.get(0).getPayloadType(null);
    handlers.get(0).handleFrame(null, 350.0);
    handlers.get(1).getPayloadType(null);
    handlers.get(1).handleFrame(null, AuctionState.OPEN);
    handlers.get(2).getPayloadType(null);
    handlers.get(2).handleFrame(null, "\"2026-06-02T10:00:00\"");

    ProductListController products = new ProductListController();
    set(products, "tableItems", mock(TableView.class));
    handlers.clear();
    invoke(products, "subscribeToRealtimeUpdates");
    assertEquals(2, handlers.size());
    handlers.get(0).getPayloadType(null);
    handlers.get(0).handleFrame(null, new AuctionPriceUpdateDTO(null, 10.0));
    handlers.get(1).getPayloadType(null);

    Thread.sleep(100);
    verify(liveTable, Mockito.atLeastOnce()).refresh();
    verify(orderTable, Mockito.atLeastOnce()).refresh();
  }

  @Test
  void simpleControllers_InitializeWithoutLoggedInUser() {
    WalletController wallet = new WalletController();
    set(wallet, "lblBalance", mock(Label.class));
    set(wallet, "lblTotalBalance", mock(Label.class));
    set(wallet, "lblHeldBalance", mock(Label.class));
    set(wallet, "lblTransactionError", mock(Label.class));
    set(wallet, "bankErrorBox", mock(HBox.class));
    set(wallet, "btnSetupBank", mock(Button.class));
    set(wallet, "txtAmount", mock(TextField.class));
    set(wallet, "txtNote", mock(TextField.class));
    TextField search = set(wallet, "txtSearch", mock(TextField.class));
    ComboBox<String> timeFilter = set(wallet, "cbTimeFilter", mock(ComboBox.class));
    when(search.textProperty()).thenReturn(new SimpleStringProperty(""));
    when(timeFilter.valueProperty()).thenReturn(new SimpleObjectProperty<>());
    set(wallet, "btnFilterIn", mock(Button.class));
    set(wallet, "btnFilterOut", mock(Button.class));
    set(wallet, "tableWalletHistory", mock(TableView.class));
    set(wallet, "colTransactionId", mock(TableColumn.class));
    set(wallet, "colTransactionTime", mock(TableColumn.class));
    set(wallet, "colTransactionType", mock(TableColumn.class));
    set(wallet, "colMoneyIn", mock(TableColumn.class));
    set(wallet, "colMoneyOut", mock(TableColumn.class));
    set(wallet, "colLastBalance", mock(TableColumn.class));
    set(wallet, "colTransactionNote", mock(TableColumn.class));
    wallet.initialize();

    SellerRevenueStatisticsController revenue = new SellerRevenueStatisticsController();
    set(revenue, "lblRevenueError", mock(Label.class));
    set(revenue, "tableRecentSales", mock(TableView.class));
    set(revenue, "chartRevenue", mock(BarChart.class));
    set(revenue, "colSaleId", mock(TableColumn.class));
    set(revenue, "colSaleTime", mock(TableColumn.class));
    set(revenue, "colSaleAmount", mock(TableColumn.class));
    set(revenue, "colSaleBalance", mock(TableColumn.class));
    set(revenue, "colSaleNote", mock(TableColumn.class));
    revenue.initialize();

    InventoryController inventory = new InventoryController();
    set(inventory, "tableInventory", mock(TableView.class));
    set(inventory, "colId", mock(TableColumn.class));
    set(inventory, "colName", mock(TableColumn.class));
    set(inventory, "colFinalPrice", mock(TableColumn.class));
    set(inventory, "colStatus", mock(TableColumn.class));
    inventory.initialize();

    PurchaseHistoryController history = new PurchaseHistoryController();
    set(history, "tableHistory", mock(TableView.class));
    set(history, "colId", mock(TableColumn.class));
    set(history, "colAuctionId", mock(TableColumn.class));
    set(history, "colAmount", mock(TableColumn.class));
    set(history, "colTime", mock(TableColumn.class));
    history.initialize();

    NotificationController notifications = new NotificationController();
    set(notifications, "listNotifications", mock(ListView.class));
    notifications.initialize();
  }

  @Test
  void notificationCell_RendersUnreadAndEmptyItems() {
    NotificationController notifications = new NotificationController();
    Object cell =
        invoke(
            notifications,
            "lambda$initialize$0",
            types(ListView.class),
            mock(ListView.class));
    NotificationDTO notification = new NotificationDTO();
    notification.setMessage("Message");
    notification.setType(NotificationType.BID_WON);
    notification.setCreatedAt(LocalDateTime.of(2026, 6, 2, 10, 0));

    invoke(cell, "updateItem", types(NotificationDTO.class, boolean.class), notification, false);
    invoke(cell, "updateItem", types(NotificationDTO.class, boolean.class), null, true);
  }

  @Test
  void sellerProductList_ConfiguresLocalControls() {
    SellerProductListController controller = new SellerProductListController();
    TextField search = set(controller, "txtSearch", mock(TextField.class));
    when(search.textProperty()).thenReturn(new SimpleStringProperty(""));
    set(controller, "btnOpenStatus", mock(Button.class));
    set(controller, "btnRunningStatus", mock(Button.class));
    set(controller, "btnFinishedStatus", mock(Button.class));
    set(controller, "btnCancelledStatus", mock(Button.class));
    set(controller, "tableProducts", mock(TableView.class));

    invoke(controller, "configureSearch");
    invoke(controller, "configureStatusButtons");
    invoke(controller, "configureProductSelection");

    SellerProductDTO product = product(1L, 10L, "Laptop", AuctionState.OPEN);
    TableColumn.CellDataFeatures<SellerProductDTO, String> features =
        new TableColumn.CellDataFeatures<>(mock(TableView.class), mock(TableColumn.class), product);
    @SuppressWarnings("unchecked")
    javafx.beans.value.ObservableValue<String> status =
        invoke(
            controller,
            "lambda$initialize$2",
            types(TableColumn.CellDataFeatures.class),
            features);
    assertEquals("OPEN", status.getValue());
  }

  private static class TestSellerDashboard extends SellerDashboardController {
    private String loadedPath;

    @Override
    protected void loadSubView(String fxmlPath) {
      loadedPath = fxmlPath;
    }
  }

  private static class TestBaseDashboard extends BaseDashboardController {
    void updateMenu(Button active, Button... inactive) {
      setActiveMenu(active, inactive);
    }
  }

  private static class TestBidderDashboard extends BidderDashboardController {
    private String loadedPath;

    @Override
    protected void loadSubView(String fxmlPath) {
      loadedPath = fxmlPath;
    }
  }

  private SellerAddProductController sellerAddController() {
    SellerAddProductController controller = new SellerAddProductController();
    set(controller, "lblFormTitle", mock(Label.class));
    text(controller, "txtName", "Product");
    area(controller, "txtDescription", "Description");
    text(controller, "txtStartingPrice", "1000");
    text(controller, "txtBuyNowPrice", "2000");
    ComboBox<ItemType> comboBox = set(controller, "cbItemType", mock(ComboBox.class));
    when(comboBox.getValue()).thenReturn(ItemType.ART);
    DatePicker startDate = set(controller, "dpStartDate", mock(DatePicker.class));
    DatePicker endDate = set(controller, "dpEndDate", mock(DatePicker.class));
    when(startDate.getValue()).thenReturn(LocalDate.now().plusDays(1));
    when(endDate.getValue()).thenReturn(LocalDate.now().plusDays(1));
    text(controller, "txtStartTime", "10:00");
    text(controller, "txtEndTime", "11:00");
    set(controller, "artFields", mock(VBox.class));
    text(controller, "txtArtistName", "Artist");
    text(controller, "txtMedium", "Oil");
    text(controller, "txtDimensions", "10x10");
    text(controller, "txtCreationYear", "2020");
    set(controller, "electronicsFields", mock(VBox.class));
    text(controller, "txtElectronicsBrand", "Brand");
    text(controller, "txtElectronicsModel", "Model");
    text(controller, "txtCondition", "New");
    set(controller, "vehicleFields", mock(VBox.class));
    text(controller, "txtManufactureYear", "2024");
    text(controller, "txtFuelType", "Electric");
    set(controller, "fashionFields", mock(VBox.class));
    text(controller, "txtFashionBrand", "Brand");
    text(controller, "txtFashionSize", "M");
    text(controller, "txtFashionMaterial", "Cotton");
    text(controller, "txtFashionGender", "Unisex");
    set(controller, "jewelryFields", mock(VBox.class));
    text(controller, "txtJewelryMaterial", "Gold");
    text(controller, "txtWeight", "2");
    text(controller, "txtGemstone", "Ruby");
    set(controller, "btnStartAuction", mock(Button.class));
    return controller;
  }

  private void login(Long id) {
    AuthResponse response = new AuthResponse();
    response.setUserId(id);
    response.setAccountName("Account");
    response.setBankAccount("123");
    response.setBalance(1000.0);
    AuctionManager.getInstance().setCurrentUser(response);
  }

  private TextField text(Object target, String name, String value) {
    TextField field = set(target, name, mock(TextField.class));
    when(field.getText()).thenReturn(value);
    return field;
  }

  private TextArea area(Object target, String name, String value) {
    TextArea field = set(target, name, mock(TextArea.class));
    when(field.getText()).thenReturn(value);
    return field;
  }

  private SellerProductDTO product(Long listingId, Long itemId, String name, AuctionState status) {
    SellerProductDTO product = new SellerProductDTO();
    product.setListingId(listingId);
    product.setItemId(itemId);
    product.setItemName(name);
    product.setStatus(status);
    return product;
  }

  private AuctionItemDTO auction(Long id, Long itemId, String name, double price) {
    AuctionItemDTO auction = new AuctionItemDTO();
    auction.setId(id);
    auction.setItemId(itemId);
    auction.setName(name);
    auction.setCurrentPrice(price);
    return auction;
  }

  @SuppressWarnings("unchecked")
  private <T> T get(Object target, String name) {
    try {
      Field field = findField(target.getClass(), name);
      field.setAccessible(true);
      return (T) field.get(target);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  private <T> T set(Object target, String name, T value) {
    field(target, name, value);
    return value;
  }

  private void field(Object target, String name, Object value) {
    try {
      Field field = findField(target.getClass(), name);
      field.setAccessible(true);
      field.set(target, value);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  private Field findField(Class<?> type, String name) throws NoSuchFieldException {
    Class<?> current = type;
    while (current != null) {
      try {
        return current.getDeclaredField(name);
      } catch (NoSuchFieldException ignored) {
        current = current.getSuperclass();
      }
    }
    throw new NoSuchFieldException(name);
  }

  private Class<?>[] types(Class<?>... types) {
    return types;
  }

  @SuppressWarnings("unchecked")
  private <T> T invoke(Object target, String name, Object... args) {
    return invoke(target, name, new Class<?>[0], args);
  }

  @SuppressWarnings("unchecked")
  private <T> T invoke(Object target, String name, Class<?>[] parameterTypes, Object... args) {
    try {
      Method method = target.getClass().getDeclaredMethod(name, parameterTypes);
      method.setAccessible(true);
      return (T) method.invoke(target, args);
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw new IllegalStateException(e.getCause());
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  @SuppressWarnings("unchecked")
  private HttpResponse<String> response(int status, String body) {
    HttpResponse<String> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(status);
    when(response.body()).thenReturn(body);
    return response;
  }
}
