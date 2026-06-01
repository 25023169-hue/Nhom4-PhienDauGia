package client.controller;

import client.ServerConnectionException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import client.pattern.ClientHttp;
import client.pattern.SceneManager;
import common.Constants;
import common.dto.AuctionItemDTO;
import common.enums.AuctionState;
import common.response.AuthResponse;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;

public class AdminDashboardController {
  @FXML private Pane dashboardPane;
  @FXML private Pane usersPane;
  @FXML private Pane auctionsPane;
  @FXML private Label statUsers;
  @FXML private Label statOpenAuctions;
  @FXML private Label statActiveAuctions;
  @FXML private Label statFinishedAuctions;
  @FXML private TableView<AuthResponse> usersTable;
  @FXML private TableColumn<AuthResponse, Long> colUserId;
  @FXML private TableColumn<AuthResponse, String> colUsername;
  @FXML private TableColumn<AuthResponse, String> colFullname;
  @FXML private TableColumn<AuthResponse, String> colRole;
  @FXML private TableView<AuctionItemDTO> auctionsTable;
  @FXML private TableColumn<AuctionItemDTO, Long> colAuctionId;
  @FXML private TableColumn<AuctionItemDTO, String> colItem;
  @FXML private TableColumn<AuctionItemDTO, AuctionState> colStatus;
  @FXML private TableColumn<AuctionItemDTO, String> colStartTime;
  @FXML private TableColumn<AuctionItemDTO, String> colEndTime;
  @FXML private TableColumn<AuctionItemDTO, Void> colAuctionAction;

  private final ObjectMapper objectMapper = ClientHttp.mapper();

  @FXML
  public void initialize() {
    setupUsersTable();
    setupAuctionsTable();
    showPane(dashboardPane);
    refreshUsers();
    refreshAuctions();
  }

  private void setupUsersTable() {
    colUserId.setCellValueFactory(new PropertyValueFactory<>("userId"));
    colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
    colFullname.setCellValueFactory(
        cellData -> {
          AuthResponse user = cellData.getValue();
          return new SimpleStringProperty(joinName(user.getLastname(), user.getFirstname()));
        });
    colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
  }

  private void setupAuctionsTable() {
    colAuctionId.setCellValueFactory(new PropertyValueFactory<>("id"));
    colItem.setCellValueFactory(new PropertyValueFactory<>("name"));
    colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
    colStatus.setCellFactory(
        column ->
            new TableCell<>() {
              @Override
              protected void updateItem(AuctionState status, boolean empty) {
                super.updateItem(status, empty);
                setText(empty ? null : displayStatus(status));
              }
            });
    colStartTime.setCellValueFactory(new PropertyValueFactory<>("startTime"));
    colEndTime.setCellValueFactory(new PropertyValueFactory<>("endTime"));
    colAuctionAction.setCellFactory(
        column ->
            new TableCell<>() {
              private final Button deleteButton = new Button("Xóa");

              {
                deleteButton.setStyle(
                    "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand;");
              }

              @Override
              protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                  setGraphic(null);
                  return;
                }

                AuctionItemDTO auction = getTableView().getItems().get(getIndex());
                if (!canDelete(auction)) {
                  setGraphic(null);
                  return;
                }

                deleteButton.setOnAction(event -> confirmDeleteAuction(auction));
                setGraphic(deleteButton);
              }
            });
  }

  private void refreshUsers() {
    new Thread(
            () -> {
              try {
                HttpRequest request =
                    HttpRequest.newBuilder()
                        .uri(URI.create(Constants.BASE_URL + "/admin/users"))
                        .GET()
                        .build();
                HttpResponse<String> response =
                    ClientHttp.send(request);

                if (response.statusCode() == 200) {
                  List<AuthResponse> users =
                      objectMapper.readValue(
                          response.body(), new TypeReference<List<AuthResponse>>() {});
                  Platform.runLater(
                      () -> {
                        usersTable.setItems(FXCollections.observableArrayList(users));
                        statUsers.setText(String.valueOf(users.size()));
                      });
                }
              } catch (Exception e) {
                System.err.println("Lỗi load bảng Admin: " + e.getMessage());
              }
            })
        .start();
  }

  private void refreshAuctions() {
    new Thread(
            () -> {
              try {
                HttpRequest request =
                    HttpRequest.newBuilder()
                        .uri(URI.create(Constants.BASE_URL + "/admin/auctions"))
                        .GET()
                        .build();
                HttpResponse<String> response =
                    ClientHttp.send(request);

                if (response.statusCode() == 200) {
                  List<AuctionItemDTO> auctions =
                      objectMapper.readValue(
                          response.body(), new TypeReference<List<AuctionItemDTO>>() {});
                  Platform.runLater(
                      () -> {
                        auctionsTable.setItems(FXCollections.observableArrayList(auctions));
                        statOpenAuctions.setText(
                            String.valueOf(countStatus(auctions, AuctionState.OPEN)));
                        statActiveAuctions.setText(
                            String.valueOf(countStatus(auctions, AuctionState.RUNNING)));
                        statFinishedAuctions.setText(
                            String.valueOf(countStatus(auctions, AuctionState.FINISHED)));
                      });
                }
              } catch (Exception e) {
                System.err.println("Lỗi load bảng phiên đấu giá Admin: " + e.getMessage());
              }
            })
        .start();
  }

  private void confirmDeleteAuction(AuctionItemDTO auction) {
    Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
    confirmation.setHeaderText("Xóa phiên đấu giá #" + auction.getId());
    confirmation.setContentText(
        "Phiên sẽ chuyển sang CANCELLED. Nếu đang chạy, tiền giữ chỗ sẽ được hoàn lại.");
    if (confirmation.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
      deleteAuction(auction.getId());
    }
  }

  private void deleteAuction(Long auctionId) {
    new Thread(
            () -> {
              try {
                HttpRequest request =
                    HttpRequest.newBuilder()
                        .uri(URI.create(Constants.BASE_URL + "/admin/auctions/" + auctionId))
                        .DELETE()
                        .build();
                HttpResponse<String> response =
                    ClientHttp.send(request);

                Platform.runLater(
                    () -> {
                      if (response.statusCode() == 200) {
                        refreshAuctions();
                      } else {
                        showAlert(Alert.AlertType.ERROR, "Không thể xóa phiên", response.body());
                      }
                    });
              } catch (Exception e) {
                Platform.runLater(
                    () ->
                        showAlert(
                            Alert.AlertType.ERROR,
                            "Không thể xóa phiên",
                            ServerConnectionException.MESSAGE));
              }
            })
        .start();
  }

  @FXML
  public void onLogoutClicked() {
    SceneManager.getInstance().switchScene("/client/user/login.fxml");
  }

  @FXML
  public void onDashboardClicked() {
    showPane(dashboardPane);
  }

  @FXML
  public void onManageUsersClicked() {
    showPane(usersPane);
  }

  @FXML
  public void onApproveAuctionsClicked() {
    showPane(auctionsPane);
    refreshAuctions();
  }

  @FXML
  public void onSettingsClicked() {
    showAlert(
        Alert.AlertType.INFORMATION, "Cài đặt", "Cài đặt dành cho Admin chưa được triển khai.");
  }

  private void showPane(Pane selectedPane) {
    for (Pane pane : new Pane[] {dashboardPane, usersPane, auctionsPane}) {
      boolean selected = pane == selectedPane;
      pane.setVisible(selected);
      pane.setManaged(selected);
    }
  }

  private boolean canDelete(AuctionItemDTO auction) {
    return auction.getStatus() == AuctionState.OPEN || auction.getStatus() == AuctionState.RUNNING;
  }

  private long countStatus(List<AuctionItemDTO> auctions, AuctionState status) {
    return auctions.stream().filter(auction -> status == auction.getStatus()).count();
  }

  private String displayStatus(AuctionState status) {
    return status == null ? "" : status.name();
  }

  private void showAlert(Alert.AlertType type, String title, String message) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }

  private String joinName(String lastname, String firstname) {
    String fullName =
        ((lastname == null ? "" : lastname) + " " + (firstname == null ? "" : firstname)).trim();
    return fullName.isEmpty() ? "-" : fullName;
  }
}
