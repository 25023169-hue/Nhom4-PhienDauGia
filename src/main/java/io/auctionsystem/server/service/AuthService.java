package io.auctionsystem.server.service;

// LỖI ĐÃ SỬA: Thay 2 import sai package "dto" bằng đúng package "response" và "request"
// import io.auctionsystem.common.dto.AuthResponse;    ← ĐÃ XÓA (sai)
// import io.auctionsystem.common.dto.RegisterRequest; ← ĐÃ XÓA (sai)
import io.auctionsystem.common.response.AuthResponse;
import io.auctionsystem.common.request.RegisterRequest;
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

        // Chỉ kiểm tra ký tự đặc biệt và khoảng trắng bằng regex
        if (!username.matches("^[a-zA-Z0-9]+$")) {
            throw new IllegalArgumentException("Tên đăng nhập chỉ được chứa chữ cái và số, không có khoảng trắng hoặc ký tự đặc biệt!");
        }

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại!");
        }

        // LỖI ĐÃ SỬA: Không thể gọi .equalsIgnoreCase() trên kiểu enum Role.
        // request.getRole() trả về Role (enum), không phải String.
        // Phải so sánh enum đúng cách: Role.SELLER == request.getRole()
        User newUser = (Role.SELLER == request.getRole()) ? new Seller() : new Bidder();
        newUser.setUsername(username);
        newUser.setPassword(request.getPassword());
        newUser.setFirstname(request.getFirstname());
        newUser.setLastname(request.getLastname());

        userRepository.save(newUser);
        return "Đăng ký thành công";
    }

    public AuthResponse login(String username, String password) {
        // Tìm User, nếu không có thì ném lỗi
        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null || !user.isActive() || !user.getPassword().equals(password)) {
            throw new IllegalArgumentException("Tài khoản hoặc mật khẩu không chính xác!");
        }

        AuthResponse response = new AuthResponse();

        // LỖI ĐÃ SỬA: Phương thức đúng là setUserId(), không phải setId()
        // (Vì field trong AuthResponse là "userId", Lombok tạo ra setUserId())
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setFirstname(user.getFirstname());
        response.setLastname(user.getLastname());

        // LỖI ĐÃ SỬA: Thêm balance vào response (trước đây bị thiếu → client luôn thấy số dư = 0)
        response.setBalance(user.getAvailableBalance());

        // LỖI ĐÃ SỬA: Thêm thông tin bankName, accountName, bankAccount, address, storeName
        // Các trường này nằm ở Bidder/Seller, cần ép kiểu để lấy
        if (user instanceof Bidder bidder) {
            response.setAddress(bidder.getAddress());
            response.setBankName(bidder.getBankName());
            response.setAccountName(bidder.getAccountName());
            response.setBankAccount(bidder.getBankAccount());
        }
        if (user instanceof Seller seller) {
            response.setStoreName(seller.getStoreName());
        }

        // Xác định Role
        if (userRepository.isUserSeller(user.getId()) > 0) {
            response.setRole(Role.SELLER);
        } else {
            response.setRole(Role.BIDDER);
        }

        return response;
    }

    public String upgradeToSeller(Long id, String storeName) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));
        if (!user.isActive()) {
            throw new RuntimeException("Tài khoản đã bị vô hiệu hóa");
        }
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
