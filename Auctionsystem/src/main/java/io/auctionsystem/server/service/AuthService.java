package io.auctionsystem.server.service;

import io.auctionsystem.common.dto.AuthResponse;
import io.auctionsystem.common.dto.LoginRequest;
import io.auctionsystem.common.dto.RegisterRequest;
import io.auctionsystem.common.enums.Role;
import io.auctionsystem.server.dao.UserDAO;
import io.auctionsystem.server.model.Bidder;
import io.auctionsystem.server.model.Seller;
import io.auctionsystem.server.model.User;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserDAO userDAO;

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public AuthResponse login(LoginRequest request) {
        User user = userDAO.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản không tồn tại!"));

        if (!user.getPassword().equals(request.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu không chính xác!");
        }

        String roleName = user.getClass().getSimpleName().toUpperCase();
        String fakeToken = "dummy-jwt-token"; // Tạm thời để string

        return new AuthResponse(fakeToken, user.getId(), user.getUsername(), Role.valueOf(roleName));
    }

    public String register(RegisterRequest request) {
        // Kiểm tra xem user đã tồn tại chưa
        if (userDAO.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại!");
        }

        User newUser;
        // Phân loại khởi tạo dựa trên Role
        if ("SELLER".equalsIgnoreCase(request.getRole())) {
            newUser = new Seller();
        } else {
            newUser = new Bidder(); // Mặc định là người mua
        }

        newUser.setUsername(request.getUsername());
        newUser.setPassword(request.getPassword());

        userDAO.save(newUser);
        return "Đăng ký thành công!";
    }
}