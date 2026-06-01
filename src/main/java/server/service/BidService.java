package server.service;

import common.dto.BidDTO;
import common.enums.AuctionState;
import common.enums.BidCommitmentStatus;
import common.request.BidRequest;
import common.response.BidResponse;
import server.exception.AccountException;
import server.exception.AuctionClosedException;
import server.exception.InvalidBidException;
import server.exception.InvalidOperationException;
import server.exception.ResourceNotFoundException;
import server.exception.ValidationException;
import server.model.Auction;
import server.model.Bid;
import server.model.BidCommitment;
import server.model.Item;
import server.model.SellerProductListing;
import server.model.User;
import server.repository.AuctionRepository;
import server.repository.BidCommitmentRepository;
import server.repository.BidRepository;
import server.repository.ItemRepository;
import server.repository.SellerProductListingRepository;
import server.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BidService {

  private final BidRepository bidRepository;

  private final BidCommitmentRepository bidCommitmentRepository;

  private final AuctionRepository auctionRepository;

  private final UserRepository userRepository;

  private final ItemRepository itemRepository;

  private final SellerProductListingRepository listingRepository;

  private final AuctionRealtimePublisher realtimePublisher;

  private final AuctionSettlementService settlementService;

  private final AntiSnipingService antiSnipingService;

  private final AuctionNotificationService auctionNotificationService;

  @Transactional
  public BidResponse placeBid(BidRequest request) {
    if (request == null
        || request.getAuctionId() == null
        || request.getBidderId() == null
        || request.getAmount() == null
        || request.getAmount() <= 0) {
      throw new ValidationException("Thông tin đặt giá không hợp lệ");
    }

    // Khóa phiên trước để mọi bid trong cùng phiên được xử lý tuần tự.
    Auction auction =
        auctionRepository
            .findByIdForUpdate(request.getAuctionId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên đấu giá"));

    LocalDateTime now = LocalDateTime.now();
    if (auction.getStatus() != AuctionState.RUNNING
        || auction.getStartTime() == null
        || auction.getEndTime() == null
        || now.isBefore(auction.getStartTime())
        || !now.isBefore(auction.getEndTime())) {
      throw new AuctionClosedException("Phiên đấu giá không trong trạng thái mở");
    }

    User bidder =
        userRepository
            .findByIdForUpdate(request.getBidderId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
    if (!bidder.isActive()) {
      throw new AccountException("Tài khoản đã bị vô hiệu hóa");
    }

    Item item =
        itemRepository
            .findById(auction.getItemId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm đấu giá"));

    if (item.getSeller() != null && item.getSeller().getId().equals(bidder.getId())) {
      throw new InvalidOperationException("Seller không thể tự đặt giá sản phẩm của mình");
    }

    double currentHighest = item.getCurrentPrice();
    if (request.getAmount() <= currentHighest) {
      throw new InvalidBidException(
          "Giá đặt ("
              + request.getAmount()
              + ") phải lớn hơn giá hiện tại ("
              + currentHighest
              + ")");
    }

    BidCommitment commitment =
        bidCommitmentRepository
            .findByAuctionIdAndBidderId(request.getAuctionId(), request.getBidderId())
            .orElseGet(BidCommitment::new);

    double previousPrice = commitment.getId() == null ? 0.0 : commitment.getAmount();
    double additionalHold = request.getAmount() - previousPrice;
    if (bidder.getAvailableBalance() < additionalHold) {
      throw new InvalidBidException("Số dư trong ví không đủ để đặt giá");
    }

    // Giá sàn được cập nhật trước. Toàn bộ thao tác vẫn nằm trong cùng transaction,
    // nên client khác chỉ nhìn thấy giá mới sau khi khoản giữ tiền đã thành công.
    item.setCurrentPrice(request.getAmount());
    itemRepository.saveAndFlush(item);

    bidder.setHeldBalance(bidder.getHeldBalance() + additionalHold);
    userRepository.save(bidder);

    commitment.setAuctionId(request.getAuctionId());
    commitment.setBidderId(request.getBidderId());
    commitment.setAmount(request.getAmount());
    commitment.setStatus(BidCommitmentStatus.ACTIVE);
    bidCommitmentRepository.save(commitment);

    Bid newBid = new Bid();
    newBid.setAuctionId(request.getAuctionId());
    newBid.setBidderId(request.getBidderId());
    newBid.setAmount(request.getAmount());
    newBid.setBidTime(now);
    bidRepository.save(newBid);

    Long previousWinnerId = auction.getWinnerId();
    auction.setWinnerId(request.getBidderId());
    auction.setFinalPrice(request.getAmount());

    boolean reachedBuyNowPrice =
        listingRepository
            .findByItemId(item.getId())
            .map(SellerProductListing::getBuyNowPrice)
            .filter(buyNowPrice -> request.getAmount() >= buyNowPrice)
            .isPresent();

    if (!reachedBuyNowPrice) {
      antiSnipingService.extendIfNeeded(auction, now);
    }
    auctionRepository.save(auction);

    BidResponse response =
        new BidResponse(
            true,
            "Đặt giá thành công!",
            auction.getId(),
            request.getAmount(),
            bidder.getUsername(),
            previousWinnerId);
    realtimePublisher.publishPriceAfterCommit(request.getAuctionId(), request.getAmount());
    auctionNotificationService.notifyBidAfterCommit(response, request.getBidderId());
    if (reachedBuyNowPrice) {
      settlementService.closeBuyNowAuction(auction.getId());
    }

    return response;
  }

  public List<BidDTO> getBidHistoryByBidder(Long bidderId) {
    return bidRepository.findByBidderIdOrderByBidTimeDesc(bidderId).stream()
        .map(this::toDTO)
        .toList();
  }

  public List<BidDTO> getBidsByAuction(Long auctionId) {
    List<Bid> bids = bidRepository.findByAuctionIdOrderByBidTimeDesc(auctionId);
    Collections.reverse(bids);
    return bids.stream().map(this::toDTO).toList();
  }

  private BidDTO toDTO(Bid bid) {
    return new BidDTO(
        bid.getId(), bid.getBidderId(), bid.getAuctionId(), bid.getAmount(), bid.getBidTime());
  }
}
