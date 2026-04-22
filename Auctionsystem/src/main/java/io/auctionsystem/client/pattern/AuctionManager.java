package io.auctionsystem.client.pattern;

import io.auctionsystem.common.dto.AuthResponse;
import io.auctionsystem.common.enums.Role;

public class AuctionManager {

    // 1. Biến static lưu trữ instance duy nhất của class
    private static AuctionManager instance;

    // 2. Các thuộc tính lưu trữ phiên đăng nhập (Session)
    private String token;
    private Long userId;
    private String username;
    private Role role;

    // 3. Private constructor: Ngăn không cho các class khác dùng từ khóa 'new'
    private AuctionManager() {
    }

    // 4. Hàm global để lấy instance duy nhất ra sử dụng
    public static AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    // --- CÁC HÀM XỬ LÝ LOGIC ---

    // Gọi hàm này sau khi Server trả về đăng nhập thành công
    public void setCurrentUser(AuthResponse response) {
        this.token = response.getToken();
        this.userId = response.getUserId();
        this.username = response.getUsername();
        this.role = response.getRole();
    }

    // Gọi hàm này khi người dùng bấm nút Đăng xuất
    public void logout() {
        this.token = null;
        this.userId = null;
        this.username = null;
        this.role = null;
    }

    // --- CÁC HÀM GETTER ĐỂ LẤY THÔNG TIN (Không có Setter vì chỉ set qua hàm setCurrentUser) ---

    public String getToken() {
        return token;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public Role getRole() {
        return role;
    }

    // Hàm tiện ích kiểm tra xem đã đăng nhập chưa
    public boolean isLoggedIn() {
        return this.userId != null;
    }
}