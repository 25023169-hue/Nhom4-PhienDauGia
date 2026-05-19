package io.auctionsystem.client.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.auctionsystem.client.pattern.AuctionManager;
import io.auctionsystem.common.request.AddressRequest;
import io.auctionsystem.common.response.AuthResponse;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AddressSettingController {
    @FXML private TextArea txtAddress;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @FXML
    public void initialize() {
        AuthResponse user = AuctionManager.getInstance().getCurrentUser();
        if (user != null && user.getAddress() != null) {
            txtAddress.setText(user.getAddress());
        }
    }

    @FXML
    public void onUpdateAddress() {
        String address = txtAddress.getText().trim();
        if (address.isEmpty()) {
            showAlert("Vui lòng nhập chuỗi địa chỉ!");
            return;
        }
        persistAddress(address);
    }

    @FXML
    public void onDeleteAddress() {
        if(txtAddress.getText().trim().isEmpty()) return;
        txtAddress.clear();
        persistAddress("");
    }

    private void persistAddress(String address) {
        AuthResponse user = AuctionManager.getInstance().getCurrentUser();
        if (user == null) return;

        new Thread(() -> {
            try {
                AddressRequest requestDto = new AddressRequest(address);
                String jsonBody = objectMapper.writeValueAsString(requestDto);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/user/" + user.getUserId() + "/address"))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        user.setAddress(address);
                        showAlert("Thao tác xử lý địa chỉ thành công.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Lỗi kết nối Server."));
            }
        }).start();
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}