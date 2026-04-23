package io.auctionsystem.client.pattern;

import io.auctionsystem.common.dto.AuthResponse;
import io.auctionsystem.common.enums.Role;

/**
 * Class quản lý Session (Phiên đăng nhập) duy nhất trong toàn bộ Client.
 * Sử dụng mô hình Singleton để đảm bảo dữ liệu người dùng đồng nhất ở mọi màn hình.
 */
public class AuctionManager {

    private static AuctionManager instance;

    // Lưu trữ nguyên đối tượng Response từ Server để không bỏ sót thông tin nào
    private AuthResponse currentUser;

    // Private constructor để thực hiện đúng chuẩn Singleton
    private AuctionManager() {
    }

    public static AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    // ================= CÁC HÀM XỬ LÝ LOGIC CHÍNH =================

    /**
     * Lưu thông tin người dùng sau khi đăng nhập thành công.
     */
    public void setCurrentUser(AuthResponse response) {
        this.currentUser = response;
    }

    /**
     * Lấy toàn bộ đối tượng người dùng hiện tại.
     */
    public AuthResponse getCurrentUser() {
        return currentUser;
    }

    /**
     * Kiểm tra xem đã có người dùng nào đăng nhập hay chưa.
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Đăng xuất: Xóa sạch dữ liệu trong phiên làm việc.
     */
    public void logout() {
        this.currentUser = null;
    }

    // ================= CÁC HÀM GETTER TIỆN ÍCH (HELPER) =================
    // Giúp lấy nhanh các thuộc tính mà không cần gọi thông qua getCurrentUser()
    // Đã bao gồm kiểm tra Null để tránh làm App bị văng (Crash).

    public String getToken() {
        return (isLoggedIn()) ? currentUser.getToken() : null;
    }

    public Long getUserId() {
        return (isLoggedIn()) ? currentUser.getUserId() : null;
    }

    public String getUsername() {
        return (isLoggedIn()) ? currentUser.getUsername() : "Guest";
    }

    public Role getRole() {
        return (isLoggedIn()) ? currentUser.getRole() : null;
    }

    /**
     * Kiểm tra xem người dùng hiện tại có phải là Admin hay không.
     */
    public boolean isAdmin() {
        return isLoggedIn() && currentUser.getRole() == Role.ADMIN;
    }
}