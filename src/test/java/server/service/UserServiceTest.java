package server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import common.enums.AuctionState;
import common.enums.BidCommitmentStatus;
import common.request.AddressRequest;
import common.request.BankRequest;
import server.model.Auction;
import server.model.Admin;
import server.model.Bidder;
import server.model.Electronics;
import server.model.Seller;
import server.repository.AuctionRepository;
import server.repository.BidCommitmentRepository;
import server.repository.ItemRepository;
import server.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class UserServiceTest {

  @InjectMocks private UserService userService;

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
    when(bidCommitmentRepository.existsByBidderIdAndStatus(1L, BidCommitmentStatus.ACTIVE))
        .thenReturn(false);
    when(itemRepository.findBySellerId(1L)).thenReturn(List.of());

    userService.deleteAccount(1L);

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
    when(bidCommitmentRepository.existsByBidderIdAndStatus(1L, BidCommitmentStatus.ACTIVE))
        .thenReturn(true);

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
    when(bidCommitmentRepository.existsByBidderIdAndStatus(1L, BidCommitmentStatus.ACTIVE))
        .thenReturn(false);
    when(itemRepository.findBySellerId(1L)).thenReturn(List.of(item));
    when(auctionRepository.findByItemId(2L)).thenReturn(List.of(auction));

    assertThrows(IllegalArgumentException.class, () -> userService.deleteAccount(1L));
  }

  @Test
  void getUser_ReturnsUserOrThrowsWhenMissing() {
    Bidder bidder = bidderWithZeroBalance();
    when(userRepository.findById(1L)).thenReturn(Optional.of(bidder));

    assertEquals(bidder, userService.getUser(1L));
    assertThrows(IllegalArgumentException.class, () -> userService.getUser(2L));
  }

  @Test
  void updateBankInfoAndAddress_SaveBidderFields() {
    Bidder bidder = bidderWithZeroBalance();
    when(userRepository.findById(1L)).thenReturn(Optional.of(bidder));
    when(userRepository.save(bidder)).thenReturn(bidder);

    assertEquals(
        bidder, userService.updateBankInfo(1L, new BankRequest("BIDV", "An Nguyen", "123")));
    assertEquals("BIDV", bidder.getBankName());
    assertEquals("An Nguyen", bidder.getAccountName());
    assertEquals("123", bidder.getBankAccount());

    assertEquals(bidder, userService.updateAddress(1L, new AddressRequest("Address")));
    assertEquals("Address", bidder.getAddress());
  }

  @Test
  void updateBankInfo_WithNonBidder_ThrowsException() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(new Admin()));

    assertThrows(
        IllegalArgumentException.class,
        () -> userService.updateBankInfo(1L, new BankRequest("BIDV", "An Nguyen", "123")));
  }

  @Test
  void deleteAccount_SellerWithoutOpenAuction_AnonymizesStore() {
    Seller seller = new Seller();
    seller.setId(1L);
    seller.setBalance(0.0);
    seller.setHeldBalance(0.0);
    seller.setStoreName("Shop");
    when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(seller));
    when(itemRepository.findBySellerId(1L)).thenReturn(List.of());

    userService.deleteAccount(1L);

    assertEquals("Tài khoản đã xóa", seller.getStoreName());
    verify(userRepository).save(seller);
  }

  private Bidder bidderWithZeroBalance() {
    Bidder bidder = new Bidder();
    bidder.setId(1L);
    bidder.setBalance(0.0);
    bidder.setHeldBalance(0.0);
    return bidder;
  }
}
