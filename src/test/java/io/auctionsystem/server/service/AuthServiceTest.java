package io.auctionsystem.server.service;

import io.auctionsystem.common.enums.Role;
import io.auctionsystem.common.request.RegisterRequest;
import io.auctionsystem.server.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

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
}
