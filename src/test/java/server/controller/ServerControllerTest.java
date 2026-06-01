package server.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import common.dto.AuctionItemDTO;
import common.dto.BidDTO;
import common.dto.NotificationDTO;
import common.dto.RevenueStatsDTO;
import common.dto.SellerProductDTO;
import common.enums.AuctionState;
import common.enums.TransactionType;
import common.request.AddressRequest;
import common.request.BankRequest;
import common.request.BidRequest;
import common.request.LoginRequest;
import common.request.RegisterRequest;
import common.request.SellerProductRequest;
import common.response.AuthResponse;
import common.response.BidResponse;
import server.model.Admin;
import server.model.Bidder;
import server.model.Transaction;
import server.model.User;
import server.service.AdminService;
import server.service.AuctionQueryService;
import server.service.AuthService;
import server.service.BidService;
import server.service.InventoryQueryService;
import server.service.NotificationService;
import server.service.SellerProductService;
import server.service.TransactionService;
import server.service.UserProfileLogicService;
import server.service.UserService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ServerControllerTest {

  @Test
  void adminController_MapsRolesAndDelegatesActions() {
    AdminService service = Mockito.mock(AdminService.class);
    AdminController controller = new AdminController(service);
    Admin admin = user(new Admin(), 1L);
    Bidder seller = user(new Bidder(), 2L);
    Bidder bidder = user(new Bidder(), 3L);
    when(service.findAllUsers()).thenReturn(List.of(admin, seller, bidder));
    when(service.findSellerIds()).thenReturn(Set.of(2L));
    when(service.findAllAuctions()).thenReturn(List.of(new AuctionItemDTO()));

    var users = controller.getAllUsers().getBody();

    assertEquals(common.enums.Role.ADMIN, users.get(0).getRole());
    assertEquals(common.enums.Role.SELLER, users.get(1).getRole());
    assertEquals(common.enums.Role.BIDDER, users.get(2).getRole());
    assertEquals(1, controller.getAllAuctions().getBody().size());
    assertTrue(controller.deleteAuction(9L).getBody().isSuccess());
    verify(service).deleteAuction(9L);
  }

  @Test
  void auctionController_DelegatesQueries() {
    AuctionQueryService service = Mockito.mock(AuctionQueryService.class);
    AuctionController controller = new AuctionController(service);
    List<AuctionItemDTO> items = List.of(new AuctionItemDTO());
    when(service.getRunningAuctions()).thenReturn(items);
    when(service.getSellerRunningAuctions(1L)).thenReturn(items);
    when(service.getParticipatingAuctions(2L)).thenReturn(items);

    assertEquals(items, controller.getRunningAuctions().getBody());
    assertEquals(items, controller.getSellerRunningAuctions(1L).getBody());
    assertEquals(items, controller.getParticipatingAuctions(2L).getBody());
  }

  @Test
  void authController_DelegatesCommands() {
    AuthService service = Mockito.mock(AuthService.class);
    AuthController controller = new AuthController(service);
    RegisterRequest registerRequest = new RegisterRequest();
    LoginRequest loginRequest = new LoginRequest("user", "pass");
    AuthResponse authResponse = new AuthResponse();
    when(service.register(registerRequest)).thenReturn("registered");
    when(service.login("user", "pass")).thenReturn(authResponse);
    when(service.upgradeToSeller(1L, "store")).thenReturn("upgraded");

    assertEquals("registered", controller.register(registerRequest).getBody());
    assertEquals(authResponse, controller.login(loginRequest).getBody());
    assertEquals("upgraded", controller.upgradeToSeller(1L, "store").getBody());
  }

  @Test
  void bidController_DelegatesCommands() {
    BidService service = Mockito.mock(BidService.class);
    BidController controller = new BidController(service);
    BidRequest request = new BidRequest(1L, 2L, 100.0);
    BidResponse response = new BidResponse();
    List<BidDTO> bids = List.of(new BidDTO());
    when(service.placeBid(request)).thenReturn(response);
    when(service.getBidHistoryByBidder(2L)).thenReturn(bids);
    when(service.getBidsByAuction(1L)).thenReturn(bids);

    assertEquals(response, controller.placeBid(request).getBody());
    assertEquals(bids, controller.getBidHistory(2L).getBody());
    assertEquals(bids, controller.getAuctionBids(1L).getBody());
  }

  @Test
  void inventoryAndNotificationControllers_DelegateQueries() {
    InventoryQueryService inventoryService = Mockito.mock(InventoryQueryService.class);
    InventoryEndpointController inventoryController =
        new InventoryEndpointController(inventoryService);
    List<AuctionItemDTO> items = List.of(new AuctionItemDTO());
    when(inventoryService.getWonItems(1L)).thenReturn(items);
    assertEquals(items, inventoryController.getWonItems(1L).getBody());

    NotificationService notificationService = Mockito.mock(NotificationService.class);
    NotificationController notificationController = new NotificationController(notificationService);
    List<NotificationDTO> notifications = List.of();
    when(notificationService.getNotificationsForUser(1L)).thenReturn(notifications);
    assertEquals(notifications, notificationController.getMyNotifications(1L));
    when(notificationService.countUnreadNotifications(1L)).thenReturn(3L);
    assertEquals(3L, notificationController.getUnreadCount(1L));
    notificationController.readNotification(2L);
    verify(notificationService).markAsRead(2L);
    notificationController.readAllNotifications(1L);
    verify(notificationService).markAllAsRead(1L);
  }

  @Test
  void sellerProductController_DelegatesCommands() {
    SellerProductService service = Mockito.mock(SellerProductService.class);
    SellerProductController controller = new SellerProductController(service);
    SellerProductRequest request = new SellerProductRequest();
    SellerProductDTO product = new SellerProductDTO();
    when(service.getSellerProducts(1L, "key", AuctionState.OPEN)).thenReturn(List.of(product));
    when(service.saveProductAndPrepareAuction(request)).thenReturn(product);
    when(service.updateOpenProduct(2L, request)).thenReturn(product);
    when(service.startOpenAuction(2L, 1L)).thenReturn(product);

    assertEquals(1, ((List<?>) controller.getSellerProducts(1L, "key", AuctionState.OPEN).getBody()).size());
    assertTrue(controller.createSellerProduct(request).getBody().isSuccess());
    assertTrue(controller.updateSellerProduct(2L, request).getBody().isSuccess());
    assertTrue(controller.startSellerProductAuction(2L, 1L).getBody().isSuccess());
    assertTrue(controller.deleteSellerProduct(2L, 1L).getBody().isSuccess());
    verify(service).hideProduct(2L, 1L);
  }

  @Test
  void userController_MapsWalletAndTransactions() {
    UserService userService = Mockito.mock(UserService.class);
    TransactionService transactionService = Mockito.mock(TransactionService.class);
    UserController controller = new UserController(userService, transactionService);
    Bidder user = new Bidder();
    user.setBalance(1000.0);
    user.setHeldBalance(200.0);
    Transaction transaction = new Transaction();
    transaction.setId(1L);
    transaction.setMoneyIn(100.0);
    transaction.setMoneyOut(0.0);
    transaction.setLastBalance(800.0);
    transaction.setType(TransactionType.DEPOSIT);
    transaction.setNote("note");
    Transaction withoutTime = new Transaction();
    when(userService.getUser(1L)).thenReturn(user);
    when(transactionService.processTransaction(1L, 100.0, "Nạp", "note"))
        .thenReturn(transaction);
    when(transactionService.getTransactionsByUserId(1L))
        .thenReturn(List.of(transaction, withoutTime));
    when(transactionService.getSellerRevenueStats(1L)).thenReturn(new RevenueStatsDTO());

    assertEquals(transaction, controller.processWalletTransaction(1L, 100.0, "Nạp", "note").getBody());
    Map<?, ?> summary = (Map<?, ?>) controller.getWalletSummary(1L).getBody();
    assertEquals(800.0, summary.get("availableBalance"));
    assertEquals(2, ((List<?>) controller.getTransactionHistory(1L).getBody()).size());
    assertTrue(controller.getSellerRevenueStats(1L).getStatusCode().is2xxSuccessful());

    controller.updateBank(1L, new BankRequest());
    controller.updateAddress(1L, new AddressRequest());
    controller.deleteAccount(1L);
    verify(userService).updateBankInfo(any(), any());
    verify(userService).updateAddress(any(), any());
    verify(userService).deleteAccount(1L);
  }

  @Test
  void profileController_DelegatesUpdate() {
    UserProfileLogicService service = Mockito.mock(UserProfileLogicService.class);
    UserProfileEndpointController controller = new UserProfileEndpointController(service);

    assertTrue(
        controller.updateProfileName(1L, Map.of("firstname", "An")).getStatusCode().is2xxSuccessful());
    verify(service).updateProfile(1L, Map.of("firstname", "An"));
  }

  private <T extends User> T user(T user, Long id) {
    user.setId(id);
    user.setUsername("user" + id);
    user.setFirstname("first");
    user.setLastname("last");
    user.setBalance(100.0);
    return user;
  }
}
