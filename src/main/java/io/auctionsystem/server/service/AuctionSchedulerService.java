package io.auctionsystem.server.service;

import io.auctionsystem.common.enums.AuctionState;
import io.auctionsystem.server.model.Auction;
import io.auctionsystem.server.repository.AuctionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuctionSchedulerService {

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Chạy ngầm định kỳ mỗi 1000 mili-giây (1 giây)
    @Scheduled(fixedRate = 1000)
    @Transactional
    public void autoCloseExpiredAuctions() {
        LocalDateTime now = LocalDateTime.now();

        // Gọi hàm repository đã được định nghĩa sẵn trong hệ thống của nhóm
        List<Auction> expiredAuctions = auctionRepository.findByStatusAndEndTimeBefore(AuctionState.RUNNING, now);

        if (!expiredAuctions.isEmpty()) {
            for (Auction auction : expiredAuctions) {
                // Chuyển trạng thái sang FINISHED (Đã kết thúc)
                auction.setStatus(AuctionState.FINISHED);
                auctionRepository.save(auction);

                // Phát tín hiệu WebSocket cho tất cả Client đang xem biết phiên này đã đóng
                messagingTemplate.convertAndSend("/topic/auctions/" + auction.getId() + "/status", "CLOSED");

                System.out.println(">>> [HỆ THỐNG] Đã tự động đóng phiên đấu giá ID: " + auction.getId()
                        + " | Người thắng cuộc ID: " + auction.getWinnerId()
                        + " | Giá chốt: " + auction.getFinalPrice());
            }
        }
    }
}