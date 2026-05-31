package io.auctionsystem.server.controller;

import io.auctionsystem.common.dto.AuctionItemDTO;
import io.auctionsystem.common.enums.AuctionState;
import io.auctionsystem.server.model.Auction;
import io.auctionsystem.server.model.Item;
import io.auctionsystem.server.repository.AuctionRepository;
import io.auctionsystem.server.repository.ItemRepository;
import io.auctionsystem.server.repository.SellerProductListingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/auctions")
public class AuctionController {

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private ItemRepository itemRepository;

    // THÊM: Để lấy imageUrl cho mỗi sản phẩm
    @Autowired
    private SellerProductListingRepository listingRepository;

    @GetMapping("/running")
    public ResponseEntity<List<AuctionItemDTO>> getRunningAuctions() {
        List<Auction> runningAuctions = auctionRepository.findByStatus(AuctionState.RUNNING);
        List<AuctionItemDTO> dtoList = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (Auction auction : runningAuctions) {
            Optional<Item> itemOpt = itemRepository.findById(auction.getItemId());
            if (itemOpt.isPresent()) {
                Item item = itemOpt.get();
                AuctionItemDTO dto = new AuctionItemDTO();
                dto.setId(auction.getId());
                dto.setName(item.getName());
                dto.setCurrentPrice(item.getCurrentPrice());
                dto.setEndTime(auction.getEndTime() != null ? auction.getEndTime().format(formatter) : "");
                dto.setStatus(auction.getStatus().name());

                // THÊM: Lấy imageUrl từ SellerProductListing nếu có
                listingRepository.findByItemId(auction.getItemId())
                        .ifPresent(listing -> dto.setImageUrl(listing.getImageUrl()));

                dtoList.add(dto);
            }
        }
        return ResponseEntity.ok(dtoList);
    }
}