package io.auctionsystem.server.service;

import io.auctionsystem.server.model.User;
import io.auctionsystem.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UserProfileLogicService {

    @Autowired
    private UserRepository userRepository;

    public User updateProfile(Long userId, Map<String, String> payload) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));
        user.setFirstname(payload.get("firstname"));
        user.setLastname(payload.get("lastname"));
        return userRepository.save(user);
    }

    public void updatePassword(Long userId, Map<String, String> payload) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        if (!user.getPassword().equals(payload.get("oldPassword"))) {
            throw new RuntimeException("Mật khẩu cũ không chính xác!");
        }

        user.setPassword(payload.get("newPassword"));
        userRepository.save(user);
    }
}