package io.auctionsystem.server.service;

import io.auctionsystem.common.dto.SellerProductDTO;
import io.auctionsystem.common.enums.AuctionState;
import io.auctionsystem.common.enums.ItemType;
import io.auctionsystem.common.request.ItemRequest;
import io.auctionsystem.common.request.SellerProductRequest;
import io.auctionsystem.server.model.Art;
import io.auctionsystem.server.model.Auction;
import io.auctionsystem.server.model.Electronics;
import io.auctionsystem.server.model.Fashion;
import io.auctionsystem.server.model.Item;
import io.auctionsystem.server.model.Jewelry;
import io.auctionsystem.server.model.SellerProductListing;
import io.auctionsystem.server.model.User;
import io.auctionsystem.server.model.Vehicle;
import io.auctionsystem.server.pattern.ItemFactory;
import io.auctionsystem.server.repository.AuctionRepository;
import io.auctionsystem.server.repository.ItemRepository;
import io.auctionsystem.server.repository.SellerProductListingRepository;
import io.auctionsystem.server.repository.UserRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SellerProductService {

  private static final DateTimeFormatter DISPLAY_FORMATTER =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  @Autowired private ItemRepository itemRepository;

  @Autowired private AuctionRepository auctionRepository;

  @Autowired private UserRepository userRepository;

  @Autowired private SellerProductListingRepository listingRepository;

  @Autowired private AuctionRealtimePublisher realtimePublisher;

  @Autowired private AuctionSettlementService settlementService;

  @Transactional
  public SellerProductDTO saveProductAndPrepareAuction(SellerProductRequest request) {
    validateRequest(request);

    User seller =
        userRepository
            .findById(request.getSellerId())
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy seller hiện tại"));
    if (!seller.isActive()) {
      throw new IllegalArgumentException("Tài khoản seller đã bị vô hiệu hóa");
    }

    if (userRepository.isUserSeller(request.getSellerId()) <= 0) {
      throw new IllegalArgumentException("Tài khoản hiện tại chưa có quyền Seller");
    }

    Item item = ItemFactory.createItem(toItemRequest(request));
    fillItemSpecificFields(item, request);
    item.setCurrentPrice(request.getStartingPrice());
    item.setSeller(seller);

    Item savedItem = itemRepository.save(item);

    AuctionState auctionStatus =
        request.getStartTime().isAfter(LocalDateTime.now())
            ? AuctionState.OPEN
            : AuctionState.RUNNING;

    Auction auction = new Auction();
    auction.setItemId(savedItem.getId());
    auction.setStartTime(request.getStartTime());
    auction.setEndTime(request.getEndTime());
    auction.setFinalPrice(request.getStartingPrice());
    auction.setStatus(auctionStatus);
    auctionRepository.save(auction);

    SellerProductListing listing = new SellerProductListing();
    listing.setItemId(savedItem.getId());
    listing.setSellerId(seller.getId());
    listing.setBuyNowPrice(request.getBuyNowPrice());
    listing.setStartTime(request.getStartTime());
    listing.setEndTime(request.getEndTime());
    listing.setItemType(request.getItemType());
    listing.setStatus(auctionStatus);

    SellerProductListing savedListing = listingRepository.save(listing);
    realtimePublisher.publishAuctionListChangedAfterCommit();
    return toDTO(savedListing, savedItem);
  }

  @Transactional
  public SellerProductDTO updateOpenProduct(Long itemId, SellerProductRequest request) {
    validateRequest(request);

    Item item = getOwnedItem(itemId, request.getSellerId());
    Auction auction = getLatestAuction(itemId);
    if (auction.getStatus() != AuctionState.OPEN) {
      throw new IllegalArgumentException("Chỉ có thể chỉnh sửa sản phẩm ở trạng thái OPEN");
    }

    ItemType currentType = resolveItemType(item);
    if (request.getItemType() != currentType) {
      throw new IllegalArgumentException("Không thể đổi loại sản phẩm sau khi đã tạo");
    }

    item.setName(request.getName().trim());
    item.setDescription(blankToNull(request.getDescription()));
    item.setStartingPrice(request.getStartingPrice());
    item.setCurrentPrice(request.getStartingPrice());
    fillItemSpecificFields(item, request);
    itemRepository.save(item);

    auction.setStartTime(request.getStartTime());
    auction.setEndTime(request.getEndTime());
    auction.setFinalPrice(request.getStartingPrice());
    auctionRepository.save(auction);

    SellerProductListing listing =
        listingRepository
            .findByItemId(itemId)
            .orElseGet(() -> listingFor(item, auction, request.getSellerId()));
    if (listing.isHidden()) {
      throw new IllegalArgumentException("Sản phẩm đã bị xóa khỏi danh sách quản lý");
    }
    listing.setBuyNowPrice(request.getBuyNowPrice());
    listing.setStartTime(request.getStartTime());
    listing.setEndTime(request.getEndTime());
    listing.setItemType(request.getItemType());
    listing.setStatus(AuctionState.OPEN);

    SellerProductListing savedListing = listingRepository.save(listing);
    realtimePublisher.publishAuctionListChangedAfterCommit();
    return toDTO(savedListing, item);
  }

  @Transactional
  public SellerProductDTO startOpenAuction(Long itemId, Long sellerId) {
    Item item = getOwnedItem(itemId, sellerId);
    Auction latestAuction = getLatestAuction(itemId);
    Auction auction =
        auctionRepository
            .findByIdForUpdate(latestAuction.getId())
            .orElseThrow(
                () -> new IllegalArgumentException("Không tìm thấy phiên đấu giá của sản phẩm"));
    if (auction.getStatus() != AuctionState.OPEN) {
      throw new IllegalArgumentException("Chỉ có thể bắt đầu sản phẩm ở trạng thái OPEN");
    }
    if (auction.getStartTime() == null
        || auction.getEndTime() == null
        || !auction.getEndTime().isAfter(auction.getStartTime())) {
      throw new IllegalArgumentException("Thời lượng phiên đấu giá không hợp lệ");
    }

    LocalDateTime startTime = LocalDateTime.now();
    LocalDateTime endTime =
        startTime.plus(Duration.between(auction.getStartTime(), auction.getEndTime()));
    auction.setStartTime(startTime);
    auction.setEndTime(endTime);
    auction.setStatus(AuctionState.RUNNING);
    auctionRepository.save(auction);

    SellerProductListing listing =
        listingRepository.findByItemId(itemId).orElseGet(() -> listingFor(item, auction, sellerId));
    if (listing.isHidden()) {
      throw new IllegalArgumentException("Sản phẩm đã bị xóa khỏi danh sách quản lý");
    }
    listing.setStartTime(startTime);
    listing.setEndTime(endTime);
    listing.setStatus(AuctionState.RUNNING);

    SellerProductListing savedListing = listingRepository.save(listing);
    realtimePublisher.publishStatusAfterCommit(auction.getId(), "RUNNING");
    realtimePublisher.publishAuctionListChangedAfterCommit();
    return toDTO(savedListing, item);
  }

  @Transactional
  public void hideProduct(Long itemId, Long sellerId) {
    Item item = getOwnedItem(itemId, sellerId);
    Auction auction = getLatestAuction(itemId);

    if (auction.getStatus() == AuctionState.OPEN || auction.getStatus() == AuctionState.RUNNING) {
      settlementService.cancelAuction(auction.getId());
    }

    SellerProductListing listing =
        listingRepository.findByItemId(itemId).orElseGet(() -> listingFor(item, auction, sellerId));
    listing.setHidden(true);
    listingRepository.save(listing);
    realtimePublisher.publishAuctionListChangedAfterCommit();
  }

  public List<SellerProductDTO> getSellerProducts(
      Long sellerId, String keyword, AuctionState status) {
    if (sellerId == null) {
      throw new IllegalArgumentException("Seller hiện tại không tồn tại");
    }

    List<SellerProductListing> listings =
        status == null
            ? listingRepository.findBySellerIdOrderByIdDesc(sellerId)
            : listingRepository.findBySellerIdAndStatusOrderByIdDesc(sellerId, status);

    List<SellerProductDTO> result = new ArrayList<>();
    Set<Long> listedItemIds = new HashSet<>();
    for (SellerProductListing listing : listings) {
      listedItemIds.add(listing.getItemId());
      if (listing.isHidden()) {
        continue;
      }
      itemRepository
          .findById(listing.getItemId())
          .map(item -> toDTO(listing, item))
          .filter(dto -> matchesKeyword(dto, keyword))
          .ifPresent(result::add);
    }

    // Dữ liệu cũ và dữ liệu mẫu có thể đã có item + auction nhưng chưa có listing.
    // Vẫn hiển thị các phiên đó trong màn quản lý Seller mà không tạo bản ghi trùng.
    for (Item item : itemRepository.findBySellerId(sellerId)) {
      if (listedItemIds.contains(item.getId())) {
        continue;
      }

      for (Auction auction : auctionRepository.findByItemId(item.getId())) {
        SellerProductDTO dto = toDTO(auction, item, sellerId);
        if ((status == null || dto.getStatus() == status) && matchesKeyword(dto, keyword)) {
          result.add(dto);
        }
      }
    }
    return result;
  }

  private void validateRequest(SellerProductRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Dữ liệu sản phẩm không hợp lệ");
    }
    if (request.getSellerId() == null) {
      throw new IllegalArgumentException("Seller hiện tại không tồn tại");
    }
    if (request.getName() == null || request.getName().trim().isEmpty()) {
      throw new IllegalArgumentException("Tên sản phẩm không được để trống");
    }
    if (request.getStartingPrice() == null || request.getStartingPrice() <= 0) {
      throw new IllegalArgumentException("Giá khởi điểm phải lớn hơn 0");
    }
    if (request.getStartTime() == null) {
      throw new IllegalArgumentException("Thời gian bắt đầu không được để trống");
    }
    if (request.getStartTime().isBefore(LocalDateTime.now())) {
      throw new IllegalArgumentException("Thời gian bắt đầu không được ở quá khứ");
    }
    if (request.getEndTime() == null) {
      throw new IllegalArgumentException("Thời gian kết thúc không được để trống");
    }
    if (!request.getEndTime().isAfter(request.getStartTime())) {
      throw new IllegalArgumentException("Thời gian kết thúc phải sau thời gian bắt đầu");
    }
    if (!request.getEndTime().isAfter(LocalDateTime.now())) {
      throw new IllegalArgumentException("Thời gian kết thúc phải ở tương lai");
    }
    if (request.getItemType() == null) {
      throw new IllegalArgumentException("Loại sản phẩm không được để trống");
    }
    if (request.getBuyNowPrice() != null
        && request.getBuyNowPrice() <= request.getStartingPrice()) {
      throw new IllegalArgumentException("Giá mua đứt phải lớn hơn giá khởi điểm");
    }
  }

  private ItemRequest toItemRequest(SellerProductRequest request) {
    ItemRequest itemRequest = new ItemRequest();
    itemRequest.setName(request.getName().trim());
    itemRequest.setDescription(blankToNull(request.getDescription()));
    itemRequest.setStartingPrice(request.getStartingPrice());
    itemRequest.setSellerId(request.getSellerId());
    itemRequest.setType(request.getItemType());
    itemRequest.setArtist(request.getArtistName());
    itemRequest.setBrand(request.getBrand());
    itemRequest.setManufactureYear(request.getManufactureYear());
    itemRequest.setSize(request.getSize());
    itemRequest.setMaterial(request.getMaterial());
    itemRequest.setGender(request.getGender());
    itemRequest.setWeight(request.getWeight());
    itemRequest.setGemstone(request.getGemstone());
    return itemRequest;
  }

  private void fillItemSpecificFields(Item item, SellerProductRequest request) {
    if (item instanceof Art art) {
      art.setArtistName(blankToNull(request.getArtistName()));
      art.setMedium(blankToNull(request.getMedium()));
      art.setDimensions(blankToNull(request.getDimensions()));
      art.setCreationYear(request.getCreationYear());
    } else if (item instanceof Electronics electronics) {
      electronics.setBrand(blankToNull(request.getBrand()));
      electronics.setModel(blankToNull(request.getModel()));
      electronics.setCondition(request.getCondition());
    } else if (item instanceof Vehicle vehicle) {
      vehicle.setVinCode(blankToNull(request.getVinCode()));
      vehicle.setManufactureYear(request.getManufactureYear());
      vehicle.setFuelType(blankToNull(request.getFuelType()));
    } else if (item instanceof Fashion fashion) {
      fashion.setBrand(blankToNull(request.getBrand()));
      fashion.setSize(blankToNull(request.getSize()));
      fashion.setMaterial(blankToNull(request.getMaterial()));
      fashion.setGender(blankToNull(request.getGender()));
    } else if (item instanceof Jewelry jewelry) {
      jewelry.setMaterial(blankToNull(request.getMaterial()));
      jewelry.setWeight(request.getWeight());
      jewelry.setGemstone(blankToNull(request.getGemstone()));
    }
  }

  private SellerProductDTO toDTO(SellerProductListing listing, Item item) {
    SellerProductDTO dto = new SellerProductDTO();
    dto.setListingId(listing.getId());
    dto.setItemId(item.getId());
    dto.setSellerId(listing.getSellerId());
    dto.setItemName(item.getName());
    dto.setDescription(item.getDescription());
    dto.setItemType(listing.getItemType());
    dto.setStartingPrice(item.getStartingPrice());
    dto.setCurrentPrice(item.getCurrentPrice());
    dto.setSoldPrice(findSoldPrice(item.getId(), listing.getStatus()));
    dto.setBuyNowPrice(listing.getBuyNowPrice());
    dto.setStartTime(
        listing.getStartTime() == null ? "" : listing.getStartTime().format(DISPLAY_FORMATTER));
    dto.setEndTime(
        listing.getEndTime() == null ? "" : listing.getEndTime().format(DISPLAY_FORMATTER));
    dto.setStatus(listing.getStatus());
    dto.setEditable(listing.getStatus() == AuctionState.OPEN);
    fillItemSpecificFields(dto, item);
    return dto;
  }

  private SellerProductDTO toDTO(Auction auction, Item item, Long sellerId) {
    SellerProductDTO dto = new SellerProductDTO();
    dto.setItemId(item.getId());
    dto.setSellerId(sellerId);
    dto.setItemName(item.getName());
    dto.setDescription(item.getDescription());
    dto.setItemType(resolveItemType(item));
    dto.setStartingPrice(item.getStartingPrice());
    dto.setCurrentPrice(item.getCurrentPrice());
    dto.setSoldPrice(getSoldPrice(auction));
    dto.setStartTime(
        auction.getStartTime() == null ? "" : auction.getStartTime().format(DISPLAY_FORMATTER));
    dto.setEndTime(
        auction.getEndTime() == null ? "" : auction.getEndTime().format(DISPLAY_FORMATTER));
    dto.setStatus(auction.getStatus());
    dto.setEditable(auction.getStatus() == AuctionState.OPEN);
    fillItemSpecificFields(dto, item);
    return dto;
  }

  private void fillItemSpecificFields(SellerProductDTO dto, Item item) {
    if (item instanceof Art art) {
      dto.setArtistName(art.getArtistName());
      dto.setMedium(art.getMedium());
      dto.setDimensions(art.getDimensions());
      dto.setCreationYear(art.getCreationYear());
    } else if (item instanceof Electronics electronics) {
      dto.setBrand(electronics.getBrand());
      dto.setModel(electronics.getModel());
      dto.setCondition(electronics.getCondition());
    } else if (item instanceof Vehicle vehicle) {
      dto.setVinCode(vehicle.getVinCode());
      dto.setManufactureYear(vehicle.getManufactureYear());
      dto.setFuelType(vehicle.getFuelType());
    } else if (item instanceof Fashion fashion) {
      dto.setBrand(fashion.getBrand());
      dto.setSize(fashion.getSize());
      dto.setMaterial(fashion.getMaterial());
      dto.setGender(fashion.getGender());
    } else if (item instanceof Jewelry jewelry) {
      dto.setMaterial(jewelry.getMaterial());
      dto.setWeight(jewelry.getWeight());
      dto.setGemstone(jewelry.getGemstone());
    }
  }

  private Item getOwnedItem(Long itemId, Long sellerId) {
    if (itemId == null || sellerId == null) {
      throw new IllegalArgumentException("Sản phẩm hoặc seller không hợp lệ");
    }

    Item item =
        itemRepository
            .findById(itemId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));
    if (item.getSeller() == null || !sellerId.equals(item.getSeller().getId())) {
      throw new IllegalArgumentException("Seller không có quyền thao tác sản phẩm này");
    }
    return item;
  }

  private Auction getLatestAuction(Long itemId) {
    return auctionRepository
        .findTopByItemIdOrderByIdDesc(itemId)
        .orElseThrow(
            () -> new IllegalArgumentException("Không tìm thấy phiên đấu giá của sản phẩm"));
  }

  private SellerProductListing listingFor(Item item, Auction auction, Long sellerId) {
    SellerProductListing listing = new SellerProductListing();
    listing.setItemId(item.getId());
    listing.setSellerId(sellerId);
    listing.setStartTime(auction.getStartTime());
    listing.setEndTime(auction.getEndTime());
    listing.setItemType(resolveItemType(item));
    listing.setStatus(auction.getStatus());
    return listing;
  }

  private Double findSoldPrice(Long itemId, AuctionState listingStatus) {
    if (listingStatus != AuctionState.FINISHED) {
      return null;
    }
    return auctionRepository
        .findTopByItemIdOrderByIdDesc(itemId)
        .map(this::getSoldPrice)
        .orElse(null);
  }

  private Double getSoldPrice(Auction auction) {
    if (auction.getStatus() != AuctionState.FINISHED || auction.getWinnerId() == null) {
      return null;
    }
    return auction.getFinalPrice();
  }

  private ItemType resolveItemType(Item item) {
    if (item instanceof Art) return ItemType.ART;
    if (item instanceof Electronics) return ItemType.ELECTRONICS;
    if (item instanceof Vehicle) return ItemType.VEHICLE;
    if (item instanceof Fashion) return ItemType.FASHION;
    if (item instanceof Jewelry) return ItemType.JEWELRY;
    throw new IllegalArgumentException("Loại sản phẩm không được hỗ trợ");
  }

  private boolean matchesKeyword(SellerProductDTO dto, String keyword) {
    if (keyword == null || keyword.trim().isEmpty()) {
      return true;
    }

    String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
    String itemName = dto.getItemName() == null ? "" : dto.getItemName().toLowerCase(Locale.ROOT);
    String itemId = dto.getItemId() == null ? "" : dto.getItemId().toString();
    String listingId = dto.getListingId() == null ? "" : dto.getListingId().toString();

    return itemName.contains(normalizedKeyword)
        || itemId.contains(normalizedKeyword)
        || listingId.contains(normalizedKeyword);
  }

  private String blankToNull(String value) {
    if (value == null || value.trim().isEmpty()) {
      return null;
    }
    return value.trim();
  }
}
