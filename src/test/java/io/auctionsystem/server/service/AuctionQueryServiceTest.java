package io.auctionsystem.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.auctionsystem.common.enums.AuctionState;
import io.auctionsystem.server.model.Auction;
import io.auctionsystem.server.model.Item;
import io.auctionsystem.server.repository.AuctionRepository;
import io.auctionsystem.server.repository.BidRepository;
import io.auctionsystem.server.repository.ItemRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class AuctionQueryServiceTest {

  @InjectMocks private AuctionQueryService queryService;

  @Mock private AuctionRepository auctionRepository;
  @Mock private ItemRepository itemRepository;
  @Mock private BidRepository bidRepository;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void getRunningAuctions_LoadsItemsInOneBatch() {
    Auction firstAuction = auction(1L, 10L);
    Auction secondAuction = auction(2L, 20L);
    Item firstItem = item(10L, "Laptop");
    Item secondItem = item(20L, "Tranh");

    when(auctionRepository.findByStatus(AuctionState.RUNNING))
        .thenReturn(List.of(firstAuction, secondAuction));
    when(itemRepository.findAllById(List.of(10L, 20L))).thenReturn(List.of(firstItem, secondItem));

    var result = queryService.getRunningAuctions();

    assertEquals(2, result.size());
    assertEquals("Laptop", result.getFirst().getName());
    assertEquals("Tranh", result.getLast().getName());
    verify(itemRepository).findAllById(List.of(10L, 20L));
  }

  private Auction auction(Long id, Long itemId) {
    Auction auction = new Auction();
    auction.setId(id);
    auction.setItemId(itemId);
    auction.setStatus(AuctionState.RUNNING);
    return auction;
  }

  private Item item(Long id, String name) {
    Item item = new Item() {};
    item.setId(id);
    item.setName(name);
    return item;
  }
}
