package io.auctionsystem.server.repository;

import io.auctionsystem.common.enums.AuctionState;
import io.auctionsystem.server.model.Auction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WonAuctionRepository extends JpaRepository<Auction, Long> {
  List<Auction> findByWinnerIdAndStatus(Long winnerId, AuctionState status);
}
