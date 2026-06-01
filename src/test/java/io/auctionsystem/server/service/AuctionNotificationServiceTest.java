package io.auctionsystem.server.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.auctionsystem.common.response.BidResponse;
import io.auctionsystem.server.model.Auction;
import io.auctionsystem.server.model.Item;
import io.auctionsystem.server.model.Seller;
import io.auctionsystem.server.repository.AuctionRepository;
import io.auctionsystem.server.repository.BidRepository;
import io.auctionsystem.server.repository.ItemRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class AuctionNotificationServiceTest {

  @Mock private BidRepository bidRepository;
  @Mock private AuctionRepository auctionRepository;
  @Mock private ItemRepository itemRepository;
  @Mock private NotificationService notificationService;

  private AuctionNotificationService auctionNotificationService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    auctionNotificationService =
        new AuctionNotificationService(
            bidRepository, auctionRepository, itemRepository, notificationService);
  }

  @Test
  void notifyBidAfterCommit_NotifiesBidderPreviousWinnerAndSeller() {
    BidResponse response = new BidResponse(true, "OK", 1L, 500000.0, "bidder", 11L);

    Seller seller = new Seller();
    seller.setId(20L);
    Item item = new Item() {};
    item.setSeller(seller);
    Auction auction = new Auction();
    auction.setItemId(2L);

    when(auctionRepository.findById(1L)).thenReturn(Optional.of(auction));
    when(itemRepository.findById(2L)).thenReturn(Optional.of(item));

    auctionNotificationService.notifyBidAfterCommit(response, 10L);

    verify(notificationService)
        .createNotification(
            10L, "Bạn đã đặt giá thành công mức 500.000đ cho phiên đấu giá #1", "BID_SUCCESS");
    verify(notificationService)
        .createNotification(11L, "Bạn đã bị vượt giá tại phiên sản phẩm #1!", "BID_OUTBID");
    verify(notificationService)
        .createNotification(20L, "Có lượt đặt giá mới: 500.000đ cho sản phẩm của bạn.", "NEW_BID");
  }

  @Test
  void notifyCancelledAuctionAfterCommit_WhenAuctionIsNotCancelled_DoesNotNotify() {
    Auction auction = new Auction();
    when(auctionRepository.findById(1L)).thenReturn(Optional.of(auction));

    auctionNotificationService.notifyCancelledAuctionAfterCommit(1L);

    verifyNoInteractions(notificationService);
  }
}
