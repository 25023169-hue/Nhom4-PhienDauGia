package io.auctionsystem.server.service;

import io.auctionsystem.server.model.User;
import io.auctionsystem.server.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

  @Autowired private UserRepository userRepository;

  // 1. Hàm lấy toàn bộ danh sách User (Phải trùng tên với bên Controller gọi)
  public List<User> findAllUsers() {
    return userRepository.findAll();
  }

  // 2. Hàm Khóa/Mở khóa tài khoản
  public boolean toggleBanUser(Long id) {
    Optional<User> userOpt = userRepository.findById(id);
    if (userOpt.isPresent()) {
      User user = userOpt.get();
      // Đảo ngược trạng thái ban (nếu đang true thì thành false và ngược lại)
      // Lưu ý: Trong Model User của bạn phải có biến boolean isBanned hoặc tương tự
      user.setBanned(!user.isBanned());
      userRepository.save(user);
      return true;
    }
    return false;
  }

  // 3. Hàm xóa User
  public void deleteUser(Long id) {
    userRepository.deleteById(id);
  }

  public boolean isSeller(Long userId) {
    return userRepository.isUserSeller(userId) > 0;
  }
}
