package io.auctionsystem.server.service;

import io.auctionsystem.common.request.BidRequest;
import io.auctionsystem.common.response.BidResponse;
import io.auctionsystem.server.model.Auction;
import io.auctionsystem.server.model.Bid;
import io.auctionsystem.server.model.Item;
import io.auctionsystem.server.model.User;
import io.auctionsystem.server.repository.AuctionRepository;
import io.auctionsystem.server.repository.BidRepository;
import io.auctionsystem.server.repository.ItemRepository;
import io.auctionsystem.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    private AuctionRepository auctionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Transactional
    public synchronized BidResponse placeBid(BidRequest request) {
        // 1. Xác thực Phiên đấu giá
        Auction auction = auctionRepository.findById(request.getAuctionId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên đấu giá"));

        if (!auction.getStatus().name().equals("RUNNING")) {
            throw new IllegalArgumentException("Phiên đấu giá không trong trạng thái mở");
        }

        // 2. Xác thực Người dùng & Sản phẩm
        User bidder = userRepository.findById(request.getBidderId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        Item item = itemRepository.findById(auction.getItemId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm đấu giá"));

        // 3. Kiểm tra tính hợp lệ của giá đấu
        double currentHighest = item.getCurrentPrice();
        if (request.getAmount() <= currentHighest) {
            throw new IllegalArgumentException("Giá đặt (" + request.getAmount() + ") phải lớn hơn giá hiện tại (" + currentHighest + ")");
        }

        // 4. --- ĐÃ CẬP NHẬT: Nghiệp vụ Ví tiền thích ứng với Transaction ---
        // Nếu cột balance của nhóm trong bảng users đang để trống hoặc bằng 0 (do dùng bảng Transaction),
        // hệ thống sẽ kích hoạt chế độ Sandbox cấp 1.000.000.000đ để nhóm tự do Demo không bị kẹt lỗi.
        double availableBalance = bidder.getBalance();
        if (availableBalance <= 0) {
            availableBalance = 1000000000.0; // Kích hoạt ví ảo 1 tỷ VNĐ để Demo
        }

        double previouslyBidded = getPreviousBidAmount(request.getBidderId(), request.getAuctionId());
        double amountToDeduct = request.getAmount() - previouslyBidded;

        if (availableBalance < amountToDeduct) {
            throw new IllegalArgumentException("Số dư trong ví không đủ để đặt giá");
        }

        // Chỉ trừ tiền vào bảng users nếu nhóm có sử dụng cột balance này
        if (bidder.getBalance() > 0) {
            bidder.setBalance(bidder.getBalance() - amountToDeduct);
            userRepository.save(bidder);
        }

        // Hoàn tiền cho người cũ nếu có cột balance
        if (auction.getWinnerId() != null && !auction.getWinnerId().equals(request.getBidderId())) {
            User previousWinner = userRepository.findById(auction.getWinnerId()).orElse(null);
            if (previousWinner != null && previousWinner.getBalance() > 0) {
                double previousWinnerBidded = getPreviousBidAmount(previousWinner.getId(), request.getAuctionId());
                previousWinner.setBalance(previousWinner.getBalance() + previousWinnerBidded);
                userRepository.save(previousWinner);
            }
        }
        // -------------------------------------------------------------------

        // 5. Lưu lịch sử Bid
        Bid newBid = new Bid();
        newBid.setAuctionId(request.getAuctionId());
        newBid.setBidderId(request.getBidderId());
        newBid.setAmount(request.getAmount());
        newBid.setBidTime(LocalDateTime.now());
        bidRepository.save(newBid);

        // 6. Cập nhật Item và Auction
        item.setCurrentPrice(request.getAmount());
        itemRepository.save(item);

        auction.setWinnerId(request.getBidderId());
        auction.setFinalPrice(request.getAmount());

        // Anti-sniping (Tự động gia hạn 1 phút nếu có người bắn tỉa ở giây cuối)
        long secondsLeft = java.time.Duration.between(LocalDateTime.now(), auction.getEndTime()).getSeconds();
        if (secondsLeft > 0 && secondsLeft <= 60) {
            auction.setEndTime(auction.getEndTime().plusSeconds(60));
        }
        auctionRepository.save(auction);

        // 7. Realtime Update qua WebSocket
        messagingTemplate.convertAndSend("/topic/bids/" + request.getAuctionId(), request.getAmount());

        return new BidResponse(true, "Đặt giá thành công!", auction.getId(), request.getAmount(), bidder.getUsername());
    }

    private double getPreviousBidAmount(Long bidderId, Long auctionId) {
        return bidRepository.findByBidderIdOrderByBidTimeDesc(bidderId).stream()
                .filter(b -> b.getAuctionId().equals(auctionId))
                .findFirst()
                .map(Bid::getAmount)
                .orElse(0.0);
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