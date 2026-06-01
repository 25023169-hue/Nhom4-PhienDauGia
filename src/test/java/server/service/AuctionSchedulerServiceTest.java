package server.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import common.enums.AuctionState;
import server.model.Auction;
import server.model.SellerProductListing;
import server.repository.AuctionRepository;
import server.repository.SellerProductListingRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class AuctionSchedulerServiceTest {

  @InjectMocks private AuctionSchedulerService schedulerService;

  @Mock private AuctionRepository auctionRepository;
  @Mock private SellerProductListingRepository listingRepository;
  @Mock private AuctionSettlementService settlementService;
  @Mock private AuctionRealtimePublisher realtimePublisher;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void autoCloseExpiredAuctions_ClosesOpenAuctionMissedWhileServerWasOffline() {
    Auction missedAuction = new Auction();
    missedAuction.setId(1L);

    when(auctionRepository.findByStatusAndEndTimeBefore(
            org.mockito.ArgumentMatchers.eq(AuctionState.RUNNING),
            org.mockito.ArgumentMatchers.any(LocalDateTime.class)))
        .thenReturn(List.of());
    when(auctionRepository.findByStatusAndEndTimeBefore(
            org.mockito.ArgumentMatchers.eq(AuctionState.OPEN),
            org.mockito.ArgumentMatchers.any(LocalDateTime.class)))
        .thenReturn(List.of(missedAuction));
    when(settlementService.closeExpiredAuction(
            org.mockito.ArgumentMatchers.eq(1L),
            org.mockito.ArgumentMatchers.any(LocalDateTime.class)))
        .thenReturn(true);

    schedulerService.autoCloseExpiredAuctions();

    verify(settlementService)
        .closeExpiredAuction(
            org.mockito.ArgumentMatchers.eq(1L),
            org.mockito.ArgumentMatchers.any(LocalDateTime.class));
  }

  @Test
  void autoStartScheduledAuctions_StartsValidAuctionAndListing() {
    Auction auction = new Auction();
    auction.setId(1L);
    auction.setItemId(2L);
    auction.setEndTime(LocalDateTime.now().plusHours(1));
    SellerProductListing listing = new SellerProductListing();
    when(auctionRepository.findByStatusAndStartTimeLessThanEqual(
            org.mockito.ArgumentMatchers.eq(AuctionState.OPEN),
            org.mockito.ArgumentMatchers.any(LocalDateTime.class)))
        .thenReturn(List.of(auction));
    when(listingRepository.findByItemId(2L)).thenReturn(Optional.of(listing));

    schedulerService.autoStartScheduledAuctions();

    org.junit.jupiter.api.Assertions.assertEquals(AuctionState.RUNNING, auction.getStatus());
    org.junit.jupiter.api.Assertions.assertEquals(AuctionState.RUNNING, listing.getStatus());
    verify(auctionRepository).save(auction);
    verify(listingRepository).save(listing);
    verify(realtimePublisher).publishStatusAfterCommit(1L, AuctionState.RUNNING);
    verify(realtimePublisher).publishAuctionListChangedAfterCommit();
  }

  @Test
  void autoStartScheduledAuctions_SkipsExpiredOpenAuction() {
    Auction auction = new Auction();
    auction.setEndTime(LocalDateTime.now().minusSeconds(1));
    when(auctionRepository.findByStatusAndStartTimeLessThanEqual(
            org.mockito.ArgumentMatchers.eq(AuctionState.OPEN),
            org.mockito.ArgumentMatchers.any(LocalDateTime.class)))
        .thenReturn(List.of(auction));

    schedulerService.autoStartScheduledAuctions();

    org.mockito.Mockito.verify(auctionRepository, org.mockito.Mockito.never()).save(auction);
  }
}
