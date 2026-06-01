package server.repository;

import common.enums.BidCommitmentStatus;
import server.model.BidCommitment;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

@Repository
public interface BidCommitmentRepository extends JpaRepository<BidCommitment, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<BidCommitment> findByAuctionIdAndBidderId(Long auctionId, Long bidderId);

  List<BidCommitment> findByAuctionIdAndStatusOrderByBidderIdAsc(
      Long auctionId, BidCommitmentStatus status);

  boolean existsByBidderIdAndStatus(Long bidderId, BidCommitmentStatus status);
}
