package io.auctionsystem.server.service;

import io.auctionsystem.common.enums.AuctionState;
import io.auctionsystem.server.model.Auction;
import io.auctionsystem.server.repository.AuctionRepository;
import io.auctionsystem.server.repository.SellerProductListingRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuctionSchedulerService {

  @Autowired private AuctionRepository auctionRepository;

  @Autowired private SellerProductListingRepository listingRepository;

  @Autowired private AuctionSettlementService settlementService;

  @Autowired private AuctionRealtimePublisher realtimePublisher;

  @Scheduled(fixedRate = 1000)
  @Transactional
  public void autoStartScheduledAuctions() {
    LocalDateTime now = LocalDateTime.now();
    List<Auction> scheduledAuctions =
        auctionRepository.findByStatusAndStartTimeLessThanEqual(AuctionState.OPEN, now);

    for (Auction auction : scheduledAuctions) {
      if (auction.getEndTime() == null || !now.isBefore(auction.getEndTime())) {
        continue;
      }

      auction.setStatus(AuctionState.RUNNING);
      auctionRepository.save(auction);
      listingRepository
          .findByItemId(auction.getItemId())
          .ifPresent(
              listing -> {
                listing.setStatus(AuctionState.RUNNING);
                listingRepository.save(listing);
              });
      realtimePublisher.publishStatusAfterCommit(auction.getId(), "RUNNING");
      realtimePublisher.publishAuctionListChangedAfterCommit();
    }
  }

  @Scheduled(fixedRate = 1000)
  public void autoCloseExpiredAuctions() {
    LocalDateTime now = LocalDateTime.now();
    List<Auction> expiredAuctions =
        auctionRepository.findByStatusAndEndTimeBefore(AuctionState.RUNNING, now);

    for (Auction auction : expiredAuctions) {
      if (settlementService.closeExpiredAuction(auction.getId(), now)) {
        System.out.println(
            ">>> [HỆ THỐNG] Đã tự động đóng phiên đấu giá ID: "
                + auction.getId()
                + " | Người thắng cuộc ID: "
                + auction.getWinnerId()
                + " | Giá chốt: "
                + auction.getFinalPrice());
      }
    }
  }
}
