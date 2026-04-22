package io.auctionsystem.server.controller;

import io.auctionsystem.common.dto.AuthResponse;
import io.auctionsystem.common.dto.LoginRequest;
import io.auctionsystem.common.dto.RegisterRequest;
import io.auctionsystem.server.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        System.out.println("Nhận request đăng ký cho: " + request.getUsername()); // Log kiểm tra
        try {
            return ResponseEntity.ok(authService.register(request));
        } catch (Exception e) {
            System.err.println("--- LỖI TẠI SERVER ---");
            e.printStackTrace(); // Dòng này sẽ bắt lỗi phải hiện ra Console
            return ResponseEntity.status(500).body("Lỗi Server: " + e.getMessage());
        }
    }
}