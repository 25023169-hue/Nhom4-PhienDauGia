package io.auctionsystem.server.repository;

import io.auctionsystem.server.model.AutoBid;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AutoBidRepository extends JpaRepository<AutoBid, Long> {
    List<AutoBid> findByAuctionIdAndActiveTrue(Long auctionId);
    Optional<AutoBid> findByAuctionIdAndBidderId(Long auctionId, Long bidderId);
}