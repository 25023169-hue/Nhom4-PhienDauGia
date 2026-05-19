package io.auctionsystem.server.service;

import io.auctionsystem.common.request.AddressRequest;
import io.auctionsystem.common.request.BankRequest;
import io.auctionsystem.server.model.User;
import io.auctionsystem.server.model.Bidder;
import io.auctionsystem.server.model.Seller;
import io.auctionsystem.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // Cập nhật thông tin ngân hàng (Giữ nguyên vì ngân hàng vẫn ở class User dùng chung)
    public User updateBankInfo(Long userId, BankRequest request) {
        // 1. Tìm kiếm và hứng bằng kiểu User từ userRepository
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User với ID: " + userId));

        // 2. Kiểm tra nếu user này là một Bidder (hoặc Seller vì Seller extends Bidder)
        if (user instanceof Bidder bidder) {
            // Lúc này Java tự hiểu 'bidder' là kiểu Bidder, bạn tha hồ set thông tin
            bidder.setBankName(request.getBankName());
            bidder.setAccountName(request.getAccountName());
            bidder.setBankAccount(request.getBankAccount());

            // 3. Lưu lại và trả về đối tượng đã cập nhật
            return userRepository.save(bidder);
        } else {
            throw new RuntimeException("Tài khoản này không phải là Người mua hoặc Người bán để cập nhật ngân hàng!");
        }
    }

    // Cập nhật địa chỉ
    public User updateAddress(Long userId, AddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User với ID: " + userId));

        if (user instanceof Bidder bidder) {
            bidder.setAddress(request.getAddress());
            return userRepository.save(bidder);
        } else {
            throw new RuntimeException("Tài khoản không hỗ trợ cập nhật địa chỉ!");
        }
    }
}