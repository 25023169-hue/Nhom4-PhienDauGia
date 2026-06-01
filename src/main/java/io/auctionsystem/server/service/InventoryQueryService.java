package io.auctionsystem.server.service;

import io.auctionsystem.common.dto.AuctionItemDTO;
import io.auctionsystem.common.enums.AuctionState;
import io.auctionsystem.server.model.Auction;
import io.auctionsystem.server.model.Item;
import io.auctionsystem.server.repository.ItemRepository;
import io.auctionsystem.server.repository.WonAuctionRepository;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryQueryService {

  private static final DateTimeFormatter DISPLAY_FORMATTER =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  private final WonAuctionRepository wonAuctionRepository;
  private final ItemRepository itemRepository;

  public List<AuctionItemDTO> getWonItems(Long winnerId) {
    List<Auction> wonAuctions =
        wonAuctionRepository.findByWinnerIdAndStatus(winnerId, AuctionState.FINISHED);
    Map<Long, Item> itemsById =
        itemRepository
            .findAllById(wonAuctions.stream().map(Auction::getItemId).distinct().toList())
            .stream()
            .collect(Collectors.toMap(Item::getId, Function.identity()));

    return wonAuctions.stream()
        .map(auction -> toDTO(auction, itemsById.get(auction.getItemId())))
        .filter(java.util.Objects::nonNull)
        .toList();
  }

  private AuctionItemDTO toDTO(Auction auction, Item item) {
    if (item == null) {
      return null;
    }

    AuctionItemDTO dto = new AuctionItemDTO();
    dto.setId(auction.getId());
    dto.setItemId(item.getId());
    dto.setName(item.getName());
    dto.setCurrentPrice(auction.getFinalPrice());
    dto.setEndTime(
        auction.getEndTime() == null ? "" : auction.getEndTime().format(DISPLAY_FORMATTER));
    dto.setStatus("WON");
    return dto;
  }
}
