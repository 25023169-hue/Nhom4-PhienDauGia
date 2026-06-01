package io.auctionsystem.server.config;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.auctionsystem.common.request.BidRequest;
import io.auctionsystem.common.response.BidResponse;
import io.auctionsystem.server.model.Auction;
import io.auctionsystem.server.model.Item;
import io.auctionsystem.server.model.Seller;
import io.auctionsystem.server.repository.AuctionRepository;
import io.auctionsystem.server.repository.BidRepository;
import io.auctionsystem.server.repository.ItemRepository;
import io.auctionsystem.server.service.NotificationService;
import java.util.Optional;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class CompleteNotificationAspectTest {

  @Mock private BidRepository bidRepository;
  @Mock private AuctionRepository auctionRepository;
  @Mock private ItemRepository itemRepository;
  @Mock private NotificationService notificationService;
  @Mock private ProceedingJoinPoint joinPoint;

  private CompleteNotificationAspect aspect;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    aspect =
        new CompleteNotificationAspect(
            bidRepository, auctionRepository, itemRepository, notificationService);
  }

  @Test
  void handleBidNotifications_NotifiesBidderPreviousWinnerAndSeller() throws Throwable {
    BidRequest request = new BidRequest(1L, 10L, 500000.0);
    BidResponse response = new BidResponse(true, "OK", 1L, 500000.0, "bidder", 11L);

    Seller seller = new Seller();
    seller.setId(20L);
    Item item = new Item() {};
    item.setSeller(seller);
    Auction auction = new Auction();
    auction.setItemId(2L);

    when(joinPoint.proceed()).thenReturn(response);
    when(auctionRepository.findById(1L)).thenReturn(Optional.of(auction));
    when(itemRepository.findById(2L)).thenReturn(Optional.of(item));

    aspect.handleBidNotifications(joinPoint, request);

    verify(notificationService)
        .createNotification(
            10L, "Bạn đã đặt giá thành công mức 500.000đ cho phiên đấu giá #1", "BID_SUCCESS");
    verify(notificationService)
        .createNotification(11L, "Bạn đã bị vượt giá tại phiên sản phẩm #1!", "BID_OUTBID");
    verify(notificationService)
        .createNotification(20L, "Có lượt đặt giá mới: 500.000đ cho sản phẩm của bạn.", "NEW_BID");
  }

  @Test
  void handleCancelledAuction_WhenCancellationFails_DoesNotNotify() {
    aspect.handleCancelledAuction(1L, false);

    verifyNoInteractions(notificationService);
  }
}
