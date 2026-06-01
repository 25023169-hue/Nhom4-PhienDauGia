package server.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import common.enums.AuctionState;
import common.enums.NotificationType;
import common.response.BidResponse;
import server.model.Auction;
import server.model.Bid;
import server.model.Item;
import server.model.Seller;
import server.repository.AuctionRepository;
import server.repository.BidRepository;
import server.repository.ItemRepository;
import java.util.List;
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
            11L,
            "Bạn đã bị vượt giá tại phiên sản phẩm #1!",
            NotificationType.BID_OUTBID);
    verify(notificationService)
        .createNotification(
            20L,
            "Có lượt đặt giá mới: 500.000đ cho sản phẩm của bạn.",
            NotificationType.NEW_BID);
    verify(notificationService, org.mockito.Mockito.never())
        .createNotification(
            org.mockito.ArgumentMatchers.eq(10L),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any(NotificationType.class));
  }

  @Test
  void notifyCancelledAuctionAfterCommit_WhenAuctionIsNotCancelled_DoesNotNotify() {
    Auction auction = new Auction();
    when(auctionRepository.findById(1L)).thenReturn(Optional.of(auction));

    auctionNotificationService.notifyCancelledAuctionAfterCommit(1L);

    verifyNoInteractions(notificationService);
  }

  @Test
  void notifyFinishedAuctionAfterCommit_NotifiesWinnerLoserAndSellerOnlyOnce() {
    Seller seller = new Seller();
    seller.setId(20L);
    Item item = new Item() {};
    item.setSeller(seller);
    Auction auction = new Auction();
    auction.setId(1L);
    auction.setItemId(2L);
    auction.setWinnerId(10L);
    auction.setFinalPrice(500000.0);
    auction.setStatus(AuctionState.FINISHED);
    when(auctionRepository.findById(1L)).thenReturn(Optional.of(auction));
    when(itemRepository.findById(2L)).thenReturn(Optional.of(item));
    when(bidRepository.findByAuctionId(1L)).thenReturn(List.of(bid(10L), bid(11L)));

    auctionNotificationService.notifyFinishedAuctionAfterCommit(1L);
    auctionNotificationService.notifyFinishedAuctionAfterCommit(1L);

    verify(notificationService)
        .createNotification(
            10L, "Tuyệt vời! Bạn đã thắng phiên đấu giá #1!", NotificationType.BID_WON);
    verify(notificationService)
        .createNotification(
            11L, "Phiên #1 đã kết thúc. Bạn không trúng giải.", NotificationType.BID_LOST);
    verify(notificationService)
        .createNotification(
            20L,
            "Chúc mừng! Sản phẩm của bạn đã bán thành công với giá 500.000đ.",
            NotificationType.AUCTION_SOLD);
  }

  @Test
  void notifyCancelledAuctionAfterCommit_NotifiesSellerAndDistinctBidders() {
    Seller seller = new Seller();
    seller.setId(20L);
    Item item = new Item() {};
    item.setSeller(seller);
    Auction auction = new Auction();
    auction.setItemId(2L);
    auction.setStatus(AuctionState.CANCELLED);
    when(auctionRepository.findById(1L)).thenReturn(Optional.of(auction));
    when(itemRepository.findById(2L)).thenReturn(Optional.of(item));
    when(bidRepository.findByAuctionId(1L)).thenReturn(List.of(bid(10L), bid(10L)));

    auctionNotificationService.notifyCancelledAuctionAfterCommit(1L);

    verify(notificationService)
        .createNotification(
            20L,
            "Bạn đã chủ động hủy phiên đấu giá của chính mình.",
            NotificationType.AUCTION_CANCELED);
    verify(notificationService)
        .createNotification(
            10L, "Phiên đấu giá #1 đã bị hủy.", NotificationType.AUCTION_CANCELED);
  }

  @Test
  void notifyExpiredAuctionAfterCommit_NotifiesSellerWithoutBuyer() {
    Seller seller = new Seller();
    seller.setId(20L);
    Item item = new Item() {};
    item.setSeller(seller);
    Auction auction = new Auction();
    auction.setItemId(2L);
    auction.setStatus(AuctionState.CANCELLED);
    when(auctionRepository.findById(1L)).thenReturn(Optional.of(auction));
    when(itemRepository.findById(2L)).thenReturn(Optional.of(item));

    auctionNotificationService.notifyExpiredAuctionAfterCommit(1L);

    verify(notificationService)
        .createNotification(
            20L,
            "Phiên đấu giá đã kết thúc nhưng không có ai mua (Ế).",
            NotificationType.AUCTION_EXPIRED);
  }

  @Test
  void afterPropertiesSet_RemembersAlreadyTerminalAuctions() {
    Auction auction = new Auction();
    auction.setId(1L);
    auction.setStatus(AuctionState.FINISHED);
    when(auctionRepository.findAll()).thenReturn(List.of(auction));

    auctionNotificationService.afterPropertiesSet();
    auctionNotificationService.notifyFinishedAuctionAfterCommit(1L);

    verifyNoInteractions(notificationService);
  }

  private Bid bid(Long bidderId) {
    Bid bid = new Bid();
    bid.setBidderId(bidderId);
    return bid;
  }
}
