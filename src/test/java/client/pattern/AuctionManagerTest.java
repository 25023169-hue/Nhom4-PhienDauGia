package client.pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import common.enums.Role;
import common.response.AuthResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AuctionManagerTest {

  private final AuctionManager manager = AuctionManager.getInstance();

  @AfterEach
  void tearDown() {
    manager.isLoggedOut();
    manager.consumeSettingsTabRequest();
  }

  @Test
  void loggedOut_ReturnsDefaults() {
    manager.isLoggedOut();

    assertFalse(manager.isLoggedIn());
    assertNull(manager.getToken());
    assertNull(manager.getId());
    assertEquals("Guest", manager.getUsername());
    assertNull(manager.getFirstname());
    assertNull(manager.getLastname());
    assertEquals("Seller", manager.getStoreName());
    assertEquals(0.0, manager.getBalance());
    assertNull(manager.getBankAccount());
    assertNull(manager.getRole());
    assertNull(manager.getAddress());
    assertNull(manager.getAccountName());
    assertFalse(manager.isAdmin());
    assertFalse(manager.hasBankInfo());
    manager.setBalance(100.0);
    assertEquals(0.0, manager.getBalance());
  }

  @Test
  void loggedIn_ReturnsUserValuesAndConsumesSettingsRequest() {
    AuthResponse response = new AuthResponse();
    response.setToken("token");
    response.setUserId(1L);
    response.setUsername("user");
    response.setFirstname("first");
    response.setLastname("last");
    response.setStoreName("store");
    response.setBalance(100.0);
    response.setBankAccount("123");
    response.setAccountName("name");
    response.setAddress("address");
    response.setRole(Role.ADMIN);

    manager.setCurrentUser(response);
    manager.setBalance(200.0);
    manager.requestSettingsTab("bank");

    assertTrue(manager.isLoggedIn());
    assertEquals("token", manager.getToken());
    assertEquals(1L, manager.getId());
    assertEquals("user", manager.getUsername());
    assertEquals("first", manager.getFirstname());
    assertEquals("last", manager.getLastname());
    assertEquals("store", manager.getStoreName());
    assertEquals(200.0, manager.getBalance());
    assertEquals("123", manager.getBankAccount());
    assertEquals(Role.ADMIN, manager.getRole());
    assertEquals("address", manager.getAddress());
    assertEquals("name", manager.getAccountName());
    assertTrue(manager.isAdmin());
    assertTrue(manager.hasBankInfo());
    assertEquals("bank", manager.consumeSettingsTabRequest());
    assertNull(manager.consumeSettingsTabRequest());
  }

  @Test
  void hasBankInfo_RejectsBlankFields() {
    AuthResponse response = new AuthResponse();
    response.setAccountName(" ");
    response.setBankAccount("123");
    manager.setCurrentUser(response);
    assertFalse(manager.hasBankInfo());

    response.setAccountName("name");
    response.setBankAccount(" ");
    assertFalse(manager.hasBankInfo());
  }
}
