package io.auctionsystem.server.repository;

import io.auctionsystem.common.enums.AuctionState;
import io.auctionsystem.server.model.Auction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WonAuctionRepository extends JpaRepository<Auction, Long> {
    List<Auction> findByWinnerIdAndStatus(Long winnerId, AuctionState status);
}