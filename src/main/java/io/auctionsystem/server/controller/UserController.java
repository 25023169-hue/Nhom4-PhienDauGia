package io.auctionsystem.server.controller;

import io.auctionsystem.common.request.AddressRequest;
import io.auctionsystem.common.request.BankRequest;
import io.auctionsystem.server.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    // API: PUT http://localhost:8080/api/user/{id}/bank
    @PutMapping("/{id}/bank")
    public ResponseEntity<?> updateBank(@PathVariable("id") Long id, @RequestBody BankRequest request) {
        try {
            userService.updateBankInfo(id, request);
            return ResponseEntity.ok().body("Cập nhật thông tin ngân hàng thành công!");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi Server: " + e.getMessage());
        }
    }

    // API: PUT http://localhost:8080/api/user/{id}/address
    @PutMapping("/{id}/address")
    public ResponseEntity<?> updateAddress(@PathVariable("id") Long id, @RequestBody AddressRequest request) {
        try {
            userService.updateAddress(id, request);
            return ResponseEntity.ok().body("Cập nhật địa chỉ thành công!");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi Server: " + e.getMessage());
        }
    }
}