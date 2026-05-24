package io.auctionsystem.server.service;

import io.auctionsystem.common.dto.AuthResponse;
import io.auctionsystem.common.dto.RegisterRequest;
import io.auctionsystem.common.enums.Role;
import io.auctionsystem.server.repository.UserRepository;
import io.auctionsystem.server.model.User;
import io.auctionsystem.server.model.Admin;  // PHẢI IMPORT THÊM DÒNG NÀY
import io.auctionsystem.server.model.Bidder;
import io.auctionsystem.server.model.Seller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    public String register(RegisterRequest request) {
        String username = request.getUsername();

        if (!username.matches("^[a-zA-Z0-9]+$")) {
            throw new IllegalArgumentException("Tên đăng nhập chỉ được chứa chữ cái và số, không có khoảng trắng hoặc ký tự đặc biệt!");
        }

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại!");
        }

        User newUser = ("SELLER".equalsIgnoreCase(request.getRole())) ? new Seller() : new Bidder();
        newUser.setUsername(username);
        newUser.setPassword(request.getPassword());
        newUser.setFirstname(request.getFirstname());
        newUser.setLastname(request.getLastname());

        userRepository.save(newUser);
        return "Đăng ký thành công";
    }

    public AuthResponse login(String username, String password) {
        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null || !user.getPassword().equals(password)) {
            throw new IllegalArgumentException("Tài khoản hoặc mật khẩu không chính xác!");
        }

        AuthResponse response = new AuthResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setFirstname(user.getFirstname());
        response.setLastname(user.getLastname());

        // --- ĐOẠN NÀY ĐÃ ĐƯỢC CẬP NHẬT ĐỂ NHẬN DIỆN ADMIN ---
        if (user instanceof Admin) {
            // Kiểm tra xem đối tượng lấy lên có phải là Admin không
            response.setRole(Role.ADMIN);
            System.out.println(">>> Server xac nhan: Day la ADMIN");
        } else if (userRepository.isUserSeller(user.getId()) > 0) {
            response.setRole(Role.SELLER);
        } else {
            response.setRole(Role.BIDDER);
        }

        return response;
    }

    public String upgradeToSeller(Long id, String storeName) {
        if (userRepository.isUserSeller(id) > 0) {
            throw new RuntimeException("Tài khoản này đã đăng ký Kênh Người Bán rồi!");
        }

        try {
            userRepository.upgradeToSellerNative(id, storeName);
            return "Đăng ký Kênh Người Bán thành công!";
        } catch (Exception e) {
            throw new RuntimeException("Lỗi hệ thống khi nâng cấp: " + e.getMessage());
        }
    }
}