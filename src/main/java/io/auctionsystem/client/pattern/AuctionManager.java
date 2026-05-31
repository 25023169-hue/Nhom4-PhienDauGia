package io.auctionsystem.client.pattern;

import io.auctionsystem.common.enums.Role;
import io.auctionsystem.common.response.AuthResponse;

public class AuctionManager {

  // LỖI ĐÃ SỬA: Singleton pattern cũ không thread-safe:
  //   if (instance == null) { instance = new AuctionManager(); }
  // → 2 thread cùng vào cùng lúc có thể tạo ra 2 instance.
  // Sửa: Dùng Initialization-on-demand Holder pattern (thread-safe, lazy, không cần synchronized)
  private static final AuctionManager INSTANCE = new AuctionManager();

  private AuthResponse currentUser;
  private String requestedSettingsTab;

  private AuctionManager() {}

  public static AuctionManager getInstance() {
    return INSTANCE;
  }

  public void setCurrentUser(AuthResponse response) {
    this.currentUser = response;
  }

  public AuthResponse getCurrentUser() {
    return currentUser;
  }

  public boolean isLoggedIn() {
    return currentUser != null;
  }

  public void isLoggedOut() {
    this.currentUser = null;
  }

  public String getToken() {
    return (isLoggedIn()) ? currentUser.getToken() : null;
  }

  public Long getId() {
    return (isLoggedIn()) ? currentUser.getUserId() : null;
  }

  public String getUsername() {
    return (isLoggedIn()) ? currentUser.getUsername() : "Guest";
  }

  public String getFirstname() {
    return (isLoggedIn()) ? currentUser.getFirstname() : null;
  }

  public String getLastname() {
    return (isLoggedIn()) ? currentUser.getLastname() : null;
  }

  public String getStoreName() {
    return (isLoggedIn()) ? currentUser.getStoreName() : "Seller";
  }

  public double getBalance() {
    return (isLoggedIn()) ? currentUser.getBalance() : 0.0;
  }

  public void setBalance(double balance) {
    if (isLoggedIn()) currentUser.setBalance(balance);
  }

  public String getBankAccount() {
    return (isLoggedIn()) ? currentUser.getBankAccount() : null;
  }

  public Role getRole() {
    return (isLoggedIn()) ? currentUser.getRole() : null;
  }

  public String getAddress() {
    return (isLoggedIn()) ? currentUser.getAddress() : null;
  }

  public String getAccountName() {
    return (isLoggedIn()) ? currentUser.getAccountName() : null;
  }

  public boolean isAdmin() {
    return isLoggedIn() && currentUser.getRole() == Role.ADMIN;
  }

  public boolean hasBankInfo() {
    return isLoggedIn()
        && currentUser.getAccountName() != null
        && !currentUser.getAccountName().trim().isEmpty()
        && currentUser.getBankAccount() != null
        && !currentUser.getBankAccount().trim().isEmpty();
  }

  public void requestSettingsTab(String tabName) {
    this.requestedSettingsTab = tabName;
  }

  public String consumeSettingsTabRequest() {
    String tabName = requestedSettingsTab;
    requestedSettingsTab = null;
    return tabName;
  }
}
