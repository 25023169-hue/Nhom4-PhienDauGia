package server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import common.enums.AuctionState;
import server.model.Auction;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class AntiSnipingServiceTest {

  @InjectMocks private AntiSnipingService antiSnipingService;

  @Mock private AuctionRealtimePublisher realtimePublisher;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void extendIfNeeded_BidInLast60Seconds_ExtendsAndPublishes() {
    LocalDateTime bidTime = LocalDateTime.now();
    Auction auction = runningAuction(bidTime.plusSeconds(30));

    assertTrue(antiSnipingService.extendIfNeeded(auction, bidTime));

    assertEquals(bidTime.plusSeconds(90), auction.getEndTime());
    verify(realtimePublisher).publishExtendedEndTimeAfterCommit(eq(1L), anyString());
  }

  @Test
  void extendIfNeeded_MoreThan60SecondsLeft_DoesNotExtend() {
    LocalDateTime bidTime = LocalDateTime.now();
    Auction auction = runningAuction(bidTime.plusSeconds(61));

    assertFalse(antiSnipingService.extendIfNeeded(auction, bidTime));

    assertEquals(bidTime.plusSeconds(61), auction.getEndTime());
    verify(realtimePublisher, never()).publishExtendedEndTimeAfterCommit(eq(1L), anyString());
  }

  @Test
  void extendIfNeeded_FinishedAuction_DoesNotExtend() {
    LocalDateTime bidTime = LocalDateTime.now();
    Auction auction = runningAuction(bidTime.plusSeconds(30));
    auction.setStatus(AuctionState.FINISHED);

    assertFalse(antiSnipingService.extendIfNeeded(auction, bidTime));

    verify(realtimePublisher, never()).publishExtendedEndTimeAfterCommit(eq(1L), anyString());
  }

  @Test
  void extendIfNeeded_MissingAuction_DoesNotExtend() {
    assertFalse(antiSnipingService.extendIfNeeded(null, LocalDateTime.now()));

    verify(realtimePublisher, never()).publishExtendedEndTimeAfterCommit(eq(1L), anyString());
  }

  private Auction runningAuction(LocalDateTime endTime) {
    Auction auction = new Auction();
    auction.setId(1L);
    auction.setStatus(AuctionState.RUNNING);
    auction.setEndTime(endTime);
    return auction;
  }
}
