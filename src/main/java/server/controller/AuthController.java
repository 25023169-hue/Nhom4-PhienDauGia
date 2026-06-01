package server.controller;

import common.request.LoginRequest;
import common.request.RegisterRequest;
import common.response.AuthResponse;
import server.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/register")
  public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
    String result = authService.register(request);
    return ResponseEntity.ok(result);
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody LoginRequest request) {
    AuthResponse authResponse = authService.login(request.getUsername(), request.getPassword());
    return ResponseEntity.ok(authResponse);
  }

  @PostMapping("/upgrade-seller/{id}")
  public ResponseEntity<?> upgradeToSeller(@PathVariable Long id, @RequestParam String storeName) {
    String result = authService.upgradeToSeller(id, storeName);
    return ResponseEntity.ok(result);
  }
}
