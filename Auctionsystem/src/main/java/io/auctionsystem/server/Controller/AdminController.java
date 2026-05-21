package io.auctionsystem.server.Controller;

import io.auctionsystem.common.dto.AuthResponse;
import io.auctionsystem.server.model.User;
import io.auctionsystem.server.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    // =========================
    // 1. Lấy danh sách user (DTO SAFE)
    // =========================
    @GetMapping("/users")
    public ResponseEntity<List<AuthResponse>> getAllUsers() {

        List<User> users = adminService.findAllUsers();

        List<AuthResponse> result = users.stream().map(user -> {
            AuthResponse dto = new AuthResponse();

            dto.setId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setFirstname(user.getFirstname());
            dto.setLastname(user.getLastname());

            // map role an toàn
            if (user instanceof io.auctionsystem.server.model.Admin) {
                dto.setRole(io.auctionsystem.common.enums.Role.ADMIN);
            } else if (adminService.isSeller(user.getId())) {
                dto.setRole(io.auctionsystem.common.enums.Role.SELLER);
            } else {
                dto.setRole(io.auctionsystem.common.enums.Role.BIDDER);
            }

            // balance nếu có logic
            dto.setBalance(user.getBalance());
            dto.setBanned(user.isBanned());

            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // =========================
    // 2. Toggle ban user
    // =========================
    @PutMapping("/users/toggle-ban/{id}")
    public ResponseEntity<String> toggleBan(@PathVariable Long id) {

        boolean success = adminService.toggleBanUser(id);

        if (success) {
            return ResponseEntity.ok("Cập nhật trạng thái thành công!");
        }

        return ResponseEntity.badRequest().body("Không tìm thấy người dùng.");
    }
    // Thêm vào AdminController.java

    @GetMapping("/stats/monthly")
    public ResponseEntity<Map<Integer, Long>> getMonthlyStats(
            @RequestParam(defaultValue = "2026") int year) {
        return ResponseEntity.ok(adminService.getMonthlyAuctionStats(year));
    }
}