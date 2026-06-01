package server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import common.enums.Role;
import common.request.RegisterRequest;
import common.response.AuthResponse;
import server.model.Admin;
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
}
