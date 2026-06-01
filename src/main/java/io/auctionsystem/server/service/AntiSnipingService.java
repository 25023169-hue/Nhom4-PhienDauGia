package io.auctionsystem.server.service;

import io.auctionsystem.common.enums.AuctionState;
import io.auctionsystem.server.model.Auction;
import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class AntiSnipingService {

  private static final long EXTENSION_THRESHOLD_SECONDS = 60;
  private static final long EXTENSION_SECONDS = 60;

  private final AuctionRealtimePublisher realtimePublisher;

  public AntiSnipingService(AuctionRealtimePublisher realtimePublisher) {
    this.realtimePublisher = realtimePublisher;
  }

  public boolean extendIfNeeded(Auction auction, LocalDateTime bidTime) {
    if (auction == null
        || auction.getId() == null
        || auction.getStatus() != AuctionState.RUNNING
        || auction.getEndTime() == null) {
      return false;
    }

    long secondsLeft = Duration.between(bidTime, auction.getEndTime()).getSeconds();
    if (secondsLeft <= 0 || secondsLeft > EXTENSION_THRESHOLD_SECONDS) {
      return false;
    }

    auction.setEndTime(auction.getEndTime().plusSeconds(EXTENSION_SECONDS));
    realtimePublisher.publishExtendedEndTimeAfterCommit(
        auction.getId(), auction.getEndTime().toString());
    return true;
  }
}
