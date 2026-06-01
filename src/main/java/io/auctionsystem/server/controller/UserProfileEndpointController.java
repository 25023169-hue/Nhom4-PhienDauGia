package io.auctionsystem.server.controller;

import io.auctionsystem.server.service.UserProfileLogicService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user-profile")
@RequiredArgsConstructor
public class UserProfileEndpointController {

  private final UserProfileLogicService userProfileLogicService;

  @PutMapping("/{id}/name")
  public ResponseEntity<?> updateProfileName(
      @PathVariable("id") Long id, @RequestBody Map<String, String> payload) {
    userProfileLogicService.updateProfile(id, payload);
    return ResponseEntity.ok().body("Cập nhật tên thành công!");
  }

  @PutMapping("/{id}/password")
  public ResponseEntity<?> updatePassword(
      @PathVariable("id") Long id, @RequestBody Map<String, String> payload) {
    userProfileLogicService.updatePassword(id, payload);
    return ResponseEntity.ok().body("Đổi mật khẩu thành công!");
  }
}
