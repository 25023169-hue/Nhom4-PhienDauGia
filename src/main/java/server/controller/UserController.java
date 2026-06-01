package server.controller;

import common.request.AddressRequest;
import common.request.BankRequest;
import server.model.Transaction;
import server.model.User;
import server.service.TransactionService;
import server.service.UserService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  private final TransactionService transactionService;

  @PostMapping("/{id}/transaction")
  public ResponseEntity<?> processWalletTransaction(
      @PathVariable("id") Long id,
      @RequestParam("amount") Double amount,
      @RequestParam("type") String type,
      @RequestParam(value = "note", required = false) String note) {
    Transaction tx = transactionService.processTransaction(id, amount, type, note);
    return ResponseEntity.ok().body(tx);
  }

  @GetMapping("/{id}/wallet")
  public ResponseEntity<?> getWalletSummary(@PathVariable("id") Long id) {
    User user = userService.getUser(id);
    Map<String, Double> summary = new HashMap<>();
    summary.put("totalBalance", user.getBalance());
    summary.put("heldBalance", user.getHeldBalance());
    summary.put("availableBalance", user.getAvailableBalance());
    return ResponseEntity.ok(summary);
  }

  @GetMapping("/{id}/transactions")
  public ResponseEntity<?> getTransactionHistory(@PathVariable("id") Long id) {
    List<Transaction> list = transactionService.getTransactionsByUserId(id);
    List<Map<String, Object>> simplifiedList = new ArrayList<>();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    for (Transaction tx : list) {
      Map<String, Object> map = new HashMap<>();
      map.put("id", tx.getId());
      map.put("type", tx.getType() == null ? "" : tx.getType().getDisplayName());
      map.put("moneyIn", tx.getMoneyIn());
      map.put("moneyOut", tx.getMoneyOut());
      map.put("lastBalance", tx.getLastBalance());
      map.put("note", tx.getNote());
      if (tx.getTransactionTime() != null) {
        map.put("time", tx.getTransactionTime().format(formatter));
      } else {
        map.put("time", LocalDateTime.now().format(formatter));
      }

      simplifiedList.add(map);
    }
    return ResponseEntity.ok().body(simplifiedList);
  }

  @GetMapping("/{id}/revenue-stats")
  public ResponseEntity<?> getSellerRevenueStats(@PathVariable("id") Long id) {
    return ResponseEntity.ok(transactionService.getSellerRevenueStats(id));
  }

  @PutMapping("/{id}/bank")
  public ResponseEntity<?> updateBank(
      @PathVariable("id") Long id, @RequestBody BankRequest request) {
    userService.updateBankInfo(id, request);
    return ResponseEntity.ok().body("Cập nhật thông tin ngân hàng thành công!");
  }

  @PutMapping("/{id}/address")
  public ResponseEntity<?> updateAddress(
      @PathVariable("id") Long id, @RequestBody AddressRequest request) {
    userService.updateAddress(id, request);
    return ResponseEntity.ok().body("Cập nhật địa chỉ thành công!");
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> deleteAccount(@PathVariable("id") Long id) {
    userService.deleteAccount(id);
    return ResponseEntity.ok("Tài khoản đã được xóa thành công");
  }
}
