package server.service;

import common.enums.AuctionState;
import common.enums.BidCommitmentStatus;
import common.request.AddressRequest;
import common.request.BankRequest;
import server.exception.AccountException;
import server.exception.InvalidOperationException;
import server.exception.ResourceNotFoundException;
import server.model.Auction;
import server.model.Bidder;
import server.model.Item;
import server.model.Seller;
import server.model.User;
import server.repository.AuctionRepository;
import server.repository.BidCommitmentRepository;
import server.repository.ItemRepository;
import server.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

  private static final double MONEY_EPSILON = 0.001;

  private final UserRepository userRepository;

  private final BidCommitmentRepository bidCommitmentRepository;

  private final ItemRepository itemRepository;

  private final AuctionRepository auctionRepository;

  public User getUser(Long userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy User với ID: " + userId));
  }

  // Cập nhật thông tin ngân hàng (Giữ nguyên vì ngân hàng vẫn ở class User dùng chung)
  public User updateBankInfo(Long userId, BankRequest request) {
    // 1. Tìm kiếm và hứng bằng kiểu User từ userRepository
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Không tìm thấy User với ID: " + userId));
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
      throw new InvalidOperationException(
          "Tài khoản này không phải là Người mua hoặc Người bán để cập nhật ngân hàng!");
    }
  }

  // Cập nhật địa chỉ
  public User updateAddress(Long userId, AddressRequest request) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Không tìm thấy User với ID: " + userId));
    ensureActive(user);

    if (user instanceof Bidder bidder) {
      bidder.setAddress(request.getAddress());
      return userRepository.save(bidder);
    } else {
      throw new InvalidOperationException("Tài khoản không hỗ trợ cập nhật địa chỉ!");
    }
  }

  @Transactional
  public void deleteAccount(Long userId) {
    User user =
        userRepository
            .findByIdForUpdate(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));

    if (!user.isActive()) {
      throw new AccountException("Tài khoản đã được xóa trước đó");
    }
    if (Math.abs(user.getBalance()) > MONEY_EPSILON) {
      throw new InvalidOperationException(
          "Vui lòng rút hết số dư trong ví trước khi xóa tài khoản");
    }
    if (Math.abs(user.getHeldBalance()) > MONEY_EPSILON
        || bidCommitmentRepository.existsByBidderIdAndStatus(userId, BidCommitmentStatus.ACTIVE)) {
      throw new InvalidOperationException(
          "Không thể xóa tài khoản khi còn tiền đang giữ cho phiên đấu giá");
    }
    if (hasOpenSellerAuction(userId)) {
      throw new InvalidOperationException(
          "Không thể xóa tài khoản khi còn phiên bán đang mở hoặc đang chờ bắt đầu");
    }

    anonymize(user);
    userRepository.save(user);
  }

  private boolean hasOpenSellerAuction(Long userId) {
    for (Item item : itemRepository.findBySellerId(userId)) {
      for (Auction auction : auctionRepository.findByItemId(item.getId())) {
        if (auction.getStatus() == AuctionState.OPEN
            || auction.getStatus() == AuctionState.RUNNING) {
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
      throw new AccountException("Tài khoản đã bị vô hiệu hóa");
    }
  }
}
