package io.auctionsystem.server.service;

import io.auctionsystem.common.dto.RegisterRequest;
import io.auctionsystem.server.repogistory.UserDAO; // Import DAO của bạn
import io.auctionsystem.server.model.User;
import io.auctionsystem.server.model.Bidder;
import io.auctionsystem.server.model.Seller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// ... các import giữ nguyên ...

@Service
public class AuthService {

    @Autowired
    private UserDAO userDAO;

    // --- HÀM ĐĂNG KÝ (Giữ nguyên của bạn) ---
    public String register(RegisterRequest request) {
        if (userDAO.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại!");
        }
        User newUser;
        if ("SELLER".equalsIgnoreCase(request.getRole())) {
            newUser = new Seller();
        } else {
            newUser = new Bidder();
        }
        newUser.setUsername(request.getUsername());
        newUser.setPassword(request.getPassword());
        userDAO.save(newUser);
        return "Đăng ký thành công";
    }

    // --- HÀM ĐĂNG NHẬP (ĐÃ FIX LỖI OPTIONAL) ---
    public User login(String username, String password) {
        System.out.println(">>> [AuthService] Đang kiểm tra đăng nhập cho: " + username);

        // 1 & 2. Mở hộp Optional. Nếu hộp rỗng (không tìm thấy user), ném ngay lỗi!
        User user = userDAO.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản không tồn tại!"));

        // 3. Kiểm tra mật khẩu
        if (!user.getPassword().equals(password)) {
            throw new IllegalArgumentException("Mật khẩu không chính xác!");
        }

        System.out.println(">>> [AuthService] Đăng nhập thành công!");
        return user;
    }
}