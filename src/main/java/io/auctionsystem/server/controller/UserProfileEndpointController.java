package io.auctionsystem.server.controller;

import io.auctionsystem.server.service.UserProfileLogicService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user-profile")
public class UserProfileEndpointController {

  @Autowired private UserProfileLogicService userProfileLogicService;

  @PutMapping("/{id}/name")
  public ResponseEntity<?> updateProfileName(
      @PathVariable("id") Long id, @RequestBody Map<String, String> payload) {
    try {
      userProfileLogicService.updateProfile(id, payload);
      return ResponseEntity.ok().body("Cập nhật tên thành công!");
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }

  @PutMapping("/{id}/password")
  public ResponseEntity<?> updatePassword(
      @PathVariable("id") Long id, @RequestBody Map<String, String> payload) {
    try {
      userProfileLogicService.updatePassword(id, payload);
      return ResponseEntity.ok().body("Đổi mật khẩu thành công!");
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }
}
