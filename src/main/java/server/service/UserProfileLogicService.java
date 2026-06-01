package server.service;

import server.exception.ResourceNotFoundException;
import server.model.User;
import server.repository.UserRepository;
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
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng!"));
    user.setFirstname(payload.get("firstname"));
    user.setLastname(payload.get("lastname"));
    return userRepository.save(user);
  }
}
