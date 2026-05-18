package io.auctionsystem.client.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.auctionsystem.client.model.AddressRow;
import io.auctionsystem.client.pattern.AuctionManager;
import io.auctionsystem.client.pattern.SceneManager;
import io.auctionsystem.common.request.AddressRequest;
import io.auctionsystem.common.response.AuthResponse;
import io.auctionsystem.common.request.BankRequest;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

public class SettingsController {

    @FXML private VBox paneProfile, paneBank, paneAddress, panePassword;
    @FXML private Button btnProfile, btnBank, btnAddress, btnPassword;

    @FXML private TextField txtUsername, txtFirstName, txtLastName, txtAccountName, txtBankAccount, txtRecovery;

    @FXML private ComboBox<String> cbBankName;

    @FXML private TextField txtReceiverName, txtPhoneNumber, txtStreet, txtCity;
    @FXML private CheckBox chkIsDefault;
    @FXML private TableView<AddressRow> tableAddress;
    @FXML private TableColumn<AddressRow, String> colReceiverName, colPhoneNumber, colAddressDetails, colIsDefault;
    @FXML private PasswordField txtOldPass, txtNewPass;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObservableList<AddressRow> addressRows = FXCollections.observableArrayList();
    private long nextAddressId = 1;
    private String savedBankName = "";
    private FilteredList<String> filteredBanks;
    private boolean updatingBankEditor = false;

    // Khai báo danh sách ngân hàng làm biến toàn cục để dùng cho Autocomplete
    private final ObservableList<String> BANK_LIST = FXCollections.observableArrayList(
            "Vietcombank (VCB)", "MB Bank (MB)", "Techcombank (TCB)", "Agribank",
            "BIDV", "TP Bank", "ACB", "VP Bank", "Sacombank", "VietinBank"
    );

    @FXML
    public void initialize() {
        // --- THIẾT LẬP AUTOCOMPLETE CHO COMBOBOX ---
        filteredBanks = new FilteredList<>(BANK_LIST, bank -> true);
        cbBankName.setItems(filteredBanks);

        cbBankName.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
            if (updatingBankEditor) return;

            TextField editor = cbBankName.getEditor();
            String keyword = newValue == null ? "" : newValue;

            updateBankFilter(keyword);

            Platform.runLater(() -> {
                if (keyword.equals(editor.getText())) {
                    editor.positionCaret(editor.getText().length());
                }
            });

            if (editor.isFocused()) {
                if (filteredBanks.isEmpty()) {
                    cbBankName.hide();
                } else if (!cbBankName.isShowing()) {
                    cbBankName.show();
                }
            }
        });

        cbBankName.valueProperty().addListener((observable, oldValue, selectedBank) -> {
            if (!updatingBankEditor && selectedBank != null) {
                showBankName(selectedBank);
            }
        });

        // --- ĐỔ DỮ LIỆU USER CŨ LÊN GIAO DIỆN ---
        AuthResponse user = AuctionManager.getInstance().getCurrentUser();
        if (user != null) {
            txtUsername.setText(user.getUsername() != null ? user.getUsername() : "");
            txtLastName.setText(user.getLastname() != null ? user.getLastname() : "");
            txtFirstName.setText(user.getFirstname() != null ? user.getFirstname() : "");

            // Hiển thị ngân hàng cũ lên ComboBox
            if (user.getBankName() != null && !user.getBankName().isEmpty()) {
                savedBankName = user.getBankName();
                showBankName(savedBankName);
            }
            txtAccountName.setText(user.getAccountName() != null ? user.getAccountName() : "");
            txtBankAccount.setText(user.getBankAccount() != null ? user.getBankAccount() : "");
            if (user.getAddress() != null && !user.getAddress().isEmpty()) {
                addressRows.add(new AddressRow(nextAddressId++, "", "", user.getAddress(), false));
            }
        }

        setupAddressTable();

        String requestedTab = AuctionManager.getInstance().consumeSettingsTabRequest();
        if ("BANK".equals(requestedTab)) {
            showBank();
        } else {
            showProfile();
        }
    }

    // --- HÀM CHUYỂN TAB ---
    public void showProfile() { switchTab(paneProfile, btnProfile); }
    public void showBank() {
        switchTab(paneBank, btnBank);
        if (!savedBankName.isBlank() && cbBankName.getEditor().getText().isBlank()) {
            showBankName(savedBankName);
        }
    }
    public void showAddress() { switchTab(paneAddress, btnAddress); }
    public void showPassword() { switchTab(panePassword, btnPassword); }

    private void switchTab(VBox activePane, Button activeBtn) {
        paneProfile.setVisible(false);
        paneBank.setVisible(false);
        paneAddress.setVisible(false);
        panePassword.setVisible(false);
        activePane.setVisible(true);

        String normalStyle = "-fx-background-color: transparent; -fx-text-fill: black; -fx-font-weight: normal;";
        btnProfile.setStyle(normalStyle);
        btnBank.setStyle(normalStyle);
        btnAddress.setStyle(normalStyle);
        btnPassword.setStyle(normalStyle);

        activeBtn.setStyle("-fx-background-color: #e9ecef; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
    }

    // --- HÀM XỬ LÝ LƯU ---
    public void saveProfile() {
        System.out.println("Lưu hồ sơ: " + txtLastName.getText() + " " + txtFirstName.getText());
    }

    public void saveBank() {
        // Lấy dữ liệu từ ComboBox Editable
        final String bank = cbBankName.getEditor().getText().trim();
        final String accountname = txtAccountName.getText().trim();
        final String bankaccount = txtBankAccount.getText().trim();

        if (bank.isEmpty() || accountname.isEmpty() || bankaccount.isEmpty()) {
            showInfo("Vui lòng nhập đủ thông tin ngân hàng.");
            return;
        }
        if (!bankaccount.matches("\\d+")) {
            showInfo("Số tài khoản chỉ được chứa chữ số.");
            return;
        }

        AuthResponse user = AuctionManager.getInstance().getCurrentUser();
        if (user == null) return;

        Long userId = user.getUserId();
        BankRequest requestDto = new BankRequest(bank, accountname, bankaccount);

        new Thread(() -> {
            try {
                String jsonBody = objectMapper.writeValueAsString(requestDto);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/user/" + userId + "/bank"))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        user.setBankName(bank);
                        user.setAccountName(accountname);
                        user.setBankAccount(bankaccount);
                        savedBankName = bank;
                        showBankName(savedBankName);

                        System.out.println("Lưu Bank: " + bank + " | Chủ thẻ: " + accountname.toUpperCase() + " | STK: " + bankaccount);
                        System.out.println(AuctionManager.getInstance().hasBankInfo());
                        showInfo("Đã lưu thông tin ngân hàng thành công vào hệ thống.");
                        showBankName(savedBankName);
                    } else {
                        showInfo("Lỗi cập nhật từ Server: " + response.body());
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showInfo("Không thể kết nối đến server Spring Boot."));
            }
        }).start();
    }

    private void setupAddressTable() {
        colReceiverName.setCellValueFactory(new PropertyValueFactory<>("receiverName"));
        colPhoneNumber.setCellValueFactory(new PropertyValueFactory<>("phoneNumber"));
        colAddressDetails.setCellValueFactory(new PropertyValueFactory<>("addressDetails"));
        colIsDefault.setCellValueFactory(new PropertyValueFactory<>("defaultText"));
        tableAddress.setItems(addressRows);
        tableAddress.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            if (selected != null) {
                txtReceiverName.setText(selected.getReceiverName());
                txtPhoneNumber.setText(selected.getPhoneNumber());
                txtStreet.setText(selected.getStreet());
                txtCity.setText(selected.getCity());
                chkIsDefault.setSelected(selected.isDefaultAddress());
            }
        });
    }

    private void showBankName(String bankName) {
        updatingBankEditor = true;
        cbBankName.hide();
        cbBankName.setValue(null);
        cbBankName.getSelectionModel().clearSelection();
        cbBankName.getEditor().setText(bankName);
        updateBankFilter(bankName);
        cbBankName.getEditor().positionCaret(bankName.length());
        updatingBankEditor = false;

        Platform.runLater(() -> {
            updatingBankEditor = true;
            cbBankName.hide();
            cbBankName.setValue(null);
            cbBankName.getSelectionModel().clearSelection();
            cbBankName.getEditor().setText(bankName);
            updateBankFilter(bankName);
            cbBankName.getEditor().positionCaret(bankName.length());
            cbBankName.hide();
            updatingBankEditor = false;
        });
    }

    private void updateBankFilter(String keyword) {
        if (filteredBanks == null) return;

        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        filteredBanks.setPredicate(bank ->
                normalizedKeyword.isBlank() || bank.toLowerCase().contains(normalizedKeyword));
    }

    @FXML
    public void onAddAddress() {
        String receiverName = txtReceiverName.getText().trim();
        String phoneNumber = txtPhoneNumber.getText().trim();
        String street = txtStreet.getText().trim();
        String city = txtCity.getText().trim();
        boolean isDefault = chkIsDefault.isSelected();

        if (receiverName.isEmpty() || phoneNumber.isEmpty() || street.isEmpty() || city.isEmpty()) {
            showInfo("Vui lòng nhập đủ thông tin địa chỉ.");
            return;
        }

        if (isDefault) {
            clearDefaultAddress();
        }

        AddressRow row = new AddressRow(nextAddressId++, receiverName, phoneNumber, street, city, isDefault);
        addressRows.add(row);
        persistPrimaryAddress(row);
        clearAddressForm();
        showInfo("Đã thêm địa chỉ thành công!");
    }

    @FXML
    public void onUpdateAddress() {
        AddressRow selected = tableAddress.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Vui lòng chọn địa chỉ cần sửa.");
            return;
        }

        String receiverName = txtReceiverName.getText().trim();
        String phoneNumber = txtPhoneNumber.getText().trim();
        String street = txtStreet.getText().trim();
        String city = txtCity.getText().trim();
        boolean isDefault = chkIsDefault.isSelected();

        if (receiverName.isEmpty() || phoneNumber.isEmpty() || street.isEmpty() || city.isEmpty()) {
            showInfo("Vui lòng nhập đủ thông tin địa chỉ.");
            return;
        }

        if (isDefault) {
            clearDefaultAddress();
        }

        selected.setReceiverName(receiverName);
        selected.setPhoneNumber(phoneNumber);
        selected.setStreet(street);
        selected.setCity(city);
        selected.setDefaultAddress(isDefault);
        tableAddress.refresh();
        persistPrimaryAddress(selected);
        clearAddressForm();
        showInfo("Đã cập nhật địa chỉ thành công!");
    }

    @FXML
    public void onDeleteAddress() {
        AddressRow selected = tableAddress.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Vui lòng chọn địa chỉ cần xóa.");
            return;
        }

        addressRows.remove(selected);
        clearAddressForm();
        showInfo("Đã xóa địa chỉ.");
    }

    public void saveAddress() {
        onAddAddress();
    }

    private void persistPrimaryAddress(AddressRow row) {
        AuthResponse user = AuctionManager.getInstance().getCurrentUser();
        if (user == null) return;

        Long userId = user.getUserId();
        String address = row.getAddressDetails();
        AddressRequest requestDto = new AddressRequest(address);

        new Thread(() -> {
            try {
                String jsonBody = objectMapper.writeValueAsString(requestDto);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/user/" + userId + "/address"))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        user.setAddress(address);
                        System.out.println("Địa chỉ mới: " + address);
                    } else {
                        showInfo("Lỗi cập nhật từ Server: " + response.body());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showInfo("Không thể kết nối đến server Spring Boot."));
            }
        }).start();
    }

    private void clearDefaultAddress() {
        for (AddressRow row : addressRows) {
            row.setDefaultAddress(false);
        }
    }

    private void clearAddressForm() {
        txtReceiverName.clear();
        txtPhoneNumber.clear();
        txtStreet.clear();
        txtCity.clear();
        chkIsDefault.setSelected(false);
        tableAddress.getSelectionModel().clearSelection();
    }

    public void savePassword() {
        System.out.println("Đổi mật khẩu: Old=" + txtOldPass.getText() + " | New=" + txtNewPass.getText());
    }

    public void deleteAccount() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận xóa tài khoản");
        alert.setHeaderText("Cảnh báo: Hành động này không thể hoàn tác!");
        alert.setContentText("Bạn có chắc chắn muốn xóa vĩnh viễn tài khoản này không?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            System.out.println("Thực hiện logic xóa tài khoản tại đây...");
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setContentText("Tài khoản của bạn đã được xóa.");
            info.showAndWait();
            SceneManager.getInstance().switchScene("/client/fxml/login.fxml");
        }
    }

    public void onBack() {
        SceneManager.getInstance().switchScene("/client/fxml/dashboard.fxml");
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}