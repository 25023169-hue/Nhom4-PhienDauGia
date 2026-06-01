package server.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import server.model.Admin;
import server.model.Bidder;
import server.model.Item;
import server.model.Seller;
import server.repository.AuctionRepository;
import server.repository.BidRepository;
import server.repository.ItemRepository;
import server.repository.NotificationRepository;
import server.repository.SellerProductListingRepository;
import server.repository.UserRepository;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.core.env.Environment;

class DataInitializerTest {

  @Mock private ItemRepository itemRepository;
  @Mock private AuctionRepository auctionRepository;
  @Mock private BidRepository bidRepository;
  @Mock private UserRepository userRepository;
  @Mock private NotificationRepository notificationRepository;
  @Mock private SellerProductListingRepository listingRepository;
  @Mock private Environment environment;
  private DataInitializer initializer;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    initializer =
        new DataInitializer(
            itemRepository,
            auctionRepository,
            bidRepository,
            userRepository,
            notificationRepository,
            listingRepository,
            environment);
    when(environment.getProperty("AUCTION_ADMIN_USERNAME", "admin")).thenReturn("admin");
  }

  @Test
  void run_ExistingData_SkipsSampleInitialization() throws Exception {
    when(userRepository.existsByUsername("admin")).thenReturn(true);
    when(itemRepository.count()).thenReturn(1L);

    initializer.run();

    verify(itemRepository, never()).save(any());
  }

  @Test
  void run_EmptyDatabaseWithoutSeller_SkipsSamples() throws Exception {
    when(userRepository.existsByUsername("admin")).thenReturn(true);
    when(itemRepository.count()).thenReturn(0L);
    when(userRepository.findAll()).thenReturn(List.of());

    initializer.run();

    verify(itemRepository, never()).save(any());
  }

  @Test
  void run_EmptyDatabaseWithSeller_CreatesNineSampleAuctions() throws Exception {
    Seller seller = new Seller();
    seller.setId(10L);
    AtomicLong ids = new AtomicLong();
    when(userRepository.existsByUsername("admin")).thenReturn(true);
    when(itemRepository.count()).thenReturn(0L);
    when(userRepository.findAll()).thenReturn(List.of(seller));
    when(userRepository.isUserSeller(10L)).thenReturn(1);
    when(itemRepository.save(any(Item.class)))
        .thenAnswer(
            invocation -> {
              Item item = invocation.getArgument(0);
              item.setId(ids.incrementAndGet());
              return item;
            });

    initializer.run();

    verify(itemRepository, Mockito.times(9)).save(any(Item.class));
    verify(auctionRepository, Mockito.times(9)).save(any());
    verify(listingRepository, Mockito.times(9)).save(any());
  }

  @Test
  void run_EmptyNotificationTableWithSellerAndBidder_CreatesEightSampleNotifications()
      throws Exception {
    Seller seller = new Seller();
    seller.setId(10L);
    Bidder bidder = new Bidder();
    bidder.setId(20L);
    when(userRepository.existsByUsername("admin")).thenReturn(true);
    when(itemRepository.count()).thenReturn(1L);
    when(notificationRepository.count()).thenReturn(0L);
    when(userRepository.findAllClients()).thenReturn(List.of(seller, bidder));
    when(userRepository.isUserSeller(10L)).thenReturn(1);
    when(userRepository.isUserSeller(20L)).thenReturn(0);

    initializer.run();

    verify(notificationRepository, Mockito.times(8)).save(any());
  }

  @Test
  void run_ConfiguredBootstrapAdmin_CreatesAdmin() throws Exception {
    when(userRepository.existsByUsername("admin")).thenReturn(false);
    when(environment.getProperty("AUCTION_ADMIN_PASSWORD")).thenReturn("secret");
    when(itemRepository.count()).thenReturn(1L);

    initializer.run();

    verify(userRepository).save(any(Admin.class));
  }

  @Test
  void run_MissingBootstrapPassword_DoesNotCreateAdmin() throws Exception {
    when(userRepository.existsByUsername("admin")).thenReturn(false);
    when(environment.getProperty("AUCTION_ADMIN_PASSWORD")).thenReturn(null);
    when(itemRepository.count()).thenReturn(1L);

    initializer.run();

    verify(userRepository, never()).save(any(Admin.class));
  }
}
