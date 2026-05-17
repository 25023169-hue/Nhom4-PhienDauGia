package io.auctionsystem.server.service;

import io.auctionsystem.common.dto.AuthResponse;
import io.auctionsystem.common.dto.RegisterRequest;
import io.auctionsystem.common.enums.Role;
import io.auctionsystem.server.repository.UserRepository;
import io.auctionsystem.server.model.User;
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

        // CHỈ KIỂM TRA KÝ TỰ ĐẶC BIỆT VÀ KHOẢNG TRẮNG BẰNG REGEX
        // ^[a-zA-Z0-9]+$ : Bắt buộc chỉ gồm chữ cái (không dấu) và số.
        if (!username.matches("^[a-zA-Z0-9]+$")) {
            throw new IllegalArgumentException("Tên đăng nhập chỉ được chứa chữ cái và số, không có khoảng trắng hoặc ký tự đặc biệt!");
        }

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại!");
        }

        User newUser = (request.getRole() == Role.SELLER) ? new Seller() : new Bidder();
        newUser.setUsername(username);
        newUser.setPassword(request.getPassword());
        newUser.setFirstname(request.getFirstname());
        newUser.setLastname(request.getLastname());

        userRepository.save(newUser);
        return "Đăng ký thành công";
    }

    public AuthResponse login(String username, String password) {
        // Tìm User, nếu không có thì trả về null luôn cho gọn
        User user = userRepository.findByUsername(username).orElse(null);

        // Dùng toán tử OR (||) để kiểm tra: Nếu User không tồn tại HOẶC sai mật khẩu
        if (user == null || !user.getPassword().equals(password) || !username.matches("^[a-zA-Z0-9]+$")) {
            throw new IllegalArgumentException("Tài khoản hoặc mật khẩu không chính xác!");
        }

        AuthResponse response = new AuthResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setFirstname(user.getFirstname());
        response.setLastname(user.getLastname());
        response.setBalance(user.getBalance());
        response.setBankName(user.getBankName());
        response.setBankAccount(user.getBankAccount());

        if (userRepository.isUserSeller(user.getId()) > 0) {
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
