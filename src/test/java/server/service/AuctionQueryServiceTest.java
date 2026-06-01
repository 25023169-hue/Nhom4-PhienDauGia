package server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import common.enums.AuctionState;
import server.model.Auction;
import server.model.Item;
import server.model.Seller;
import server.repository.AuctionRepository;
import server.repository.BidRepository;
import server.repository.ItemRepository;
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

  @Test
  void getSellerRunningAuctions_FiltersOtherSellers() {
    Auction firstAuction = auction(1L, 10L);
    Auction secondAuction = auction(2L, 20L);
    Item firstItem = item(10L, "Laptop");
    Item secondItem = item(20L, "Phone");
    Seller firstSeller = new Seller();
    firstSeller.setId(1L);
    Seller secondSeller = new Seller();
    secondSeller.setId(2L);
    firstItem.setSeller(firstSeller);
    secondItem.setSeller(secondSeller);
    when(auctionRepository.findByStatus(AuctionState.RUNNING))
        .thenReturn(List.of(firstAuction, secondAuction));
    when(itemRepository.findAllById(List.of(10L, 20L))).thenReturn(List.of(firstItem, secondItem));

    assertEquals(1, queryService.getSellerRunningAuctions(1L).size());
  }

  @Test
  void getParticipatingAuctions_SkipsMissingAndFinishedAuctions() {
    Auction running = auction(1L, 10L);
    Auction finished = auction(2L, 20L);
    finished.setStatus(AuctionState.FINISHED);
    when(bidRepository.findParticipatingAuctionIdsByBidderId(9L)).thenReturn(List.of(1L, 2L, 3L));
    when(auctionRepository.findAllById(List.of(1L, 2L, 3L))).thenReturn(List.of(running, finished));
    when(itemRepository.findAllById(List.of(10L))).thenReturn(List.of(item(10L, "Laptop")));

    assertEquals(1, queryService.getParticipatingAuctions(9L).size());
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
