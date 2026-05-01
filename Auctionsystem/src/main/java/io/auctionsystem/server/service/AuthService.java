package io.auctionsystem.server.service;

import io.auctionsystem.common.dto.AuthResponse;
import io.auctionsystem.common.dto.RegisterRequest;
import io.auctionsystem.common.enums.Role;
import io.auctionsystem.server.repogistory.UserRepogistory;
import io.auctionsystem.server.model.User;
import io.auctionsystem.server.model.Bidder;
import io.auctionsystem.server.model.Seller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserRepogistory userRepogistory;

    public String register(RegisterRequest request) {
        String username = request.getUsername();

        // CHỈ KIỂM TRA KÝ TỰ ĐẶC BIỆT VÀ KHOẢNG TRẮNG BẰNG REGEX
        // ^[a-zA-Z0-9]+$ : Bắt buộc chỉ gồm chữ cái (không dấu) và số.
        if (!username.matches("^[a-zA-Z0-9]+$")) {
            throw new IllegalArgumentException("Tên đăng nhập chỉ được chứa chữ cái và số, không có khoảng trắng hoặc ký tự đặc biệt!");
        }

        if (userRepogistory.existsByUsername(username)) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại!");
        }

        User newUser = ("SELLER".equalsIgnoreCase(request.getRole())) ? new Seller() : new Bidder();
        newUser.setUsername(username);
        newUser.setPassword(request.getPassword());
        newUser.setFirstname(request.getFirstname());
        newUser.setLastname(request.getLastname());

        userRepogistory.save(newUser);
        return "Đăng ký thành công";
    }

    public AuthResponse login(String username, String password) {
        // Tìm User, nếu không có thì trả về null luôn cho gọn
        User user = userRepogistory.findByUsername(username).orElse(null);

        // Dùng toán tử OR (||) để kiểm tra: Nếu User không tồn tại HOẶC sai mật khẩu
        if (user == null || !user.getPassword().equals(password)) {
            throw new IllegalArgumentException("Tài khoản hoặc mật khẩu không chính xác!");
        }

        AuthResponse response = new AuthResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setFirstname(user.getFirstname());
        response.setLastname(user.getLastname());

        if (userRepogistory.isUserSeller(user.getId()) > 0) {
            response.setRole(Role.SELLER);
        } else {
            response.setRole(Role.BIDDER);
        }

        return response;
    }

    public String upgradeToSeller(Long id, String storeName) {
        if (userRepogistory.isUserSeller(id) > 0) {
            throw new RuntimeException("Tài khoản này đã đăng ký Kênh Người Bán rồi!");
        }

        try {
            userRepogistory.upgradeToSellerNative(id, storeName);
            return "Đăng ký Kênh Người Bán thành công!";
        } catch (Exception e) {
            throw new RuntimeException("Lỗi hệ thống khi nâng cấp: " + e.getMessage());
        }
    }
}