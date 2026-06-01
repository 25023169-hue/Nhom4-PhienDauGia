package client.controller;

import client.ServerConnectionException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import client.pattern.ClientHttp;
import client.pattern.WebSocketClientManager;
import common.Constants;
import common.dto.AuctionItemDTO;
import common.dto.AuctionPriceUpdateDTO;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
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
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;

public class ProductListController {

  @FXML private TableView<AuctionItemDTO> tableItems;
  @FXML private TableColumn<AuctionItemDTO, Long> colId;
  @FXML private TableColumn<AuctionItemDTO, String> colName;
  @FXML private TableColumn<AuctionItemDTO, Double> colCurrentPrice;
  @FXML private TableColumn<AuctionItemDTO, String> colEndTime;

  // --- PHẦN KHAI BÁO THÊM MỚI ---
  private final ObservableList<AuctionItemDTO> auctionList = FXCollections.observableArrayList();

  private final ObjectMapper objectMapper = ClientHttp.mapper();
  private final NumberFormat currencyFormat =
      NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));
  private StompSession.Subscription priceSubscription;
  private StompSession.Subscription listChangedSubscription;

  @FXML
  public void initialize() {
    // 1. Gắn dữ liệu vào các cột (Code cũ giữ nguyên)
    colId.setCellValueFactory(new PropertyValueFactory<>("id"));
    colName.setCellValueFactory(new PropertyValueFactory<>("name"));
    colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
    colEndTime.setCellValueFactory(new PropertyValueFactory<>("endTime"));

    // 2. Định dạng cột giá tiền sang chuẩn VNĐ (Code thêm mới)
    colCurrentPrice.setCellFactory(
        tc ->
            new TableCell<AuctionItemDTO, Double>() {
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

    // 3. Chọn sản phẩm bằng một lần nhấn để vào phòng đấu giá
    tableItems.setOnMouseClicked(
        (MouseEvent event) -> {
          if (event.getClickCount() == 1) {
            AuctionItemDTO selected = tableItems.getSelectionModel().getSelectedItem();
            if (selected != null) {
              // Truyền dữ liệu sang màn hình LiveBids
              LiveBidsController.selectedAuctionId = selected.getId();
              LiveBidsController.selectedAuctionName = selected.getName();
              LiveBidsController.selectedInitialPrice = selected.getCurrentPrice();
              LiveBidsController.selectedEndTime = selected.getEndTime();

              // Chuyển tab
              BidderDashboardController.getInstance().onLiveBidsClicked();
            }
          }
        });

    // 4. Load dữ liệu
    tableItems.setItems(auctionList);
    loadProductsFromServer();
    tableItems
        .sceneProperty()
        .addListener(
            (observable, oldScene, newScene) -> {
              if (newScene == null) {
                unsubscribeRealtimeUpdates();
              } else {
                subscribeToRealtimeUpdates();
              }
            });
    if (tableItems.getScene() != null) {
      subscribeToRealtimeUpdates();
    }
  }

  // --- CÁC HÀM XỬ LÝ API THÊM MỚI ---
  private void loadProductsFromServer() {
    new Thread(
            () -> {
              try {
                HttpRequest request =
                    HttpRequest.newBuilder()
                        .uri(URI.create(Constants.AUCTION_ENDPOINT + "/running"))
                        .GET()
                        .build();

                HttpResponse<String> response =
                    ClientHttp.send(request);

                Platform.runLater(
                    () -> {
                      if (response.statusCode() == 200) {
                        try {
                          List<AuctionItemDTO> items =
                              objectMapper.readValue(
                                  response.body(), new TypeReference<List<AuctionItemDTO>>() {});
                          auctionList.setAll(items);
                        } catch (Exception e) {
                          System.err.println("Lỗi Parse JSON danh sách sản phẩm.");
                        }
                      } else {
                        showAlert("Không thể tải danh sách sản phẩm lúc này!");
                      }
                    });
              } catch (Exception e) {
                Platform.runLater(() -> showAlert(ServerConnectionException.MESSAGE));
              }
            })
        .start();
  }

  private void subscribeToRealtimeUpdates() {
    if (priceSubscription != null || listChangedSubscription != null) return;

    WebSocketClientManager.getInstance().connect();
    StompSession session = WebSocketClientManager.getInstance().getSession();
    if (session == null || !session.isConnected()) return;

    priceSubscription =
        session.subscribe(
            "/topic/auctions/prices",
            new StompSessionHandlerAdapter() {
              @Override
              public Type getPayloadType(StompHeaders headers) {
                return AuctionPriceUpdateDTO.class;
              }

              @Override
              public void handleFrame(StompHeaders headers, Object payload) {
                if (!(payload instanceof AuctionPriceUpdateDTO update)) return;
                Platform.runLater(() -> applyPriceUpdate(update));
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
                loadProductsFromServer();
              }
            });
  }

  private void unsubscribeRealtimeUpdates() {
    if (priceSubscription != null) {
      priceSubscription.unsubscribe();
      priceSubscription = null;
    }
    if (listChangedSubscription != null) {
      listChangedSubscription.unsubscribe();
      listChangedSubscription = null;
    }
  }

  private void applyPriceUpdate(AuctionPriceUpdateDTO update) {
    if (update.getAuctionId() == null || update.getCurrentPrice() == null) return;

    for (AuctionItemDTO item : auctionList) {
      if (update.getAuctionId().equals(item.getId())) {
        item.setCurrentPrice(update.getCurrentPrice());
        tableItems.refresh();
        return;
      }
    }
  }

  private void showAlert(String msg) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setHeaderText(null);
    alert.setContentText(msg);
    alert.showAndWait();
  }
}
