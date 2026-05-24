package io.auctionsystem.server.service;

import io.auctionsystem.server.model.User;
import io.auctionsystem.server.repository.AuctionRepository;  // ← THÊM
import io.auctionsystem.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;  // ← THÊM
import java.util.List;
import java.util.Map;            // ← THÊM
import java.util.Optional;

@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuctionRepository auctionRepository;  // ← THÊM

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    public boolean toggleBanUser(Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setBanned(!user.isBanned());
            userRepository.save(user);
            return true;
        }
        return false;
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public boolean isSeller(Long userId) {
        return userRepository.isUserSeller(userId) > 0;
    }

    // ← THÊM VÀO ĐÂY
    public Map<Integer, Long> getMonthlyAuctionStats(int year) {
        List<Object[]> raw = auctionRepository.countByMonth(year);
        Map<Integer, Long> result = new LinkedHashMap<>();
        for (Object[] row : raw) {
            int month = ((Number) row[0]).intValue();
            long count = ((Number) row[1]).longValue();
            result.put(month, count);
        }
        return result;
    }
}