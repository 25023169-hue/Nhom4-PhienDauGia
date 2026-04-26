package io.auctionsystem.server.API;

import io.auctionsystem.common.dto.LoginRequest; // Thêm import LoginRequest
import io.auctionsystem.common.dto.RegisterRequest;
import io.auctionsystem.server.model.User; // Thêm import User
import io.auctionsystem.server.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthAPI {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        System.out.println(">>> BẮT ĐẦU XỬ LÝ ĐĂNG KÝ CHO TÀI KHOẢN: " + request.getUsername());

        try {
            String result = authService.register(request);
            System.out.println(">>> ĐĂNG KÝ THÀNH CÔNG: " + request.getUsername());
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            // Bắt lỗi logic (Ví dụ: trùng tên đăng nhập) -> Trả về mã 400
            System.err.println(">>> LỖI NHẬP LIỆU: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());

        } catch (Exception e) {
            // Bắt lỗi hệ thống (Database, NullPointer, Hibernate...) -> Trả về mã 500
            System.err.println("============= LỖI HỆ THỐNG NGHIÊM TRỌNG =============");
            e.printStackTrace(); // ÉP IN LỖI RA CONSOLE
            System.err.println("=====================================================");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi Server: " + e.getMessage());
        }
    }

    // ================= THÊM API ĐĂNG NHẬP =================
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        System.out.println(">>> BẮT ĐẦU XỬ LÝ ĐĂNG NHẬP CHO TÀI KHOẢN: " + request.getUsername());

        try {
            // Gọi hàm login đã sửa lỗi Optional bên AuthService
            User user = authService.login(request.getUsername(), request.getPassword());
            System.out.println(">>> ĐĂNG NHẬP THÀNH CÔNG: " + request.getUsername());

            // Trả về thông tin user cho Client (Giao diện sẽ nhận được HTTP 200 OK)
            return ResponseEntity.ok(user);

        } catch (IllegalArgumentException e) {
            // Bắt lỗi logic (Tài khoản không tồn tại / Sai mật khẩu) -> Trả về mã 400
            System.err.println(">>> LỖI ĐĂNG NHẬP: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());

        } catch (Exception e) {
            // Bắt lỗi hệ thống -> Trả về mã 500
            System.err.println("============= LỖI HỆ THỐNG KHI ĐĂNG NHẬP =============");
            e.printStackTrace();
            System.err.println("=====================================================");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi Server: " + e.getMessage());
        }
    }
}