package io.auctionsystem.server.controller;

import io.auctionsystem.common.dto.AuctionItemDTO;
import io.auctionsystem.common.enums.AuctionState;
import io.auctionsystem.server.model.Auction;
import io.auctionsystem.server.model.Item;
import io.auctionsystem.server.repository.AuctionRepository;
import io.auctionsystem.server.repository.BidRepository;
import io.auctionsystem.server.repository.ItemRepository;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auctions")
public class AuctionController {

  @Autowired private AuctionRepository auctionRepository;

  @Autowired private ItemRepository itemRepository;

  @Autowired private BidRepository bidRepository;

  @GetMapping("/running")
  public ResponseEntity<List<AuctionItemDTO>> getRunningAuctions() {
    // Chỉ lấy các phiên đấu giá đang trong trạng thái RUNNING
    List<Auction> runningAuctions = auctionRepository.findByStatus(AuctionState.RUNNING);
    List<AuctionItemDTO> dtoList = new ArrayList<>();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    for (Auction auction : runningAuctions) {
      Optional<Item> itemOpt = itemRepository.findById(auction.getItemId());
      if (itemOpt.isPresent()) {
        dtoList.add(toDTO(auction, itemOpt.get(), formatter));
      }
    }
    return ResponseEntity.ok(dtoList);
  }

  @GetMapping("/running/seller/{sellerId}")
  public ResponseEntity<List<AuctionItemDTO>> getSellerRunningAuctions(
      @PathVariable Long sellerId) {
    List<AuctionItemDTO> dtoList = new ArrayList<>();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    for (Auction auction : auctionRepository.findByStatus(AuctionState.RUNNING)) {
      itemRepository
          .findById(auction.getItemId())
          .filter(item -> item.getSeller() != null && sellerId.equals(item.getSeller().getId()))
          .map(item -> toDTO(auction, item, formatter))
          .ifPresent(dtoList::add);
    }
    return ResponseEntity.ok(dtoList);
  }

  @GetMapping("/participating/{bidderId}")
  public ResponseEntity<List<AuctionItemDTO>> getParticipatingAuctions(
      @PathVariable Long bidderId) {
    List<AuctionItemDTO> dtoList = new ArrayList<>();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    for (Long auctionId : bidRepository.findParticipatingAuctionIdsByBidderId(bidderId)) {
      Optional<Auction> auctionOpt = auctionRepository.findById(auctionId);
      if (auctionOpt.isEmpty() || auctionOpt.get().getStatus() != AuctionState.RUNNING) {
        continue;
      }

      Auction auction = auctionOpt.get();
      itemRepository
          .findById(auction.getItemId())
          .map(item -> toDTO(auction, item, formatter))
          .ifPresent(dtoList::add);
    }
    return ResponseEntity.ok(dtoList);
  }

  private AuctionItemDTO toDTO(Auction auction, Item item, DateTimeFormatter formatter) {
    AuctionItemDTO dto = new AuctionItemDTO();
    dto.setId(auction.getId());
    dto.setName(item.getName());
    dto.setCurrentPrice(item.getCurrentPrice());
    dto.setEndTime(auction.getEndTime() != null ? auction.getEndTime().format(formatter) : "");
    dto.setStatus(auction.getStatus().name());
    return dto;
  }
}
