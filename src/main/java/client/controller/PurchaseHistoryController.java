package client.controller;

import server.exception.ServerConnectionException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import client.pattern.AuctionManager;
import client.pattern.ClientHttp;
import common.Constants;
import common.dto.BidDTO;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

public class PurchaseHistoryController {
  @FXML private TableView<BidDTO> tableHistory;
  @FXML private TableColumn<BidDTO, Long> colId;
  @FXML private TableColumn<BidDTO, Long> colAuctionId;
  @FXML private TableColumn<BidDTO, Double> colAmount;

  @FXML private TableColumn<BidDTO, LocalDateTime> colTime;

  private final ObservableList<BidDTO> historyList = FXCollections.observableArrayList();

  private final ObjectMapper objectMapper = ClientHttp.mapper();

  private final NumberFormat currencyFormat =
      NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));
  private final DateTimeFormatter timeFormatter =
      DateTimeFormatter.ofPattern("HH:mm:ss - dd/MM/yyyy");

  @FXML
  public void initialize() {

    colId.setCellValueFactory(new PropertyValueFactory<>("id"));
    colAuctionId.setCellValueFactory(new PropertyValueFactory<>("auctionId"));
    colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
    colTime.setCellValueFactory(new PropertyValueFactory<>("bidTime"));

    colAmount.setCellFactory(
        tc ->
            new TableCell<BidDTO, Double>() {
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

    colTime.setCellFactory(
        tc ->
            new TableCell<BidDTO, LocalDateTime>() {
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

    new Thread(
            () -> {
              try {
                HttpRequest request =
                    HttpRequest.newBuilder()
                        .uri(URI.create(Constants.BASE_URL + "/bids/history/" + bidderId))
                        .GET()
                        .build();

                HttpResponse<String> response =
                    ClientHttp.send(request);

                Platform.runLater(
                    () -> {
                      if (response.statusCode() == 200) {
                        try {
                          List<BidDTO> bids =
                              objectMapper.readValue(
                                  response.body(), new TypeReference<List<BidDTO>>() {});
                          historyList.setAll(bids);
                        } catch (Exception e) {
                          e.printStackTrace();
                        }
                      }
                    });
              } catch (Exception e) {
                Platform.runLater(
                    () -> {
                      Alert alert = new Alert(Alert.AlertType.ERROR, ServerConnectionException.MESSAGE);
                      alert.show();
                    });
              }
            })
        .start();
  }
}
