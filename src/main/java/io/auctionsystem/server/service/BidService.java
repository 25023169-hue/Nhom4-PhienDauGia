package io.auctionsystem.server.service;

import io.auctionsystem.common.enums.AuctionState;
import io.auctionsystem.common.enums.BidCommitmentStatus;
import io.auctionsystem.common.request.BidRequest;
import io.auctionsystem.common.response.BidResponse;
import io.auctionsystem.server.model.Auction;
import io.auctionsystem.server.model.Bid;
import io.auctionsystem.server.model.BidCommitment;
import io.auctionsystem.server.model.Item;
import io.auctionsystem.server.model.SellerProductListing;
import io.auctionsystem.server.model.User;
import io.auctionsystem.server.repository.AuctionRepository;
import io.auctionsystem.server.repository.BidRepository;
import io.auctionsystem.server.repository.BidCommitmentRepository;
import io.auctionsystem.server.repository.ItemRepository;
import io.auctionsystem.server.repository.SellerProductListingRepository;
import io.auctionsystem.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class BidService {

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private BidCommitmentRepository bidCommitmentRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private SellerProductListingRepository listingRepository;

    @Autowired
    private AuctionRealtimePublisher realtimePublisher;

    @Autowired
    private AuctionSettlementService settlementService;

    @Transactional
    public synchronized BidResponse placeBid(BidRequest request) {
        if (request == null || request.getAuctionId() == null || request.getBidderId() == null
                || request.getAmount() == null || request.getAmount() <= 0) {
            throw new IllegalArgumentException("Thông tin đặt giá không hợp lệ");
        }

        // Khóa phiên trước để mọi bid trong cùng phiên được xử lý tuần tự.
        Auction auction = auctionRepository.findByIdForUpdate(request.getAuctionId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên đấu giá"));

        LocalDateTime now = LocalDateTime.now();
        if (auction.getStatus() != AuctionState.RUNNING
                || auction.getStartTime() == null
                || auction.getEndTime() == null
                || now.isBefore(auction.getStartTime())
                || !now.isBefore(auction.getEndTime())) {
            throw new IllegalArgumentException("Phiên đấu giá không trong trạng thái mở");
        }

        User bidder = userRepository.findByIdForUpdate(request.getBidderId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        if (!bidder.isActive()) {
            throw new IllegalArgumentException("Tài khoản đã bị vô hiệu hóa");
        }

        Item item = itemRepository.findById(auction.getItemId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm đấu giá"));

        if (item.getSeller() != null && item.getSeller().getId().equals(bidder.getId())) {
            throw new IllegalArgumentException("Seller không thể tự đặt giá sản phẩm của mình");
        }

        double currentHighest = item.getCurrentPrice();
        if (request.getAmount() <= currentHighest) {
            throw new IllegalArgumentException("Giá đặt (" + request.getAmount() + ") phải lớn hơn giá hiện tại (" + currentHighest + ")");
        }

        BidCommitment commitment = bidCommitmentRepository
                .findByAuctionIdAndBidderId(request.getAuctionId(), request.getBidderId())
                .orElseGet(BidCommitment::new);

        double previousPrice = commitment.getId() == null ? 0.0 : commitment.getAmount();
        double additionalHold = request.getAmount() - previousPrice;
        if (bidder.getAvailableBalance() < additionalHold) {
            throw new IllegalArgumentException("Số dư trong ví không đủ để đặt giá");
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

        auction.setWinnerId(request.getBidderId());
        auction.setFinalPrice(request.getAmount());

        boolean reachedBuyNowPrice = listingRepository.findByItemId(item.getId())
                .map(SellerProductListing::getBuyNowPrice)
                .filter(buyNowPrice -> request.getAmount() >= buyNowPrice)
                .isPresent();

        if (!reachedBuyNowPrice) {
            long secondsLeft = java.time.Duration.between(now, auction.getEndTime()).getSeconds();
            if (secondsLeft > 0 && secondsLeft <= 60) {
                auction.setEndTime(auction.getEndTime().plusSeconds(60));
            }
        }
        auctionRepository.save(auction);

        realtimePublisher.publishPriceAfterCommit(request.getAuctionId(), request.getAmount());
        if (reachedBuyNowPrice) {
            settlementService.closeBuyNowAuction(auction.getId());
        }

        return new BidResponse(true, "Đặt giá thành công!", auction.getId(), request.getAmount(), bidder.getUsername());
    }

    public List<Bid> getBidHistoryByBidder(Long bidderId) {
        return bidRepository.findByBidderIdOrderByBidTimeDesc(bidderId);
    }

    public List<Bid> getBidsByAuction(Long auctionId) {
        List<Bid> bids = bidRepository.findByAuctionIdOrderByBidTimeDesc(auctionId);
        Collections.reverse(bids);
        return bids;
    }
}
