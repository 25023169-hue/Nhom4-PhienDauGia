package server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import common.enums.AuctionState;
import common.enums.BidCommitmentStatus;
import common.request.BidRequest;
import server.model.Auction;
import server.model.BidCommitment;
import server.model.Bidder;
import server.model.Item;
import server.model.SellerProductListing;
import server.repository.AuctionRepository;
import server.repository.BidCommitmentRepository;
import server.repository.BidRepository;
import server.repository.ItemRepository;
import server.repository.SellerProductListingRepository;
import server.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class BidServiceTest {

  @InjectMocks private BidService bidService;

  @Mock private AuctionRepository auctionRepository;
  @Mock private UserRepository userRepository;
  @Mock private ItemRepository itemRepository;
  @Mock private BidRepository bidRepository;
  @Mock private BidCommitmentRepository bidCommitmentRepository;
  @Mock private SellerProductListingRepository listingRepository;
  @Mock private AuctionRealtimePublisher realtimePublisher;
  @Mock private AuctionSettlementService settlementService;
  @Mock private AntiSnipingService antiSnipingService;
  @Mock private AuctionNotificationService auctionNotificationService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testPlaceBid_AuctionNotRunning_ThrowsException() {
    // Giả lập phiên đấu giá đã kết thúc
    BidRequest request = new BidRequest(1L, 100L, 50000.0);
    Auction closedAuction = new Auction();
    closedAuction.setId(1L);
    closedAuction.setStatus(AuctionState.FINISHED);

    when(auctionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(closedAuction));

    // Kiểm tra xem hệ thống có chặn lại không
    assertThrows(IllegalArgumentException.class, () -> bidService.placeBid(request));
  }

  @Test
  void testPlaceBid_AmountTooLow_ThrowsException() {
    // Giả lập sản phẩm đang có giá 100k, nhưng đặt 50k
    BidRequest request = new BidRequest(1L, 100L, 50000.0);

    Auction runningAuction = new Auction();
    runningAuction.setId(1L);
    runningAuction.setStatus(AuctionState.RUNNING);
    runningAuction.setItemId(2L);
    runningAuction.setStartTime(LocalDateTime.now().minusMinutes(5));
    runningAuction.setEndTime(LocalDateTime.now().plusHours(1));

    Item item = new Item() {};
    item.setCurrentPrice(100000.0);

    Bidder bidder = new Bidder();
    bidder.setId(100L);
    bidder.setBalance(500000.0);

    when(auctionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(runningAuction));
    when(userRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(bidder));
    when(itemRepository.findById(2L)).thenReturn(Optional.of(item));

    // Kiểm tra logic chặn đặt giá thấp
    assertThrows(IllegalArgumentException.class, () -> bidService.placeBid(request));
  }

  @Test
  void testPlaceBid_ReturningBidderOnlyHoldsPriceDifference() {
    BidRequest request = new BidRequest(1L, 100L, 500000.0);

    Auction runningAuction = new Auction();
    runningAuction.setId(1L);
    runningAuction.setStatus(AuctionState.RUNNING);
    runningAuction.setItemId(2L);
    runningAuction.setStartTime(LocalDateTime.now().minusMinutes(5));
    runningAuction.setEndTime(LocalDateTime.now().plusHours(1));

    Item item = new Item() {};
    item.setId(2L);
    item.setCurrentPrice(400000.0);

    Bidder bidder = new Bidder();
    bidder.setId(100L);
    bidder.setBalance(1000000.0);
    bidder.setHeldBalance(300000.0);
    bidder.setUsername("bidder");

    BidCommitment commitment = new BidCommitment();
    commitment.setId(10L);
    commitment.setAuctionId(1L);
    commitment.setBidderId(100L);
    commitment.setAmount(300000.0);
    commitment.setStatus(BidCommitmentStatus.ACTIVE);

    when(auctionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(runningAuction));
    when(userRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(bidder));
    when(itemRepository.findById(2L)).thenReturn(Optional.of(item));
    when(bidCommitmentRepository.findByAuctionIdAndBidderId(1L, 100L))
        .thenReturn(Optional.of(commitment));

    bidService.placeBid(request);

    assertEquals(500000.0, bidder.getHeldBalance());
    assertEquals(500000.0, commitment.getAmount());
    assertEquals(500000.0, item.getCurrentPrice());
    verify(itemRepository).saveAndFlush(item);
    verify(userRepository).save(bidder);
    verify(bidCommitmentRepository).save(commitment);
    verify(bidRepository).save(any());
    verify(realtimePublisher).publishPriceAfterCommit(1L, 500000.0);
    verify(antiSnipingService).extendIfNeeded(eq(runningAuction), any(LocalDateTime.class));
  }

  @Test
  void testPlaceBid_ReachesBuyNowPrice_ClosesAuctionImmediately() {
    BidRequest request = new BidRequest(1L, 100L, 600000.0);

    Auction runningAuction = new Auction();
    runningAuction.setId(1L);
    runningAuction.setStatus(AuctionState.RUNNING);
    runningAuction.setItemId(2L);
    runningAuction.setStartTime(LocalDateTime.now().minusMinutes(5));
    runningAuction.setEndTime(LocalDateTime.now().plusHours(1));

    Item item = new Item() {};
    item.setId(2L);
    item.setCurrentPrice(500000.0);

    Bidder bidder = new Bidder();
    bidder.setId(100L);
    bidder.setBalance(1000000.0);
    bidder.setUsername("bidder");

    SellerProductListing listing = new SellerProductListing();
    listing.setItemId(2L);
    listing.setBuyNowPrice(600000.0);

    when(auctionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(runningAuction));
    when(userRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(bidder));
    when(itemRepository.findById(2L)).thenReturn(Optional.of(item));
    when(listingRepository.findByItemId(2L)).thenReturn(Optional.of(listing));

    bidService.placeBid(request);

    verify(settlementService).closeBuyNowAuction(1L);
    verify(realtimePublisher).publishPriceAfterCommit(1L, 600000.0);
    verify(antiSnipingService, never()).extendIfNeeded(any(), any());
  }
}
