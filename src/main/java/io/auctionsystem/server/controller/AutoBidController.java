package io.auctionsystem.server.controller;

import io.auctionsystem.server.service.AutoBidService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/autobids")
public class AutoBidController {

    @Autowired
    private AutoBidService autoBidService;

    @PostMapping("/register")
    public ResponseEntity<?> registerAutoBid(@RequestBody Map<String, Object> payload) {
        try {
            Long auctionId = Long.parseLong(payload.get("auctionId").toString());
            Long bidderId = Long.parseLong(payload.get("bidderId").toString());
            Double maxAmount = Double.parseDouble(payload.get("maxAmount").toString());
            Double incrementAmount = Double.parseDouble(payload.get("incrementAmount").toString());

            autoBidService.registerAutoBid(auctionId, bidderId, maxAmount, incrementAmount);
            return ResponseEntity.ok("Đã kích hoạt cấu hình Đấu giá tự động thành công!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi dữ liệu: " + e.getMessage());
        }
    }
}