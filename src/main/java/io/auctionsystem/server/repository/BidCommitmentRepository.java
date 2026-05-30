package io.auctionsystem.server.repository;

import io.auctionsystem.common.enums.BidCommitmentStatus;
import io.auctionsystem.server.model.BidCommitment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BidCommitmentRepository extends JpaRepository<BidCommitment, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<BidCommitment> findByAuctionIdAndBidderId(Long auctionId, Long bidderId);

    List<BidCommitment> findByAuctionIdAndStatusOrderByBidderIdAsc(
            Long auctionId,
            BidCommitmentStatus status
    );

    boolean existsByBidderIdAndStatus(Long bidderId, BidCommitmentStatus status);
}
