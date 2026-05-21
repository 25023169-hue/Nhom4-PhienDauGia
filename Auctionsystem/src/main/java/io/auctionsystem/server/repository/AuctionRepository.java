package io.auctionsystem.server.repository;

import io.auctionsystem.common.enums.AuctionState;
import io.auctionsystem.server.model.Auction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long> {

    List<Auction> findByStatus(AuctionState status);

    List<Auction> findByStatusAndEndTimeBefore(AuctionState status, LocalDateTime currentTime);

    List<Auction> findByItemId(Long itemId);

    // ← THÊM VÀO ĐÂY
    @Query("SELECT MONTH(a.startTime) as month, COUNT(a) as total " +
            "FROM Auction a " +
            "WHERE YEAR(a.startTime) = :year " +
            "GROUP BY MONTH(a.startTime) " +
            "ORDER BY MONTH(a.startTime)")
    List<Object[]> countByMonth(@Param("year") int year);
}