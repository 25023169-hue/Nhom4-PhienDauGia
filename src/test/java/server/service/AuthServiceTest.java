package server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import common.enums.Role;
import common.request.RegisterRequest;
import common.response.AuthResponse;
import server.model.Admin;
import server.model.Bidder;
import server.model.Seller;
import server.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class AuthServiceTest {

  @InjectMocks private AuthService authService;

  @Mock private UserRepository userRepository;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testRegister_WithVietnameseUsername_ThrowsException() {
    RegisterRequest request = new RegisterRequest("nguyễn", "secret", "Nguyen", "An", Role.BIDDER);

    assertThrows(IllegalArgumentException.class, () -> authService.register(request));
  }

  @Test
  void testRegister_WithNullUsername_ThrowsException() {
    RegisterRequest request = new RegisterRequest(null, "secret", "Nguyen", "An", Role.BIDDER);

    assertThrows(IllegalArgumentException.class, () -> authService.register(request));
  }

  @Test
  void testLogin_WithVietnameseUsername_DoesNotQueryDatabase() {
    assertThrows(IllegalArgumentException.class, () -> authService.login("â", "secret"));

    verifyNoInteractions(userRepository);
  }

  @Test
  void testLogin_WithNullUsername_DoesNotQueryDatabase() {
    assertThrows(IllegalArgumentException.class, () -> authService.login(null, "secret"));

    verifyNoInteractions(userRepository);
  }

  @Test
  void testLogin_WithAdmin_ReturnsAdminRole() {
    Admin admin = new Admin();
    admin.setId(1L);
    admin.setUsername("admin");
    admin.setPassword("admin123");
    when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

    AuthResponse response = authService.login("admin", "admin123");

    assertEquals(Role.ADMIN, response.getRole());
    verify(userRepository, never()).isUserSeller(admin.getId());
  }

  @Test
  void register_WithBidderAndSeller_SavesMatchingUserTypes() {
    assertEquals(
        "Đăng ký thành công",
        authService.register(new RegisterRequest("bidder", "secret", "Nguyen", "An", Role.BIDDER)));
    verify(userRepository).save(any(Bidder.class));

    assertEquals(
        "Đăng ký thành công",
        authService.register(new RegisterRequest("seller", "secret", "Tran", "Binh", Role.SELLER)));
    verify(userRepository).save(any(Seller.class));
  }

  @Test
  void register_WithExistingUsername_ThrowsException() {
    when(userRepository.existsByUsername("user")).thenReturn(true);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            authService.register(
                new RegisterRequest("user", "secret", "Nguyen", "An", Role.BIDDER)));
  }

  @Test
  void login_WithBidder_ReturnsProfileAndBidderRole() {
    Bidder bidder = new Bidder();
    bidder.setId(2L);
    bidder.setUsername("bidder");
    bidder.setPassword("secret");
    bidder.setAddress("Address");
    bidder.setBankName("BIDV");
    bidder.setAccountName("An Nguyen");
    bidder.setBankAccount("123");
    when(userRepository.findByUsername("bidder")).thenReturn(Optional.of(bidder));

    AuthResponse response = authService.login("bidder", "secret");

    assertEquals(Role.BIDDER, response.getRole());
    assertEquals("Address", response.getAddress());
    assertEquals("BIDV", response.getBankName());
  }

  @Test
  void login_WithSeller_ReturnsStoreNameAndSellerRole() {
    Seller seller = new Seller();
    seller.setId(3L);
    seller.setUsername("seller");
    seller.setPassword("secret");
    seller.setStoreName("Shop");
    when(userRepository.findByUsername("seller")).thenReturn(Optional.of(seller));
    when(userRepository.isUserSeller(3L)).thenReturn(1);

    AuthResponse response = authService.login("seller", "secret");

    assertEquals(Role.SELLER, response.getRole());
    assertEquals("Shop", response.getStoreName());
  }

  @Test
  void login_WithWrongPassword_ThrowsException() {
    Bidder bidder = new Bidder();
    bidder.setUsername("bidder");
    bidder.setPassword("secret");
    when(userRepository.findByUsername("bidder")).thenReturn(Optional.of(bidder));

    assertThrows(IllegalArgumentException.class, () -> authService.login("bidder", "wrong"));
  }

  @Test
  void upgradeToSeller_HandlesSuccessAndFailures() {
    Bidder bidder = new Bidder();
    when(userRepository.findById(1L)).thenReturn(Optional.of(bidder));

    assertEquals("Đăng ký Kênh Người Bán thành công!", authService.upgradeToSeller(1L, "Shop"));
    verify(userRepository).upgradeToSellerNative(1L, "Shop");

    when(userRepository.isUserSeller(1L)).thenReturn(1);
    assertThrows(IllegalArgumentException.class, () -> authService.upgradeToSeller(1L, "Shop"));

    when(userRepository.findById(2L)).thenReturn(Optional.empty());
    assertThrows(IllegalArgumentException.class, () -> authService.upgradeToSeller(2L, "Shop"));

    when(userRepository.isUserSeller(1L)).thenReturn(0);
    doThrow(new RuntimeException("database")).when(userRepository).upgradeToSellerNative(1L, "Shop");
    assertThrows(IllegalStateException.class, () -> authService.upgradeToSeller(1L, "Shop"));
  }
}
