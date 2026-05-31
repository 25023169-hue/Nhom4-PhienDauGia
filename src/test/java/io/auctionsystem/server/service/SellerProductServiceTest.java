package io.auctionsystem.server.service;

import io.auctionsystem.common.enums.AuctionState;
import io.auctionsystem.common.enums.ItemType;
import io.auctionsystem.common.request.SellerProductRequest;
import io.auctionsystem.server.model.Auction;
import io.auctionsystem.server.model.Electronics;
import io.auctionsystem.server.model.Item;
import io.auctionsystem.server.model.Seller;
import io.auctionsystem.server.model.SellerProductListing;
import io.auctionsystem.server.repository.AuctionRepository;
import io.auctionsystem.server.repository.ItemRepository;
import io.auctionsystem.server.repository.SellerProductListingRepository;
import io.auctionsystem.server.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SellerProductServiceTest {

    @InjectMocks
    private SellerProductService sellerProductService;

    @Mock private ItemRepository itemRepository;
    @Mock private AuctionRepository auctionRepository;
    @Mock private UserRepository userRepository;
    @Mock private SellerProductListingRepository listingRepository;
    @Mock private AuctionRealtimePublisher realtimePublisher;
    @Mock private AuctionSettlementService settlementService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSaveProductAndPrepareAuction_CreatesScheduledAuction() {
        Seller seller = new Seller();
        seller.setId(10L);

        LocalDateTime startTime = LocalDateTime.now().plusHours(1);
        LocalDateTime endTime = startTime.plusHours(2);

        SellerProductRequest request = new SellerProductRequest();
        request.setSellerId(10L);
        request.setName("Tranh đấu giá");
        request.setStartingPrice(100000.0);
        request.setItemType(ItemType.ART);
        request.setStartTime(startTime);
        request.setEndTime(endTime);

        when(userRepository.findById(10L)).thenReturn(Optional.of(seller));
        when(userRepository.isUserSeller(10L)).thenReturn(1);
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> {
            Item item = invocation.getArgument(0);
            item.setId(20L);
            return item;
        });
        when(listingRepository.save(any(SellerProductListing.class))).thenAnswer(invocation -> invocation.getArgument(0));

        sellerProductService.saveProductAndPrepareAuction(request);

        ArgumentCaptor<Auction> auctionCaptor = ArgumentCaptor.forClass(Auction.class);
        verify(auctionRepository).save(auctionCaptor.capture());
        Auction auction = auctionCaptor.getValue();

        assertEquals(20L, auction.getItemId());
        assertEquals(startTime, auction.getStartTime());
        assertEquals(endTime, auction.getEndTime());
        assertEquals(100000.0, auction.getFinalPrice());
        assertEquals(AuctionState.OPEN, auction.getStatus());
        verify(realtimePublisher).publishAuctionListChangedAfterCommit();
    }

    @Test
    void testSaveProductAndPrepareAuction_StartTimeInPast_ThrowsException() {
        SellerProductRequest request = new SellerProductRequest();
        request.setSellerId(10L);
        request.setName("Tranh đấu giá");
        request.setStartingPrice(100000.0);
        request.setItemType(ItemType.ART);
        request.setStartTime(LocalDateTime.now().minusMinutes(1));
        request.setEndTime(LocalDateTime.now().plusHours(1));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> sellerProductService.saveProductAndPrepareAuction(request)
        );

        assertEquals("Thời gian bắt đầu không được ở quá khứ", exception.getMessage());
    }

    @Test
    void testGetSellerProducts_IncludesAuctionWithoutListing() {
        Seller seller = new Seller();
        seller.setId(10L);

        Electronics item = new Electronics();
        item.setId(20L);
        item.setSeller(seller);
        item.setName("Sản phẩm mẫu");
        item.setStartingPrice(100000.0);
        item.setCurrentPrice(120000.0);

        Auction auction = new Auction();
        auction.setItemId(20L);
        auction.setStartTime(LocalDateTime.now().minusHours(1));
        auction.setEndTime(LocalDateTime.now().plusHours(1));
        auction.setStatus(AuctionState.RUNNING);

        when(listingRepository.findBySellerIdOrderByIdDesc(10L)).thenReturn(List.of());
        when(itemRepository.findBySellerId(10L)).thenReturn(List.of(item));
        when(auctionRepository.findByItemId(20L)).thenReturn(List.of(auction));

        var products = sellerProductService.getSellerProducts(10L, null, null);

        assertEquals(1, products.size());
        assertEquals("Sản phẩm mẫu", products.getFirst().getItemName());
        assertEquals(ItemType.ELECTRONICS, products.getFirst().getItemType());
        assertEquals(AuctionState.RUNNING, products.getFirst().getStatus());
    }

    @Test
    void testGetSellerProducts_ShowsSoldPriceForFinishedListing() {
        SellerProductListing listing = new SellerProductListing();
        listing.setItemId(20L);
        listing.setSellerId(10L);
        listing.setStatus(AuctionState.FINISHED);

        Electronics item = new Electronics();
        item.setId(20L);
        item.setName("Sản phẩm đã bán");
        item.setStartingPrice(100000.0);
        item.setCurrentPrice(150000.0);

        Auction auction = new Auction();
        auction.setItemId(20L);
        auction.setWinnerId(30L);
        auction.setFinalPrice(150000.0);
        auction.setStatus(AuctionState.FINISHED);

        when(listingRepository.findBySellerIdOrderByIdDesc(10L)).thenReturn(List.of(listing));
        when(itemRepository.findById(20L)).thenReturn(Optional.of(item));
        when(itemRepository.findBySellerId(10L)).thenReturn(List.of(item));
        when(auctionRepository.findTopByItemIdOrderByIdDesc(20L)).thenReturn(Optional.of(auction));

        var products = sellerProductService.getSellerProducts(10L, null, null);

        assertEquals(1, products.size());
        assertEquals(150000.0, products.getFirst().getSoldPrice());
    }

    @Test
    void testGetSellerProducts_ShowsSoldPriceForFinishedAuctionWithoutListing() {
        Electronics item = new Electronics();
        item.setId(20L);
        item.setName("Sản phẩm dữ liệu cũ đã bán");
        item.setStartingPrice(100000.0);
        item.setCurrentPrice(150000.0);

        Auction auction = new Auction();
        auction.setItemId(20L);
        auction.setWinnerId(30L);
        auction.setFinalPrice(150000.0);
        auction.setStatus(AuctionState.FINISHED);

        when(listingRepository.findBySellerIdOrderByIdDesc(10L)).thenReturn(List.of());
        when(itemRepository.findBySellerId(10L)).thenReturn(List.of(item));
        when(auctionRepository.findByItemId(20L)).thenReturn(List.of(auction));

        var products = sellerProductService.getSellerProducts(10L, null, null);

        assertEquals(1, products.size());
        assertEquals(150000.0, products.getFirst().getSoldPrice());
    }

    @Test
    void testUpdateOpenProduct_UpdatesItemAuctionAndListing() {
        Seller seller = new Seller();
        seller.setId(10L);

        Electronics item = new Electronics();
        item.setId(20L);
        item.setSeller(seller);
        item.setName("Tên cũ");

        Auction auction = new Auction();
        auction.setItemId(20L);
        auction.setStatus(AuctionState.OPEN);

        SellerProductListing listing = new SellerProductListing();
        listing.setItemId(20L);
        listing.setSellerId(10L);
        listing.setItemType(ItemType.ELECTRONICS);
        listing.setStatus(AuctionState.OPEN);

        LocalDateTime startTime = LocalDateTime.now().plusHours(1);
        SellerProductRequest request = validRequest(10L, ItemType.ELECTRONICS, startTime);
        request.setName("Tên mới");
        request.setStartingPrice(250000.0);
        request.setBrand("Brand");

        when(itemRepository.findById(20L)).thenReturn(Optional.of(item));
        when(auctionRepository.findTopByItemIdOrderByIdDesc(20L)).thenReturn(Optional.of(auction));
        when(listingRepository.findByItemId(20L)).thenReturn(Optional.of(listing));
        when(listingRepository.save(listing)).thenReturn(listing);

        var result = sellerProductService.updateOpenProduct(20L, request);

        assertEquals("Tên mới", item.getName());
        assertEquals(250000.0, item.getStartingPrice());
        assertEquals(250000.0, auction.getFinalPrice());
        assertEquals("Brand", result.getBrand());
        verify(realtimePublisher).publishAuctionListChangedAfterCommit();
    }

    @Test
    void testHideProduct_RunningAuction_CancelsAuctionAndHidesListing() {
        Seller seller = new Seller();
        seller.setId(10L);

        Electronics item = new Electronics();
        item.setId(20L);
        item.setSeller(seller);

        Auction auction = new Auction();
        auction.setId(30L);
        auction.setItemId(20L);
        auction.setStatus(AuctionState.RUNNING);

        SellerProductListing listing = new SellerProductListing();
        listing.setItemId(20L);
        listing.setSellerId(10L);
        listing.setStatus(AuctionState.RUNNING);

        when(itemRepository.findById(20L)).thenReturn(Optional.of(item));
        when(auctionRepository.findTopByItemIdOrderByIdDesc(20L)).thenReturn(Optional.of(auction));
        when(listingRepository.findByItemId(20L)).thenReturn(Optional.of(listing));

        sellerProductService.hideProduct(20L, 10L);

        assertTrue(listing.isHidden());
        verify(settlementService).cancelAuction(30L);
        verify(listingRepository).save(listing);
        verify(realtimePublisher).publishAuctionListChangedAfterCommit();
    }

    @Test
    void testGetSellerProducts_DoesNotShowHiddenListing() {
        SellerProductListing listing = new SellerProductListing();
        listing.setItemId(20L);
        listing.setSellerId(10L);
        listing.setHidden(true);

        when(listingRepository.findBySellerIdOrderByIdDesc(10L)).thenReturn(List.of(listing));
        when(itemRepository.findBySellerId(10L)).thenReturn(List.of());

        var products = sellerProductService.getSellerProducts(10L, null, null);

        assertTrue(products.isEmpty());
    }

    private SellerProductRequest validRequest(Long sellerId, ItemType itemType, LocalDateTime startTime) {
        SellerProductRequest request = new SellerProductRequest();
        request.setSellerId(sellerId);
        request.setName("Sản phẩm");
        request.setStartingPrice(100000.0);
        request.setItemType(itemType);
        request.setStartTime(startTime);
        request.setEndTime(startTime.plusHours(1));
        return request;
    }
}
