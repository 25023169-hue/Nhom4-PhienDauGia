package io.auctionsystem.server.service;

import io.auctionsystem.common.dto.AuctionPriceUpdateDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class AuctionRealtimePublisher {

  @Autowired private SimpMessagingTemplate messagingTemplate;

  public void publishPriceAfterCommit(Long auctionId, Double currentPrice) {
    afterCommit(
        () -> {
          messagingTemplate.convertAndSend("/topic/bids/" + auctionId, currentPrice);
          messagingTemplate.convertAndSend(
              "/topic/auctions/prices", new AuctionPriceUpdateDTO(auctionId, currentPrice));
        });
  }

  public void publishStatusAfterCommit(Long auctionId, String status) {
    afterCommit(
        () -> messagingTemplate.convertAndSend("/topic/auctions/" + auctionId + "/status", status));
  }

  public void publishExtendedEndTimeAfterCommit(Long auctionId, String endTime) {
    afterCommit(
        () -> {
          messagingTemplate.convertAndSend("/topic/auctions/" + auctionId + "/extended", endTime);
          publishAuctionListChanged();
        });
  }

  public void publishAuctionListChangedAfterCommit() {
    afterCommit(this::publishAuctionListChanged);
  }

  private void publishAuctionListChanged() {
    messagingTemplate.convertAndSend("/topic/auctions/changed", "RELOAD");
  }

  private void afterCommit(Runnable action) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      action.run();
      return;
    }

    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            action.run();
          }
        });
  }
}
