package io.auctionsystem.server.service;

import io.auctionsystem.common.enums.AuctionState;
import io.auctionsystem.server.model.Auction;
import io.auctionsystem.server.repository.AuctionRepository;
import io.auctionsystem.server.repository.SellerProductListingRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuctionSchedulerService {

  private static final Logger LOGGER = LoggerFactory.getLogger(AuctionSchedulerService.class);

  private final AuctionRepository auctionRepository;

  private final SellerProductListingRepository listingRepository;

  private final AuctionSettlementService settlementService;

  private final AuctionRealtimePublisher realtimePublisher;

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
        new ArrayList<>(auctionRepository.findByStatusAndEndTimeBefore(AuctionState.RUNNING, now));
    expiredAuctions.addAll(auctionRepository.findByStatusAndEndTimeBefore(AuctionState.OPEN, now));

    for (Auction auction : expiredAuctions) {
      if (settlementService.closeExpiredAuction(auction.getId(), now)) {
        LOGGER.info(
            "Đã tự động đóng phiên đấu giá ID: {} | Người thắng cuộc ID: {} | Giá chốt: {}",
            auction.getId(),
            auction.getWinnerId(),
            auction.getFinalPrice());
      }
    }
  }
}
