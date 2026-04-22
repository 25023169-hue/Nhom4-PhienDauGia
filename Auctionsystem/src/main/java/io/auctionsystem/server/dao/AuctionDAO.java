package io.auctionsystem.server.dao;

import io.auctionsystem.common.enums.AuctionState;
import io.auctionsystem.server.model.Auction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuctionDAO extends JpaRepository<Auction, Long> {

    // Lấy danh sách các phiên đấu giá theo trạng thái (ví dụ: lấy các phiên đang RUNNING)
    List<Auction> findByStatus(AuctionState status);

    // Phục vụ chức năng 3.1.4 (Kết thúc phiên):
    // Tìm các phiên đấu giá đang RUNNING nhưng thời gian endTime đã nhỏ hơn (before) thời gian hiện tại
    List<Auction> findByStatusAndEndTimeBefore(AuctionState status, LocalDateTime currentTime);

    // Tìm các phiên đấu giá của một sản phẩm cụ thể
    List<Auction> findByItemId(Long itemId);
}