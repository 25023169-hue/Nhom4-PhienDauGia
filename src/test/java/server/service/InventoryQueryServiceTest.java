package server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import common.enums.AuctionState;
import server.model.Auction;
import server.model.Electronics;
import server.repository.ItemRepository;
import server.repository.WonAuctionRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;

class InventoryQueryServiceTest {

  @InjectMocks private InventoryQueryService inventoryQueryService;
  @Mock private WonAuctionRepository wonAuctionRepository;
  @Mock private ItemRepository itemRepository;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void getWonItems_MapsAvailableItemsAndSkipsMissingItems() {
    Auction available = auction(1L, 10L, LocalDateTime.of(2026, 6, 2, 8, 30));
    Auction missing = auction(2L, 20L, null);
    Electronics item = new Electronics();
    item.setId(10L);
    item.setName("Laptop");

    when(wonAuctionRepository.findByWinnerIdAndStatus(9L, AuctionState.FINISHED))
        .thenReturn(List.of(available, missing));
    when(itemRepository.findAllById(List.of(10L, 20L))).thenReturn(List.of(item));

    var result = inventoryQueryService.getWonItems(9L);

    assertEquals(1, result.size());
    assertEquals("Laptop", result.getFirst().getName());
    assertEquals("02/06/2026 08:30", result.getFirst().getEndTime());
    assertEquals(AuctionState.FINISHED, result.getFirst().getStatus());
  }

  private Auction auction(Long id, Long itemId, LocalDateTime endTime) {
    Auction auction = new Auction();
    auction.setId(id);
    auction.setItemId(itemId);
    auction.setFinalPrice(100.0);
    auction.setEndTime(endTime);
    return auction;
  }
}
