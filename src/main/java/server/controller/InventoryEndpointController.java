package server.controller;

import common.dto.AuctionItemDTO;
import server.service.InventoryQueryService;
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
