package io.auctionsystem.server.repository;

import io.auctionsystem.common.enums.AuctionState;
import io.auctionsystem.server.model.Auction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long> {

    List<Auction> findByStatus(AuctionState status);

    List<Auction> findByStatusAndEndTimeBefore(AuctionState status, LocalDateTime currentTime);

    List<Auction> findByStatusAndStartTimeLessThanEqual(AuctionState status, LocalDateTime currentTime);

    List<Auction> findByItemId(Long itemId);

    Optional<Auction> findTopByItemIdOrderByIdDesc(Long itemId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Auction a WHERE a.id = :auctionId")
    Optional<Auction> findByIdForUpdate(@Param("auctionId") Long auctionId);

    // ← THÊM VÀO ĐÂY
    @Query("SELECT MONTH(a.startTime) as month, COUNT(a) as total " +
            "FROM Auction a " +
            "WHERE YEAR(a.startTime) = :year " +
            "GROUP BY MONTH(a.startTime) " +
            "ORDER BY MONTH(a.startTime)")
    List<Object[]> countByMonth(@Param("year") int year);
}
