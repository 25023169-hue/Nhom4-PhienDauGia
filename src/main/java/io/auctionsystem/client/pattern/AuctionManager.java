package io.auctionsystem.client.pattern;

import io.auctionsystem.common.dto.AuthResponse;
import io.auctionsystem.common.enums.Role;

public class AuctionManager {

    private static AuctionManager instance;
    private AuthResponse currentUser;
    private String requestedSettingsTab;

    private AuctionManager() {}

    public static AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    public void setCurrentUser(AuthResponse response) { this.currentUser = response; }
    public AuthResponse getCurrentUser() { return currentUser; }
    public boolean isLoggedIn() { return currentUser != null; }
    public void isLoggedOut() { this.currentUser = null; }

    public String getToken() { return (isLoggedIn()) ? currentUser.getToken() : null; }
    public Long getId() { return (isLoggedIn()) ? currentUser.getId() : null; }
    public String getUsername() { return (isLoggedIn()) ? currentUser.getUsername() : "Guest"; }
    public String getFirstname() { return (isLoggedIn()) ? currentUser.getFirstname() : null; }
    public String getLastname() { return (isLoggedIn()) ? currentUser.getLastname() : null; }
    public double getBalance() { return (isLoggedIn()) ? currentUser.getBalance() : 0.0; }
    public void setBalance(double balance) { if (isLoggedIn()) currentUser.setBalance(balance); }
    public String getBankName() { return (isLoggedIn()) ? currentUser.getBankName() : null; }
    public String getBankAccount() { return (isLoggedIn()) ? currentUser.getBankAccount() : null; }
    public Role getRole() { return (isLoggedIn()) ? currentUser.getRole() : null; }
    public boolean isAdmin() { return isLoggedIn() && currentUser.getRole() == Role.ADMIN; }
    public boolean hasBankInfo() {
        return isLoggedIn()
                && currentUser.getBankName() != null
                && !currentUser.getBankName().trim().isEmpty()
                && currentUser.getBankAccount() != null
                && currentUser.getBankAccount().trim().isEmpty();
    }
    public void requestSettingsTab(String tabName) { this.requestedSettingsTab = tabName; }
    public String consumeSettingsTabRequest() {
        String tabName = requestedSettingsTab;
        requestedSettingsTab = null;
        return tabName;
    }
}
