package io.auctionsystem.server.controller;

import io.auctionsystem.common.dto.AuctionItemDTO;
import io.auctionsystem.server.service.AuctionQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
public class AuctionController {

  private final AuctionQueryService auctionQueryService;

  @GetMapping("/running")
  public ResponseEntity<List<AuctionItemDTO>> getRunningAuctions() {
    return ResponseEntity.ok(auctionQueryService.getRunningAuctions());
  }

  @GetMapping("/running/seller/{sellerId}")
  public ResponseEntity<List<AuctionItemDTO>> getSellerRunningAuctions(
      @PathVariable Long sellerId) {
    return ResponseEntity.ok(auctionQueryService.getSellerRunningAuctions(sellerId));
  }

  @GetMapping("/participating/{bidderId}")
  public ResponseEntity<List<AuctionItemDTO>> getParticipatingAuctions(
      @PathVariable Long bidderId) {
    return ResponseEntity.ok(auctionQueryService.getParticipatingAuctions(bidderId));
  }
}
