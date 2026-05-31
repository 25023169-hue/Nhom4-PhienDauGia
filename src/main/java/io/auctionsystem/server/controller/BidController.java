package io.auctionsystem.server.controller;

import io.auctionsystem.common.request.BidRequest;
import io.auctionsystem.common.response.BidResponse;
import io.auctionsystem.server.service.BidService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bids")
public class BidController {

  @Autowired private BidService bidService;

  @PostMapping("/place")
  public ResponseEntity<?> placeBid(@RequestBody BidRequest request) {
    try {
      BidResponse response = bidService.placeBid(request);
      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    } catch (Exception e) {
      return ResponseEntity.internalServerError().body("Lỗi Server: " + e.getMessage());
    }
  }

  @GetMapping("/history/{bidderId}")
  public ResponseEntity<?> getBidHistory(@PathVariable Long bidderId) {
    try {
      return ResponseEntity.ok(bidService.getBidHistoryByBidder(bidderId));
    } catch (Exception e) {
      return ResponseEntity.internalServerError().body("Lỗi Server: " + e.getMessage());
    }
  }

  @GetMapping("/auction/{auctionId}")
  public ResponseEntity<?> getAuctionBids(@PathVariable Long auctionId) {
    try {
      return ResponseEntity.ok(bidService.getBidsByAuction(auctionId));
    } catch (Exception e) {
      return ResponseEntity.internalServerError().body("Lỗi Server: " + e.getMessage());
    }
  }
}
