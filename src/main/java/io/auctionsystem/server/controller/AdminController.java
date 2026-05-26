package io.auctionsystem.server.controller;

import io.auctionsystem.common.enums.Role;
import io.auctionsystem.common.response.AuthResponse;
import io.auctionsystem.server.model.Admin;
import io.auctionsystem.server.model.User;
import io.auctionsystem.server.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Year;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<List<AuthResponse>> getAllUsers() {
        List<AuthResponse> result = adminService.findAllUsers().stream()
                .map(this::toAuthResponse)
                .toList();

        return ResponseEntity.ok(result);
    }

    @PutMapping("/users/toggle-ban/{id}")
    public ResponseEntity<String> toggleBan(@PathVariable Long id) {
        boolean success = adminService.toggleBanUser(id);

        if (success) {
            return ResponseEntity.ok("Cap nhat trang thai thanh cong!");
        }

        return ResponseEntity.badRequest().body("Khong tim thay nguoi dung.");
    }

    @GetMapping("/auctions/monthly-stats")
    public ResponseEntity<Map<Integer, Long>> getMonthlyAuctionStats(
            @RequestParam(required = false) Integer year) {
        int targetYear = year == null ? Year.now().getValue() : year;
        return ResponseEntity.ok(adminService.getMonthlyAuctionStats(targetYear));
    }

    private AuthResponse toAuthResponse(User user) {
        AuthResponse dto = new AuthResponse();
        dto.setUserId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setFirstname(user.getFirstname());
        dto.setLastname(user.getLastname());
        dto.setBalance(user.getBalance());

        if (user instanceof Admin) {
            dto.setRole(Role.ADMIN);
        } else if (adminService.isSeller(user.getId())) {
            dto.setRole(Role.SELLER);
        } else {
            dto.setRole(Role.BIDDER);
        }

        return dto;
    }
}
