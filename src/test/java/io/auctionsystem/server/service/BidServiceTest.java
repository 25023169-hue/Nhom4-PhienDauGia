package io.auctionsystem.server.service;

import io.auctionsystem.common.enums.AuctionState;
import io.auctionsystem.common.request.BidRequest;
import io.auctionsystem.server.model.Auction;
import io.auctionsystem.server.model.Bidder;
import io.auctionsystem.server.model.Item;
import io.auctionsystem.server.repository.AuctionRepository;
import io.auctionsystem.server.repository.BidRepository;
import io.auctionsystem.server.repository.ItemRepository;
import io.auctionsystem.server.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class BidServiceTest {

    @InjectMocks
    private BidService bidService;

    @Mock private AuctionRepository auctionRepository;
    @Mock private UserRepository userRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private BidRepository bidRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testPlaceBid_AuctionNotRunning_ThrowsException() {
        // Giả lập phiên đấu giá đã kết thúc
        BidRequest request = new BidRequest(1L, 100L, 50000.0);
        Auction closedAuction = new Auction();
        closedAuction.setStatus(AuctionState.FINISHED);

        when(auctionRepository.findById(1L)).thenReturn(Optional.of(closedAuction));

        // Kiểm tra xem hệ thống có chặn lại không
        assertThrows(IllegalArgumentException.class, () -> bidService.placeBid(request));
    }

    @Test
    void testPlaceBid_AmountTooLow_ThrowsException() {
        // Giả lập sản phẩm đang có giá 100k, nhưng đặt 50k
        BidRequest request = new BidRequest(1L, 100L, 50000.0);

        Auction runningAuction = new Auction();
        runningAuction.setStatus(AuctionState.RUNNING);
        runningAuction.setItemId(2L);

        Item item = new Item() {};
        item.setCurrentPrice(100000.0);

        Bidder bidder = new Bidder();
        bidder.setId(100L);
        bidder.setBalance(500000.0);

        when(auctionRepository.findById(1L)).thenReturn(Optional.of(runningAuction));
        when(userRepository.findById(100L)).thenReturn(Optional.of(bidder));
        when(itemRepository.findById(2L)).thenReturn(Optional.of(item));

        // Kiểm tra logic chặn đặt giá thấp
        assertThrows(IllegalArgumentException.class, () -> bidService.placeBid(request));
    }
}