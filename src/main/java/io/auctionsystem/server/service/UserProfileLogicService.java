package io.auctionsystem.server.service;

import io.auctionsystem.server.model.User;
import io.auctionsystem.server.repository.UserRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProfileLogicService {

  private final UserRepository userRepository;

  public User updateProfile(Long userId, Map<String, String> payload) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng!"));
    ensureActive(user);
    user.setFirstname(payload.get("firstname"));
    user.setLastname(payload.get("lastname"));
    return userRepository.save(user);
  }

  public void updatePassword(Long userId, Map<String, String> payload) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng!"));
    ensureActive(user);

    if (!user.getPassword().equals(payload.get("oldPassword"))) {
      throw new IllegalArgumentException("Mật khẩu cũ không chính xác!");
    }

    user.setPassword(payload.get("newPassword"));
    userRepository.save(user);
  }

  private void ensureActive(User user) {
    if (!user.isActive()) {
      throw new IllegalArgumentException("Tài khoản đã bị vô hiệu hóa");
    }
  }
}
