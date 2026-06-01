package io.auctionsystem.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.auctionsystem.common.enums.AuctionState;
import io.auctionsystem.common.enums.BidCommitmentStatus;
import io.auctionsystem.server.model.Auction;
import io.auctionsystem.server.model.BidCommitment;
import io.auctionsystem.server.model.Bidder;
import io.auctionsystem.server.model.Item;
import io.auctionsystem.server.model.Seller;
import io.auctionsystem.server.model.SellerProductListing;
import io.auctionsystem.server.repository.AuctionRepository;
import io.auctionsystem.server.repository.BidCommitmentRepository;
import io.auctionsystem.server.repository.ItemRepository;
import io.auctionsystem.server.repository.SellerProductListingRepository;
import io.auctionsystem.server.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class AuctionSettlementServiceTest {

  @InjectMocks private AuctionSettlementService settlementService;

  @Mock private AuctionRepository auctionRepository;
  @Mock private BidCommitmentRepository bidCommitmentRepository;
  @Mock private UserRepository userRepository;
  @Mock private ItemRepository itemRepository;
  @Mock private SellerProductListingRepository listingRepository;
  @Mock private TransactionService transactionService;
  @Mock private AuctionRealtimePublisher realtimePublisher;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testCloseExpiredAuction_SettlesWinnerSellerAndLoser() {
    LocalDateTime now = LocalDateTime.now();

    Auction auction = new Auction();
    auction.setId(1L);
    auction.setItemId(2L);
    auction.setWinnerId(10L);
    auction.setFinalPrice(500000.0);
    auction.setStatus(AuctionState.RUNNING);
    auction.setEndTime(now.minusSeconds(1));

    Seller seller = new Seller();
    seller.setId(20L);
    seller.setBalance(100000.0);

    Item item = new Item() {};
    item.setId(2L);
    item.setName("Sản phẩm");
    item.setSeller(seller);

    Bidder winner = new Bidder();
    winner.setId(10L);
    winner.setBalance(1000000.0);
    winner.setHeldBalance(500000.0);

    Bidder loser = new Bidder();
    loser.setId(30L);
    loser.setBalance(900000.0);
    loser.setHeldBalance(400000.0);

    BidCommitment winnerCommitment = commitment(1L, 10L, 500000.0);
    BidCommitment loserCommitment = commitment(1L, 30L, 400000.0);

    when(auctionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(auction));
    when(itemRepository.findById(2L)).thenReturn(Optional.of(item));
    when(bidCommitmentRepository.findByAuctionIdAndStatusOrderByBidderIdAsc(
            1L, BidCommitmentStatus.ACTIVE))
        .thenReturn(List.of(winnerCommitment, loserCommitment));
    when(userRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(winner));
    when(userRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(seller));
    when(userRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(loser));
    when(listingRepository.findByItemId(2L)).thenReturn(Optional.empty());

    assertTrue(settlementService.closeExpiredAuction(1L, now));

    assertEquals(AuctionState.FINISHED, auction.getStatus());
    assertEquals(500000.0, winner.getBalance());
    assertEquals(0.0, winner.getHeldBalance());
    assertEquals(600000.0, seller.getBalance());
    assertEquals(900000.0, loser.getBalance());
    assertEquals(0.0, loser.getHeldBalance());
    assertEquals(BidCommitmentStatus.PAID, winnerCommitment.getStatus());
    assertEquals(BidCommitmentStatus.RELEASED, loserCommitment.getStatus());

    verify(transactionService)
        .saveTransaction(
            eq(10L), eq(0.0), eq(500000.0), eq(500000.0), eq("Thanh toán đấu giá"), any());
    verify(transactionService)
        .saveTransaction(eq(20L), eq(500000.0), eq(0.0), eq(600000.0), eq("Thu nhập"), any());
    verify(realtimePublisher).publishStatusAfterCommit(1L, "CLOSED");
    verify(realtimePublisher).publishAuctionListChangedAfterCommit();
  }

  @Test
  void testCloseBuyNowAuction_ClosesRunningAuctionImmediately() {
    Auction auction = new Auction();
    auction.setId(1L);
    auction.setItemId(2L);
    auction.setStatus(AuctionState.RUNNING);
    auction.setEndTime(LocalDateTime.now().plusHours(1));

    Item item = new Item() {};
    item.setId(2L);

    when(auctionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(auction));
    when(itemRepository.findById(2L)).thenReturn(Optional.of(item));
    when(bidCommitmentRepository.findByAuctionIdAndStatusOrderByBidderIdAsc(
            1L, BidCommitmentStatus.ACTIVE))
        .thenReturn(List.of());
    when(listingRepository.findByItemId(2L)).thenReturn(Optional.empty());

    assertTrue(settlementService.closeBuyNowAuction(1L));

    assertEquals(AuctionState.CANCELLED, auction.getStatus());
    verify(realtimePublisher).publishStatusAfterCommit(1L, "CLOSED");
    verify(realtimePublisher).publishAuctionListChangedAfterCommit();
  }

  @Test
  void testCloseExpiredAuction_WithoutWinner_CancelsAuctionAndListing() {
    LocalDateTime now = LocalDateTime.now();

    Auction auction = new Auction();
    auction.setId(1L);
    auction.setItemId(2L);
    auction.setStatus(AuctionState.RUNNING);
    auction.setEndTime(now.minusSeconds(1));

    Item item = new Item() {};
    item.setId(2L);

    SellerProductListing listing = new SellerProductListing();
    listing.setStatus(AuctionState.RUNNING);

    when(auctionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(auction));
    when(itemRepository.findById(2L)).thenReturn(Optional.of(item));
    when(bidCommitmentRepository.findByAuctionIdAndStatusOrderByBidderIdAsc(
            1L, BidCommitmentStatus.ACTIVE))
        .thenReturn(List.of());
    when(listingRepository.findByItemId(2L)).thenReturn(Optional.of(listing));

    assertTrue(settlementService.closeExpiredAuction(1L, now));

    assertEquals(AuctionState.CANCELLED, auction.getStatus());
    assertEquals(AuctionState.CANCELLED, listing.getStatus());
    verify(realtimePublisher).publishStatusAfterCommit(1L, "CLOSED");
    verify(realtimePublisher).publishAuctionListChangedAfterCommit();
  }

  @Test
  void testCloseExpiredAuction_OpenAuctionMissedWhileServerWasOffline_CancelsAuction() {
    LocalDateTime now = LocalDateTime.now();

    Auction auction = new Auction();
    auction.setId(1L);
    auction.setItemId(2L);
    auction.setStatus(AuctionState.OPEN);
    auction.setEndTime(now.minusSeconds(1));

    Item item = new Item() {};
    item.setId(2L);

    when(auctionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(auction));
    when(itemRepository.findById(2L)).thenReturn(Optional.of(item));
    when(bidCommitmentRepository.findByAuctionIdAndStatusOrderByBidderIdAsc(
            1L, BidCommitmentStatus.ACTIVE))
        .thenReturn(List.of());
    when(listingRepository.findByItemId(2L)).thenReturn(Optional.empty());

    assertTrue(settlementService.closeExpiredAuction(1L, now));

    assertEquals(AuctionState.CANCELLED, auction.getStatus());
  }

  @Test
  void testCancelAuction_RunningAuction_ReleasesHeldBalance() {
    Auction auction = new Auction();
    auction.setId(1L);
    auction.setItemId(2L);
    auction.setWinnerId(10L);
    auction.setStatus(AuctionState.RUNNING);

    Item item = new Item() {};
    item.setId(2L);

    Bidder bidder = new Bidder();
    bidder.setId(10L);
    bidder.setBalance(1000000.0);
    bidder.setHeldBalance(500000.0);

    BidCommitment commitment = commitment(1L, 10L, 500000.0);

    when(auctionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(auction));
    when(itemRepository.findById(2L)).thenReturn(Optional.of(item));
    when(bidCommitmentRepository.findByAuctionIdAndStatusOrderByBidderIdAsc(
            1L, BidCommitmentStatus.ACTIVE))
        .thenReturn(List.of(commitment));
    when(userRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(bidder));
    when(listingRepository.findByItemId(2L)).thenReturn(Optional.empty());

    assertTrue(settlementService.cancelAuction(1L));

    assertEquals(AuctionState.CANCELLED, auction.getStatus());
    assertEquals(1000000.0, bidder.getBalance());
    assertEquals(0.0, bidder.getHeldBalance());
    assertEquals(BidCommitmentStatus.RELEASED, commitment.getStatus());
    verify(realtimePublisher).publishStatusAfterCommit(1L, "CLOSED");
    verify(realtimePublisher).publishAuctionListChangedAfterCommit();
  }

  private BidCommitment commitment(Long auctionId, Long bidderId, Double amount) {
    BidCommitment commitment = new BidCommitment();
    commitment.setAuctionId(auctionId);
    commitment.setBidderId(bidderId);
    commitment.setAmount(amount);
    commitment.setStatus(BidCommitmentStatus.ACTIVE);
    return commitment;
  }
}
