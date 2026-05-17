package io.auctionsystem.server.service;

import io.auctionsystem.common.dto.AddressRequest;
import io.auctionsystem.common.dto.BankRequest;
import io.auctionsystem.server.model.User;
import io.auctionsystem.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // Cập nhật thông tin ngân hàng
    public User updateBankInfo(Long userId, BankRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User với ID: " + userId));

        user.setBankName(request.getBankName());
        user.setAccountName(request.getAccountName());
        user.setBankAccount(request.getBankAccount());

        return userRepository.save(user);
    }

    // Cập nhật địa chỉ
    public User updateAddress(Long userId, AddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User với ID: " + userId));

        user.setAddress(request.getAddress());

        return userRepository.save(user);
    }
}