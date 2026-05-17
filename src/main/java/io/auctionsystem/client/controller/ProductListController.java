package io.auctionsystem.client.controller;

import io.auctionsystem.common.dto.AuctionItemDTO;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class ProductListController {

    @FXML private TableView<AuctionItemDTO> tableItems;
    @FXML private TableColumn<AuctionItemDTO, Long> colId;
    @FXML private TableColumn<AuctionItemDTO, String> colName;
    @FXML private TableColumn<AuctionItemDTO, Double> colCurrentPrice;
    @FXML private TableColumn<AuctionItemDTO, String> colEndTime;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colEndTime.setCellValueFactory(new PropertyValueFactory<>("endTime"));
    }
}