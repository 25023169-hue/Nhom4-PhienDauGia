package io.auctionsystem.server.dao;

import io.auctionsystem.server.model.AutoBid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AutoBidDAO extends JpaRepository<AutoBid, Long> {

    // Lấy danh sách tất cả các cài đặt Auto-Bid đang kích hoạt của một phiên đấu giá
    List<AutoBid> findByAuctionIdAndIsActiveTrue(Long auctionId);

    // Tìm cấu hình AutoBid của một user cụ thể trong một phiên cụ thể để cập nhật/tắt
    Optional<AutoBid> findByAuctionIdAndBidderId(Long auctionId, Long bidderId);
}