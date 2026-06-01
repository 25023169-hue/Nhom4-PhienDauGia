package server.service;

import common.enums.AuctionState;
import common.enums.BidCommitmentStatus;
import common.request.AddressRequest;
import common.request.BankRequest;
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

  public User updateBankInfo(Long userId, BankRequest request) {
    Bidder bidder = getBidder(userId);
    bidder.setBankName(request.getBankName());
    bidder.setAccountName(request.getAccountName());
    bidder.setBankAccount(request.getBankAccount());
    return userRepository.save(bidder);
  }

  public User updateAddress(Long userId, AddressRequest request) {
    Bidder bidder = getBidder(userId);
    bidder.setAddress(request.getAddress());
    return userRepository.save(bidder);
  }

  @Transactional
  public void deleteAccount(Long userId) {
    User user =
        userRepository
            .findByIdForUpdate(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));

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

  private Bidder getBidder(Long userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
    if (user instanceof Bidder bidder) {
      return bidder;
    }
    throw new ResourceNotFoundException("Không tìm thấy tài khoản");
  }
}
