package io.auctionsystem.server.service;

import io.auctionsystem.common.enums.AuctionState;
import io.auctionsystem.common.enums.BidCommitmentStatus;
import io.auctionsystem.server.model.Auction;
import io.auctionsystem.server.model.Bidder;
import io.auctionsystem.server.model.Electronics;
import io.auctionsystem.server.model.Seller;
import io.auctionsystem.server.repository.AuctionRepository;
import io.auctionsystem.server.repository.BidCommitmentRepository;
import io.auctionsystem.server.repository.ItemRepository;
import io.auctionsystem.server.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock private UserRepository userRepository;
    @Mock private BidCommitmentRepository bidCommitmentRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private AuctionRepository auctionRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testDeleteAccount_AnonymizesUser() {
        Bidder user = bidderWithZeroBalance();
        user.setUsername("bidder");
        user.setPassword("secret");
        user.setFirstname("An");
        user.setLastname("Nguyen");
        user.setBankName("Bank");
        user.setAccountName("An Nguyen");
        user.setBankAccount("123");
        user.setAddress("Address");

        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(bidCommitmentRepository.existsByBidderIdAndStatus(1L, BidCommitmentStatus.ACTIVE)).thenReturn(false);
        when(itemRepository.findBySellerId(1L)).thenReturn(List.of());

        userService.deleteAccount(1L);

        assertFalse(user.isActive());
        assertTrue(user.getUsername().startsWith("deleted_1_"));
        assertNull(user.getBankName());
        assertNull(user.getAccountName());
        assertNull(user.getBankAccount());
        assertNull(user.getAddress());
        verify(userRepository).save(user);
    }

    @Test
    void testDeleteAccount_WithBalance_ThrowsException() {
        Bidder user = bidderWithZeroBalance();
        user.setBalance(1000.0);
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class, () -> userService.deleteAccount(1L));
    }

    @Test
    void testDeleteAccount_WithActiveCommitment_ThrowsException() {
        Bidder user = bidderWithZeroBalance();
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(bidCommitmentRepository.existsByBidderIdAndStatus(1L, BidCommitmentStatus.ACTIVE)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> userService.deleteAccount(1L));
    }

    @Test
    void testDeleteAccount_SellerWithRunningAuction_ThrowsException() {
        Seller seller = new Seller();
        seller.setId(1L);

        Electronics item = new Electronics();
        item.setId(2L);
        item.setSeller(seller);

        Auction auction = new Auction();
        auction.setStatus(AuctionState.RUNNING);

        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(seller));
        when(bidCommitmentRepository.existsByBidderIdAndStatus(1L, BidCommitmentStatus.ACTIVE)).thenReturn(false);
        when(itemRepository.findBySellerId(1L)).thenReturn(List.of(item));
        when(auctionRepository.findByItemId(2L)).thenReturn(List.of(auction));

        assertThrows(IllegalArgumentException.class, () -> userService.deleteAccount(1L));
    }

    private Bidder bidderWithZeroBalance() {
        Bidder bidder = new Bidder();
        bidder.setId(1L);
        bidder.setBalance(0.0);
        bidder.setHeldBalance(0.0);
        return bidder;
    }
}
