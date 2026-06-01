package io.auctionsystem.server.repository;

import io.auctionsystem.server.model.Bid;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {

  // Đã đổi thành 'BidTime' khớp chuẩn với model Bid của bạn
  List<Bid> findByAuctionIdOrderByBidTimeDesc(Long auctionId);

  // Lấy giá trị lớn nhất
  Optional<Bid> findTopByAuctionIdOrderByAmountDesc(Long auctionId);

  List<Bid> findByAuctionId(Long auctionId);

  // Đã đổi thành 'BidTime'
  List<Bid> findByBidderIdOrderByBidTimeDesc(Long bidderId);

  @Query(
      "SELECT b.auctionId FROM Bid b "
          + "WHERE b.bidderId = :bidderId "
          + "GROUP BY b.auctionId "
          + "ORDER BY MAX(b.bidTime) DESC")
  List<Long> findParticipatingAuctionIdsByBidderId(@Param("bidderId") Long bidderId);
}
