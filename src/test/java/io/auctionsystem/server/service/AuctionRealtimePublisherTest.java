package io.auctionsystem.server.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.auctionsystem.common.dto.AuctionPriceUpdateDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class AuctionRealtimePublisherTest {

  @InjectMocks private AuctionRealtimePublisher realtimePublisher;

  @Mock private SimpMessagingTemplate messagingTemplate;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testPublishPriceAfterCommit_OutsideTransactionPublishesRoomAndListPrice() {
    realtimePublisher.publishPriceAfterCommit(1L, 500000.0);

    verify(messagingTemplate).convertAndSend("/topic/bids/1", 500000.0);
    verify(messagingTemplate)
        .convertAndSend(eq("/topic/auctions/prices"), eq(new AuctionPriceUpdateDTO(1L, 500000.0)));
  }

  @Test
  void testPublishExtendedEndTimeAfterCommit_AlsoReloadsAuctionList() {
    realtimePublisher.publishExtendedEndTimeAfterCommit(1L, "2026-05-31T12:00:00");

    verify(messagingTemplate).convertAndSend("/topic/auctions/1/extended", "2026-05-31T12:00:00");
    verify(messagingTemplate).convertAndSend("/topic/auctions/changed", "RELOAD");
  }

  @Test
  void testPublishPriceAfterCommit_InsideTransactionWaitsUntilCommit() {
    TransactionSynchronizationManager.initSynchronization();
    try {
      realtimePublisher.publishPriceAfterCommit(1L, 500000.0);

      verifyNoInteractions(messagingTemplate);
      for (TransactionSynchronization synchronization :
          TransactionSynchronizationManager.getSynchronizations()) {
        synchronization.afterCommit();
      }

      verify(messagingTemplate).convertAndSend("/topic/bids/1", 500000.0);
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }
}
