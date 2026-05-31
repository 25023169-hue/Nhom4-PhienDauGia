package io.auctionsystem.server.controller;

import io.auctionsystem.common.dto.AuctionItemDTO;
import io.auctionsystem.common.enums.AuctionState;
import io.auctionsystem.server.model.Auction;
import io.auctionsystem.server.model.Item;
import io.auctionsystem.server.repository.ItemRepository;
import io.auctionsystem.server.repository.WonAuctionRepository;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
public class InventoryEndpointController {

  @Autowired private WonAuctionRepository wonAuctionRepository;

  @Autowired private ItemRepository itemRepository;

  @GetMapping("/{winnerId}")
  public ResponseEntity<List<AuctionItemDTO>> getWonItems(@PathVariable Long winnerId) {
    List<Auction> wonAuctions =
        wonAuctionRepository.findByWinnerIdAndStatus(winnerId, AuctionState.FINISHED);
    List<AuctionItemDTO> dtoList = new ArrayList<>();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    for (Auction auction : wonAuctions) {
      Optional<Item> itemOpt = itemRepository.findById(auction.getItemId());
      if (itemOpt.isPresent()) {
        Item item = itemOpt.get();
        AuctionItemDTO dto = new AuctionItemDTO();
        dto.setId(auction.getId());
        dto.setName(item.getName());
        dto.setCurrentPrice(auction.getFinalPrice());
        dto.setEndTime(auction.getEndTime() != null ? auction.getEndTime().format(formatter) : "");
        dto.setStatus("WON");
        dtoList.add(dto);
      }
    }
    return ResponseEntity.ok(dtoList);
  }
}
