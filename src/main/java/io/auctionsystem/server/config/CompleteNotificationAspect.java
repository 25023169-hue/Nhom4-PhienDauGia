package io.auctionsystem.server.config;

import io.auctionsystem.common.request.BidRequest;
import io.auctionsystem.common.response.BidResponse;
import io.auctionsystem.server.model.Auction;
import io.auctionsystem.server.model.Bid;
import io.auctionsystem.server.model.Item;
import io.auctionsystem.server.repository.AuctionRepository;
import io.auctionsystem.server.repository.BidRepository;
import io.auctionsystem.server.repository.ItemRepository;
import io.auctionsystem.server.service.NotificationService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

@Aspect
@Component
public class CompleteNotificationAspect implements InitializingBean {

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private NotificationService notificationService;

    // Bộ nhớ đệm giúp ngăn chặn gửi thông báo lặp lại
    private final Set<Long> processedAuctions = Collections.synchronizedSet(new HashSet<>());

    // Nạp toàn bộ các phiên đã đóng vào bộ nhớ đệm khi khởi động Server để chặn spam
    @Override
    public void afterPropertiesSet() {
        try {
            List<Auction> allAuctions = auctionRepository.findAll();
            for (Auction a : allAuctions) {
                if (a.getStatus() != null && !a.getStatus().name().equals("RUNNING")) {
                    processedAuctions.add(a.getId());
                }
            }
            System.out.println("[CompleteNotificationAspect] Đã nạp " + processedAuctions.size() + " phiên lịch sử vào bộ nhớ đệm.");
        } catch (Exception e) {
            System.err.println("Lỗi nạp processedAuctions: " + e.getMessage());
        }
    }

    // ==============================================================================
    // 1. KHI ĐẶT GIÁ HOẶC MUA ĐỨT
    // ==============================================================================
    @Around("execution(* io.auctionsystem.server.service.BidService.placeBid(..)) && args(request)")
    public Object handleBidNotifications(ProceedingJoinPoint pjp, BidRequest request) throws Throwable {
        Long auctionId = request.getAuctionId();
        Optional<Bid> prevHighest = bidRepository.findTopByAuctionIdOrderByAmountDesc(auctionId);
        Long prevBidderId = prevHighest.map(Bid::getBidderId).orElse(null);

        Object result = pjp.proceed();

        if (result instanceof BidResponse) {
            BidResponse res = (BidResponse) result;
            if (res.isAccepted()) {
                Auction auction = auctionRepository.findById(auctionId).orElse(null);
                Item item = getItemByAuctionId(auctionId);
                Long sellerId = (item != null && item.getSeller() != null) ? item.getSeller().getId() : null;
                Long newBidderId = request.getBidderId();

                // NẾU MUA ĐỨT: Phiên đổi trạng thái ngay lập tức
                if (auction != null && auction.getStatus() != null && !auction.getStatus().name().equals("RUNNING")) {
                    if (!processedAuctions.contains(auctionId)) {
                        sendFinishNotifications(auction, sellerId);
                        processedAuctions.add(auctionId);
                    }
                }
                // ĐẶT GIÁ THÔNG THƯỜNG
                else {
                    if (prevBidderId != null && !prevBidderId.equals(newBidderId)) {
                        notificationService.createNotification(prevBidderId, "Bạn đã bị vượt giá tại phiên sản phẩm #" + auctionId + "!", "BID_OUTBID");
                    }
                    if (sellerId != null && !sellerId.equals(newBidderId)) {
                        notificationService.createNotification(sellerId, "Có lượt đặt giá mới: " + formatVND(res.getNewCurrentPrice()) + " cho sản phẩm của bạn.", "NEW_BID");
                    }
                }
            }
        }
        return result;
    }

    // ==============================================================================
    // 2. KHI HẾT HẠN TỰ ĐỘNG (Quét toàn bộ, không phụ thuộc múi giờ)
    // ==============================================================================
    @AfterReturning("execution(* io.auctionsystem.server.service.AuctionService.checkAndFinishExpiredAuctions(..))")
    public void handleExpiredAuctions() {
        List<Auction> allAuctions = auctionRepository.findAll();
        for (Auction auction : allAuctions) {
            if (auction.getStatus() != null && !auction.getStatus().name().equals("RUNNING")) {
                if (!processedAuctions.contains(auction.getId())) {
                    Item item = getItemByAuctionId(auction.getId());
                    Long sellerId = (item != null && item.getSeller() != null) ? item.getSeller().getId() : null;
                    sendFinishNotifications(auction, sellerId);
                    processedAuctions.add(auction.getId());
                }
            }
        }
    }

    // ==============================================================================
    // 3. KHI HỦY PHIÊN BỞI SELLER (@Before - Đọc dữ liệu TRƯỚC KHI DB xóa)
    // ==============================================================================
    @Before(value = "execution(* io.auctionsystem.server.service.SellerProductService.hideProduct(..)) && args(itemId, sellerId)", argNames = "itemId,sellerId")
    public void handleSellerCancelAuction(Long itemId, Long sellerId) {
        List<Auction> auctions = auctionRepository.findByItemId(itemId);
        if (auctions != null) {
            for (Auction auction : auctions) {
                sendCancelNotifications(auction.getId(), sellerId);
            }
        }
    }

    // ==============================================================================
    // 4. KHI ADMIN XÓA PHIÊN ĐẤU GIÁ (@Before - Đọc dữ liệu TRƯỚC KHI DB xóa)
    // ==============================================================================
    @Before(value = "execution(* io.auctionsystem.server.service.AdminService.deleteAuction(..)) && args(auctionId)", argNames = "auctionId")
    public void handleAdminCancelAuction(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId).orElse(null);
        Long sellerId = null;
        if (auction != null) {
            Item item = getItemByAuctionId(auctionId);
            if (item != null && item.getSeller() != null) {
                sellerId = item.getSeller().getId();
            }
        }
        sendCancelNotifications(auctionId, sellerId);
    }

    // ======================= CÁC HÀM TIỆN ÍCH BỔ TRỢ =======================

    private void sendFinishNotifications(Auction auction, Long sellerId) {
        Long auctionId = auction.getId();
        List<Bid> allBids = bidRepository.findByAuctionId(auctionId);
        Set<Long> allBidderIds = allBids.stream().map(Bid::getBidderId).collect(Collectors.toSet());
        Optional<Bid> highestBidOpt = bidRepository.findTopByAuctionIdOrderByAmountDesc(auctionId);

        if (highestBidOpt.isPresent()) {
            Bid winningBid = highestBidOpt.get();
            Long winnerId = winningBid.getBidderId();
            String formattedPrice = formatVND(winningBid.getAmount());

            // Gửi báo thắng
            notificationService.createNotification(winnerId, "Tuyệt vời! Bạn đã thắng phiên đấu giá #" + auctionId + "!", "BID_WON");

            // Gửi báo thua cho TẤT CẢ các ID đã tham gia (trừ người thắng)
            for (Long bidderId : allBidderIds) {
                if (!bidderId.equals(winnerId)) {
                    notificationService.createNotification(bidderId, "Phiên #" + auctionId + " đã kết thúc. Bạn không trúng giải.", "BID_LOST");
                }
            }

            // Gửi báo đã bán cho Seller
            if (sellerId != null) {
                notificationService.createNotification(sellerId, "Chúc mừng! Sản phẩm của bạn đã bán thành công với giá " + formattedPrice + ".", "AUCTION_SOLD");
            }
        } else {
            // KHÔNG CÓ AI MUA -> Gửi báo ế cho Seller
            if (sellerId != null) {
                notificationService.createNotification(sellerId, "Phiên đấu giá đã kết thúc nhưng không có ai mua (Ế).", "AUCTION_EXPIRED");
            }
        }
    }

    private void sendCancelNotifications(Long auctionId, Long sellerId) {
        if (processedAuctions.contains(auctionId)) return;

        if (sellerId != null) {
            notificationService.createNotification(sellerId, "Bạn đã chủ động hủy phiên đấu giá của chính mình.", "AUCTION_CANCELED");
        }

        List<Bid> allBids = bidRepository.findByAuctionId(auctionId);
        if (allBids != null) {
            Set<Long> allBidderIds = allBids.stream().map(Bid::getBidderId).collect(Collectors.toSet());
            for (Long bidderId : allBidderIds) {
                notificationService.createNotification(bidderId, "Phiên đấu giá #" + auctionId + " đã bị hủy.", "AUCTION_CANCELED");
            }
        }
        processedAuctions.add(auctionId);
    }

    private Item getItemByAuctionId(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId).orElse(null);
        if (auction != null && auction.getItemId() != null) {
            return itemRepository.findById(auction.getItemId()).orElse(null);
        }
        return null;
    }

    private String formatVND(double amount) {
        NumberFormat numberFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
        return numberFormat.format(amount) + "đ";
    }
}