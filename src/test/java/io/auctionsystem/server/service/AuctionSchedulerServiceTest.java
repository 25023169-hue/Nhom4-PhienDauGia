package io.auctionsystem.server.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.auctionsystem.common.enums.AuctionState;
import io.auctionsystem.server.model.Auction;
import io.auctionsystem.server.repository.AuctionRepository;
import io.auctionsystem.server.repository.SellerProductListingRepository;
import java.time.LocalDateTime;
import java.util.List;
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
}
