package io.auctionsystem.server.controller;

import io.auctionsystem.common.request.BidRequest;
import io.auctionsystem.common.response.BidResponse;
import io.auctionsystem.server.service.BidService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bids")
@RequiredArgsConstructor
public class BidController {

  private final BidService bidService;

  @PostMapping("/place")
  public ResponseEntity<?> placeBid(@RequestBody BidRequest request) {
    BidResponse response = bidService.placeBid(request);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/history/{bidderId}")
  public ResponseEntity<?> getBidHistory(@PathVariable Long bidderId) {
    return ResponseEntity.ok(bidService.getBidHistoryByBidder(bidderId));
  }

  @GetMapping("/auction/{auctionId}")
  public ResponseEntity<?> getAuctionBids(@PathVariable Long auctionId) {
    return ResponseEntity.ok(bidService.getBidsByAuction(auctionId));
  }
}
