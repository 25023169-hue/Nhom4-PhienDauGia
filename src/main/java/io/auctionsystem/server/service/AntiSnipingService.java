package io.auctionsystem.server.service;

import io.auctionsystem.server.model.Auction;
import io.auctionsystem.server.repository.AuctionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AntiSnipingService {

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void notifyIfAntiSnipingTriggered(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId).orElse(null);
        if (auction == null || !auction.getStatus().name().equals("RUNNING")) return;

        // Khoảng thời gian từ lúc này đến khi kết thúc (đã được cộng 60s ngầm ở BidService)
        long secondsLeft = java.time.Duration.between(LocalDateTime.now(), auction.getEndTime()).getSeconds();

        // Nếu thời gian còn lại đang nằm trong ngưỡng vừa được cộng (ví dụ <= 61 giây)
        if (secondsLeft > 0 && secondsLeft <= 61) {
            // Phát tín hiệu WebSocket cho tất cả Client đang xem phiên
            // Client có thể lắng nghe topic này để load lại thời gian EndTime mới
            messagingTemplate.convertAndSend("/topic/auctions/" + auctionId + "/extended", auction.getEndTime().toString());
            System.out.println(">>> [ANTI-SNIPING] Đã phát tín hiệu Web-Socket gia hạn thời gian cho phiên " + auctionId);
        }
    }
}