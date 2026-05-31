package io.auctionsystem.server.service;

import io.auctionsystem.common.request.AddressRequest;
import io.auctionsystem.common.request.BankRequest;
import io.auctionsystem.common.enums.AuctionState;
import io.auctionsystem.common.enums.BidCommitmentStatus;
import io.auctionsystem.server.model.Auction;
import io.auctionsystem.server.model.Item;
import io.auctionsystem.server.model.User;
import io.auctionsystem.server.model.Bidder;
import io.auctionsystem.server.model.Seller;
import io.auctionsystem.server.repository.AuctionRepository;
import io.auctionsystem.server.repository.BidCommitmentRepository;
import io.auctionsystem.server.repository.ItemRepository;
import io.auctionsystem.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private static final double MONEY_EPSILON = 0.001;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BidCommitmentRepository bidCommitmentRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User với ID: " + userId));
    }

    // Cập nhật thông tin ngân hàng (Giữ nguyên vì ngân hàng vẫn ở class User dùng chung)
    public User updateBankInfo(Long userId, BankRequest request) {
        // 1. Tìm kiếm và hứng bằng kiểu User từ userRepository
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User với ID: " + userId));
        ensureActive(user);

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
        ensureActive(user);

        if (user instanceof Bidder bidder) {
            bidder.setAddress(request.getAddress());
            return userRepository.save(bidder);
        } else {
            throw new RuntimeException("Tài khoản không hỗ trợ cập nhật địa chỉ!");
        }
    }

    @Transactional
    public void deleteAccount(Long userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản"));

        if (!user.isActive()) {
            throw new IllegalArgumentException("Tài khoản đã được xóa trước đó");
        }
        if (Math.abs(user.getBalance()) > MONEY_EPSILON) {
            throw new IllegalArgumentException("Vui lòng rút hết số dư trong ví trước khi xóa tài khoản");
        }
        if (Math.abs(user.getHeldBalance()) > MONEY_EPSILON
                || bidCommitmentRepository.existsByBidderIdAndStatus(userId, BidCommitmentStatus.ACTIVE)) {
            throw new IllegalArgumentException("Không thể xóa tài khoản khi còn tiền đang giữ cho phiên đấu giá");
        }
        if (hasOpenSellerAuction(userId)) {
            throw new IllegalArgumentException("Không thể xóa tài khoản khi còn phiên bán đang mở hoặc đang chờ bắt đầu");
        }

        anonymize(user);
        userRepository.save(user);
    }

    private boolean hasOpenSellerAuction(Long userId) {
        for (Item item : itemRepository.findBySellerId(userId)) {
            for (Auction auction : auctionRepository.findByItemId(item.getId())) {
                if (auction.getStatus() == AuctionState.OPEN || auction.getStatus() == AuctionState.RUNNING) {
                    return true;
                }
            }
        }
        return false;
    }

    private void anonymize(User user) {
        user.setActive(false);
        user.setUsername("deleted_" + user.getId() + "_" + UUID.randomUUID());
        user.setPassword(UUID.randomUUID().toString());
        user.setFirstname("Tài khoản");
        user.setLastname("đã xóa");

        if (user instanceof Bidder bidder) {
            bidder.setBankName(null);
            bidder.setAccountName(null);
            bidder.setBankAccount(null);
            bidder.setAddress(null);
        }
        if (user instanceof Seller seller) {
            seller.setStoreName("Tài khoản đã xóa");
        }
    }

    private void ensureActive(User user) {
        if (!user.isActive()) {
            throw new IllegalArgumentException("Tài khoản đã bị vô hiệu hóa");
        }
    }
}
