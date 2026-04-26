package io.auctionsystem.server.service;

import io.auctionsystem.common.dto.RegisterRequest;
import io.auctionsystem.server.repogistory.UserRepogistory;
import io.auctionsystem.server.model.User;
import io.auctionsystem.server.model.Bidder;
import io.auctionsystem.server.model.Seller;
import io.auctionsystem.server.repogistory.UserRepogistory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserRepogistory userRepogistory;
    @Autowired
    private UserRepogistory userRepo;

    public String register(RegisterRequest request) {
        if (userRepogistory.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại!");
        }

        User newUser = ("SELLER".equalsIgnoreCase(request.getRole())) ? new Seller() : new Bidder();

        newUser.setUsername(request.getUsername());
        newUser.setPassword(request.getPassword());
        newUser.setFirstname(request.getFirstname());
        newUser.setLastname(request.getLastname());

        userRepogistory.save(newUser);
        return "Đăng ký thành công";
    }

    public User login(String username, String password) {
        User user = userRepogistory.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản không tồn tại!"));

        if (!user.getPassword().equals(password)) {
            throw new IllegalArgumentException("Mật khẩu không chính xác!");
        }
        return user;
    }
}