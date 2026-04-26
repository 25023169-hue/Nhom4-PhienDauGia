package io.auctionsystem.client.pattern;

import io.auctionsystem.common.dto.AuthResponse;
import io.auctionsystem.common.enums.Role;

public class AuctionManager {

    private static AuctionManager instance;
    private AuthResponse currentUser;

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
    public void logout() { this.currentUser = null; }

    public String getToken() { return (isLoggedIn()) ? currentUser.getToken() : null; }
    public Long getUserId() { return (isLoggedIn()) ? currentUser.getUserId() : null; }
    public String getUsername() { return (isLoggedIn()) ? currentUser.getUsername() : "Guest"; }
    public String getFirstname() { return (isLoggedIn()) ? currentUser.getFirstname() : null; }
    public String getLastname() { return (isLoggedIn()) ? currentUser.getLastname() : null; }
    public Role getRole() { return (isLoggedIn()) ? currentUser.getRole() : null; }
    public boolean isAdmin() { return isLoggedIn() && currentUser.getRole() == Role.ADMIN; }
}