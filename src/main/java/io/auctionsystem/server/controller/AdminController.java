package io.auctionsystem.server.controller;

import io.auctionsystem.common.dto.AuctionItemDTO;
import io.auctionsystem.common.response.ApiResponse;
import io.auctionsystem.common.response.AuthResponse;
import io.auctionsystem.server.model.User;
import io.auctionsystem.server.service.AdminService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

  @Autowired private AdminService adminService;

  // =========================
  // 1. Lấy danh sách user (DTO SAFE)
  // =========================
  @GetMapping("/users")
  public ResponseEntity<List<AuthResponse>> getAllUsers() {

    List<User> users = adminService.findAllUsers();

    List<AuthResponse> result =
        users.stream()
            .map(
                user -> {
                  AuthResponse dto = new AuthResponse();

                  dto.setUserId(user.getId());
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

                  return dto;
                })
            .collect(Collectors.toList());

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

  @GetMapping("/auctions")
  public ResponseEntity<List<AuctionItemDTO>> getAllAuctions() {
    return ResponseEntity.ok(adminService.findAllAuctions());
  }

  @DeleteMapping("/auctions/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteAuction(@PathVariable Long id) {
    try {
      adminService.deleteAuction(id);
      return ResponseEntity.ok(
          new ApiResponse<>(
              true, "Đã xóa phiên đấu giá và chuyển trạng thái sang CANCELLED.", null));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
    } catch (Exception e) {
      return ResponseEntity.internalServerError()
          .body(new ApiResponse<>(false, "Lỗi Server: " + e.getMessage(), null));
    }
  }
}
