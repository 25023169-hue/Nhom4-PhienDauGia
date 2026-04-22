package io.auctionsystem.server.dao;

import io.auctionsystem.server.model.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BidDAO extends JpaRepository<Bid, Long> {

    // Đã đổi thành 'BidTime' khớp chuẩn với model Bid của bạn
    List<Bid> findByAuctionIdOrderByBidTimeDesc(Long auctionId);

    // Lấy giá trị lớn nhất
    Optional<Bid> findTopByAuctionIdOrderByAmountDesc(Long auctionId);

    // Đã đổi thành 'BidTime'
    List<Bid> findByBidderIdOrderByBidTimeDesc(Long bidderId);
}