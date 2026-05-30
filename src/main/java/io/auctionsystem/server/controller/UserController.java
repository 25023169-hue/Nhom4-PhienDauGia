package io.auctionsystem.server.controller;

import io.auctionsystem.common.request.AddressRequest;
import io.auctionsystem.common.request.BankRequest;
import io.auctionsystem.server.service.UserService;
import io.auctionsystem.server.service.TransactionService;
import io.auctionsystem.server.model.Transaction;
import io.auctionsystem.server.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private TransactionService transactionService;

    @PostMapping("/{id}/transaction")
    public ResponseEntity<?> processWalletTransaction(
            @PathVariable("id") Long id,
            @RequestParam("amount") Double amount,
            @RequestParam("type") String type,
            @RequestParam(value = "note", required = false) String note,
            @RequestParam(value = "currentBalance", required = false) Double ignoredCurrentBalance) {
        try {
            Transaction tx = transactionService.processTransaction(id, amount, type, note);
            return ResponseEntity.ok().body(tx);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi giao dịch: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/wallet")
    public ResponseEntity<?> getWalletSummary(@PathVariable("id") Long id) {
        try {
            User user = userService.getUser(id);
            Map<String, Double> summary = new HashMap<>();
            summary.put("totalBalance", user.getBalance());
            summary.put("heldBalance", user.getHeldBalance());
            summary.put("availableBalance", user.getAvailableBalance());
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Không thể tải số dư ví: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<?> getTransactionHistory(@PathVariable("id") Long id) {
        try {
            List<Transaction> list = transactionService.getTransactionsByUserId(id);
            List<Map<String, Object>> simplifiedList = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            for (Transaction tx : list) {
                Map<String, Object> map = new HashMap<>();
                map.put("type", tx.getType());
                map.put("moneyIn", tx.getMoneyIn());
                map.put("moneyOut", tx.getMoneyOut());
                map.put("lastBalance", tx.getLastBalance());
                map.put("note", tx.getNote());

                // Lấy đúng thời gian từ database
                if (tx.getTransactionTime() != null) {
                    map.put("time", tx.getTransactionTime().format(formatter));
                } else {
                    map.put("time", LocalDateTime.now().format(formatter));
                }

                simplifiedList.add(map);
            }

            return ResponseEntity.ok().body(simplifiedList);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Không thể tải lịch sử: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/bank")
    public ResponseEntity<?> updateBank(@PathVariable("id") Long id, @RequestBody BankRequest request) {
        try {
            userService.updateBankInfo(id, request);
            return ResponseEntity.ok().body("Cập nhật thông tin ngân hàng thành công!");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi Server: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/address")
    public ResponseEntity<?> updateAddress(@PathVariable("id") Long id, @RequestBody AddressRequest request) {
        try {
            userService.updateAddress(id, request);
            return ResponseEntity.ok().body("Cập nhật địa chỉ thành công!");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi Server: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAccount(@PathVariable("id") Long id) {
        try {
            userService.deleteAccount(id);
            return ResponseEntity.ok("Tài khoản đã được xóa thành công");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi xóa tài khoản: " + e.getMessage());
        }
    }
}
