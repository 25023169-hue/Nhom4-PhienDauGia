package io.auctionsystem.server.Controller;

import io.auctionsystem.common.dto.AuthResponse;
import io.auctionsystem.common.dto.LoginRequest;
import io.auctionsystem.common.dto.RegisterRequest;
import io.auctionsystem.server.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        System.out.println(">>> BẮT ĐẦU XỬ LÝ ĐĂNG KÝ CHO TÀI KHOẢN: " + request.getUsername());
        try {
            String result = authService.register(request);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi Server: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        System.out.println(">>> BẮT ĐẦU XỬ LÝ ĐĂNG NHẬP CHO TÀI KHOẢN: " + request.getUsername());
        try {
            AuthResponse authResponse = authService.login(request.getUsername(), request.getPassword());
            return ResponseEntity.ok(authResponse);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi Server: " + e.getMessage());
        }
    }

    @PostMapping("/upgrade-seller/{id}")
    public ResponseEntity<?> upgradeToSeller(@PathVariable Long id, @RequestParam String storeName) {
        try {
            String result = authService.upgradeToSeller(id, storeName);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
