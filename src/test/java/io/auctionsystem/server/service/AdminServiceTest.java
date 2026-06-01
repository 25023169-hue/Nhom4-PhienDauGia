package io.auctionsystem.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.auctionsystem.common.enums.AuctionState;
import io.auctionsystem.server.model.Auction;
import io.auctionsystem.server.model.Electronics;
import io.auctionsystem.server.repository.AuctionRepository;
import io.auctionsystem.server.repository.ItemRepository;
import io.auctionsystem.server.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class AdminServiceTest {

  @InjectMocks private AdminService adminService;

  @Mock private UserRepository userRepository;
  @Mock private AuctionRepository auctionRepository;
  @Mock private ItemRepository itemRepository;
  @Mock private AuctionSettlementService settlementService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testFindAllAuctions_IncludesScheduledAuction() {
    Auction auction = new Auction();
    auction.setId(1L);
    auction.setItemId(2L);
    auction.setStartTime(LocalDateTime.of(2026, 6, 2, 8, 30));
    auction.setEndTime(LocalDateTime.of(2026, 6, 2, 10, 30));
    auction.setStatus(AuctionState.OPEN);

    Electronics item = new Electronics();
    item.setId(2L);
    item.setName("Laptop");
    item.setCurrentPrice(1000000.0);

    when(auctionRepository.findAll()).thenReturn(List.of(auction));
    when(itemRepository.findById(2L)).thenReturn(Optional.of(item));

    var auctions = adminService.findAllAuctions();

    assertEquals(1, auctions.size());
    assertEquals("Laptop", auctions.getFirst().getName());
    assertEquals("02/06/2026 08:30", auctions.getFirst().getStartTime());
    assertEquals("OPEN", auctions.getFirst().getStatus());
  }

  @Test
  void testDeleteAuction_OpenAuction_CancelsAuction() {
    Auction auction = new Auction();
    auction.setId(1L);
    auction.setStatus(AuctionState.OPEN);
    when(auctionRepository.findById(1L)).thenReturn(Optional.of(auction));
    when(settlementService.cancelAuction(1L)).thenReturn(true);

    adminService.deleteAuction(1L);

    verify(settlementService).cancelAuction(1L);
  }

  @Test
  void testFindAllAuctions_DoesNotIncludeCancelledAuction() {
    Auction auction = new Auction();
    auction.setId(1L);
    auction.setItemId(2L);
    auction.setStatus(AuctionState.CANCELLED);
    when(auctionRepository.findAll()).thenReturn(List.of(auction));

    var auctions = adminService.findAllAuctions();

    assertEquals(0, auctions.size());
    verify(itemRepository, never()).findById(2L);
  }

  @Test
  void testDeleteAuction_RunningAuction_CancelsAuction() {
    Auction auction = new Auction();
    auction.setId(1L);
    auction.setStatus(AuctionState.RUNNING);
    when(auctionRepository.findById(1L)).thenReturn(Optional.of(auction));
    when(settlementService.cancelAuction(1L)).thenReturn(true);

    adminService.deleteAuction(1L);

    verify(settlementService).cancelAuction(1L);
  }

  @Test
  void testDeleteAuction_FinishedAuction_ThrowsException() {
    Auction auction = new Auction();
    auction.setId(1L);
    auction.setStatus(AuctionState.FINISHED);
    when(auctionRepository.findById(1L)).thenReturn(Optional.of(auction));

    assertThrows(IllegalArgumentException.class, () -> adminService.deleteAuction(1L));

    verify(settlementService, never()).cancelAuction(1L);
  }
}
