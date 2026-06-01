package io.auctionsystem.server.service;

import io.auctionsystem.common.dto.AuctionItemDTO;
import io.auctionsystem.common.enums.AuctionState;
import io.auctionsystem.server.model.Auction;
import io.auctionsystem.server.model.Item;
import io.auctionsystem.server.repository.AuctionRepository;
import io.auctionsystem.server.repository.BidRepository;
import io.auctionsystem.server.repository.ItemRepository;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuctionQueryService {

  private static final DateTimeFormatter DISPLAY_FORMATTER =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  private final AuctionRepository auctionRepository;
  private final ItemRepository itemRepository;
  private final BidRepository bidRepository;

  public List<AuctionItemDTO> getRunningAuctions() {
    return toDTOs(auctionRepository.findByStatus(AuctionState.RUNNING), item -> true);
  }

  public List<AuctionItemDTO> getSellerRunningAuctions(Long sellerId) {
    return toDTOs(
        auctionRepository.findByStatus(AuctionState.RUNNING),
        item -> item.getSeller() != null && sellerId.equals(item.getSeller().getId()));
  }

  public List<AuctionItemDTO> getParticipatingAuctions(Long bidderId) {
    List<Long> auctionIds = bidRepository.findParticipatingAuctionIdsByBidderId(bidderId);
    Map<Long, Auction> auctionsById =
        auctionRepository.findAllById(auctionIds).stream()
            .collect(Collectors.toMap(Auction::getId, Function.identity()));
    List<Auction> runningAuctions =
        auctionIds.stream()
            .map(auctionsById::get)
            .filter(auction -> auction != null && auction.getStatus() == AuctionState.RUNNING)
            .toList();
    return toDTOs(runningAuctions, item -> true);
  }

  private List<AuctionItemDTO> toDTOs(List<Auction> auctions, Predicate<Item> itemFilter) {
    Map<Long, Item> itemsById =
        itemRepository
            .findAllById(auctions.stream().map(Auction::getItemId).distinct().toList())
            .stream()
            .collect(Collectors.toMap(Item::getId, Function.identity()));

    return auctions.stream()
        .map(auction -> toDTO(auction, itemsById.get(auction.getItemId())))
        .filter(dto -> dto != null && itemFilter.test(dto.item()))
        .map(AuctionItemWithSource::dto)
        .toList();
  }

  private AuctionItemWithSource toDTO(Auction auction, Item item) {
    if (item == null) {
      return null;
    }

    AuctionItemDTO dto = new AuctionItemDTO();
    dto.setId(auction.getId());
    dto.setItemId(item.getId());
    dto.setName(item.getName());
    dto.setCurrentPrice(item.getCurrentPrice());
    dto.setStartTime(
        auction.getStartTime() == null ? "" : auction.getStartTime().format(DISPLAY_FORMATTER));
    dto.setEndTime(
        auction.getEndTime() == null ? "" : auction.getEndTime().format(DISPLAY_FORMATTER));
    dto.setStatus(auction.getStatus().name());
    return new AuctionItemWithSource(dto, item);
  }

  private record AuctionItemWithSource(AuctionItemDTO dto, Item item) {}
}
