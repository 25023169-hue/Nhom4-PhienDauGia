package server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import server.exception.ResourceNotFoundException;
import server.model.Bidder;
import server.repository.UserRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class UserProfileLogicServiceTest {

  @InjectMocks private UserProfileLogicService service;
  @Mock private UserRepository userRepository;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void updateProfile_UpdatesNames() {
    Bidder user = new Bidder();
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(userRepository.save(user)).thenReturn(user);

    var result = service.updateProfile(1L, Map.of("firstname", "An", "lastname", "Nguyen"));

    assertEquals("An", result.getFirstname());
    assertEquals("Nguyen", result.getLastname());
    verify(userRepository).save(user);
  }

  @Test
  void updateProfile_MissingUser_ThrowsException() {
    when(userRepository.findById(1L)).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> service.updateProfile(1L, Map.of("firstname", "An", "lastname", "Nguyen")));
  }
}
