package client.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import client.pattern.AuctionManager;
import client.pattern.ClientHttp;
import common.Constants;
import common.dto.AuctionItemDTO;
import common.enums.AuctionState;
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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class InventoryController {
  @FXML private TableView<AuctionItemDTO> tableInventory;
  @FXML private TableColumn<AuctionItemDTO, Long> colId;
  @FXML private TableColumn<AuctionItemDTO, String> colName;
  @FXML private TableColumn<AuctionItemDTO, Double> colFinalPrice;
  @FXML private TableColumn<AuctionItemDTO, AuctionState> colStatus;

  private final ObservableList<AuctionItemDTO> wonItemList = FXCollections.observableArrayList();

  private final ObjectMapper objectMapper = ClientHttp.mapper();
  private final NumberFormat currencyFormat =
      NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));

  @FXML
  public void initialize() {
    colId.setCellValueFactory(new PropertyValueFactory<>("id"));
    colName.setCellValueFactory(new PropertyValueFactory<>("name"));
    colFinalPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
    colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

    colFinalPrice.setCellFactory(
        tc ->
            new TableCell<AuctionItemDTO, Double>() {
              @Override
              protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) setText(null);
                else setText(currencyFormat.format(price));
              }
            });

    tableInventory.setItems(wonItemList);
    loadWonItems();
  }

  private void loadWonItems() {
    Long bidderId = AuctionManager.getInstance().getId();
    if (bidderId == null) return;

    new Thread(
            () -> {
              try {
                HttpRequest request =
                    HttpRequest.newBuilder()
                        .uri(URI.create(Constants.BASE_URL + "/inventory/" + bidderId))
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
                          wonItemList.setAll(items);
                        } catch (Exception e) {
                          e.printStackTrace();
                        }
                      }
                    });
              } catch (Exception e) {
                e.printStackTrace();
              }
            })
        .start();
  }
}
