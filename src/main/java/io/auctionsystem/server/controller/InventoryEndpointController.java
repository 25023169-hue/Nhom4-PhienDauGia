package io.auctionsystem.server.controller;

import io.auctionsystem.common.dto.AuctionItemDTO;
import io.auctionsystem.server.service.InventoryQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryEndpointController {

  private final InventoryQueryService inventoryQueryService;

  @GetMapping("/{winnerId}")
  public ResponseEntity<List<AuctionItemDTO>> getWonItems(@PathVariable Long winnerId) {
    return ResponseEntity.ok(inventoryQueryService.getWonItems(winnerId));
  }
}
