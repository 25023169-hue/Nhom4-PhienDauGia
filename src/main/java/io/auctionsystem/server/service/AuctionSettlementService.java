package io.auctionsystem.server.service;

import io.auctionsystem.common.enums.AuctionState;
import io.auctionsystem.common.enums.BidCommitmentStatus;
import io.auctionsystem.server.model.Auction;
import io.auctionsystem.server.model.BidCommitment;
import io.auctionsystem.server.model.Item;
import io.auctionsystem.server.model.User;
import io.auctionsystem.server.repository.AuctionRepository;
import io.auctionsystem.server.repository.BidCommitmentRepository;
import io.auctionsystem.server.repository.ItemRepository;
import io.auctionsystem.server.repository.SellerProductListingRepository;
import io.auctionsystem.server.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuctionSettlementService {

  @Autowired private AuctionRepository auctionRepository;

  @Autowired private BidCommitmentRepository bidCommitmentRepository;

  @Autowired private UserRepository userRepository;

  @Autowired private ItemRepository itemRepository;

  @Autowired private SellerProductListingRepository listingRepository;

  @Autowired private TransactionService transactionService;

  @Autowired private AuctionRealtimePublisher realtimePublisher;

  @Transactional
  public boolean closeExpiredAuction(Long auctionId, LocalDateTime closedAt) {
    Auction auction =
        auctionRepository
            .findByIdForUpdate(auctionId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên đấu giá"));

    if (auction.getStatus() != AuctionState.RUNNING
        || auction.getEndTime() == null
        || auction.getEndTime().isAfter(closedAt)) {
      return false;
    }

    settleAuction(auction, false);
    return true;
  }

  @Transactional
  public boolean closeBuyNowAuction(Long auctionId) {
    Auction auction =
        auctionRepository
            .findByIdForUpdate(auctionId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên đấu giá"));

    if (auction.getStatus() != AuctionState.RUNNING) {
      return false;
    }

    settleAuction(auction, false);
    return true;
  }

  @Transactional
  public boolean cancelAuction(Long auctionId) {
    Auction auction =
        auctionRepository
            .findByIdForUpdate(auctionId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên đấu giá"));

    if (auction.getStatus() != AuctionState.OPEN && auction.getStatus() != AuctionState.RUNNING) {
      return false;
    }

    settleAuction(auction, true);
    return true;
  }

  private void settleAuction(Auction auction, boolean cancelled) {
    Item item =
        itemRepository
            .findById(auction.getItemId())
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm đấu giá"));

    List<BidCommitment> commitments =
        bidCommitmentRepository.findByAuctionIdAndStatusOrderByBidderIdAsc(
            auction.getId(), BidCommitmentStatus.ACTIVE);

    Long winnerId = cancelled ? null : auction.getWinnerId();
    AuctionState finalStatus = winnerId == null ? AuctionState.CANCELLED : AuctionState.FINISHED;
    Map<Long, User> lockedUsers = lockSettlementUsers(item, winnerId, commitments);
    settleBidderCommitments(auction, item, commitments, lockedUsers, winnerId);
    auction.setStatus(finalStatus);
    auctionRepository.save(auction);

    listingRepository
        .findByItemId(item.getId())
        .ifPresent(
            listing -> {
              listing.setStatus(finalStatus);
              listingRepository.save(listing);
            });

    realtimePublisher.publishStatusAfterCommit(auction.getId(), "CLOSED");
    realtimePublisher.publishAuctionListChangedAfterCommit();
  }

  private Map<Long, User> lockSettlementUsers(
      Item item, Long winnerId, List<BidCommitment> commitments) {
    Stream<Long> bidderIds = commitments.stream().map(BidCommitment::getBidderId);
    Stream<Long> sellerIds =
        winnerId == null || item.getSeller() == null
            ? Stream.empty()
            : Stream.of(item.getSeller().getId());

    Map<Long, User> lockedUsers = new HashMap<>();
    Stream.concat(bidderIds, sellerIds)
        .distinct()
        .sorted()
        .forEach(userId -> lockedUsers.put(userId, getLockedUser(userId)));
    return lockedUsers;
  }

  private void settleBidderCommitments(
      Auction auction,
      Item item,
      List<BidCommitment> commitments,
      Map<Long, User> lockedUsers,
      Long winnerId) {
    if (winnerId == null) {
      commitments.forEach(commitment -> releaseCommitment(commitment, lockedUsers));
      return;
    }
    if (item.getSeller() == null) {
      throw new IllegalStateException("Sản phẩm đấu giá không có seller");
    }

    double finalPrice = auction.getFinalPrice() == null ? 0.0 : auction.getFinalPrice();
    BidCommitment winningCommitment =
        commitments.stream()
            .filter(commitment -> winnerId.equals(commitment.getBidderId()))
            .findFirst()
            .orElseThrow(
                () -> new IllegalStateException("Không tìm thấy khoản giữ tiền của người thắng"));

    for (BidCommitment commitment : commitments) {
      if (winnerId.equals(commitment.getBidderId())) {
        payWinningCommitment(commitment, finalPrice, item, lockedUsers);
      } else {
        releaseCommitment(commitment, lockedUsers);
      }
    }

    if (winningCommitment.getAmount() < finalPrice) {
      throw new IllegalStateException("Khoản giữ tiền của người thắng nhỏ hơn giá chốt");
    }
  }

  private void payWinningCommitment(
      BidCommitment commitment, double finalPrice, Item item, Map<Long, User> lockedUsers) {
    User winner = lockedUsers.get(commitment.getBidderId());
    releaseHeldBalance(winner, commitment.getAmount());
    if (winner.getBalance() < finalPrice) {
      throw new IllegalStateException("Số dư người thắng không đủ để kết toán");
    }

    winner.setBalance(winner.getBalance() - finalPrice);
    userRepository.save(winner);
    transactionService.saveTransaction(
        winner.getId(),
        0.0,
        finalPrice,
        winner.getAvailableBalance(),
        "Thanh toán đấu giá",
        "Thanh toán sản phẩm: " + item.getName());

    User seller = lockedUsers.get(item.getSeller().getId());
    seller.setBalance(seller.getBalance() + finalPrice);
    userRepository.save(seller);
    transactionService.saveTransaction(
        seller.getId(),
        finalPrice,
        0.0,
        seller.getAvailableBalance(),
        "Thu nhập",
        "Bán sản phẩm: " + item.getName());

    commitment.setStatus(BidCommitmentStatus.PAID);
    bidCommitmentRepository.save(commitment);
  }

  private void releaseCommitment(BidCommitment commitment, Map<Long, User> lockedUsers) {
    User bidder = lockedUsers.get(commitment.getBidderId());
    releaseHeldBalance(bidder, commitment.getAmount());
    userRepository.save(bidder);
    commitment.setStatus(BidCommitmentStatus.RELEASED);
    bidCommitmentRepository.save(commitment);
  }

  private User getLockedUser(Long userId) {
    return userRepository
        .findByIdForUpdate(userId)
        .orElseThrow(() -> new IllegalStateException("Không tìm thấy người dùng cần kết toán"));
  }

  private void releaseHeldBalance(User user, double amount) {
    user.setHeldBalance(Math.max(0.0, user.getHeldBalance() - amount));
  }
}
