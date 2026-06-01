package server.service;

import common.dto.SellerProductDTO;
import common.enums.AuctionState;
import common.enums.ItemType;
import common.request.ItemRequest;
import common.request.SellerProductRequest;
import server.exception.AccountException;
import server.exception.InvalidOperationException;
import server.exception.ResourceNotFoundException;
import server.exception.ValidationException;
import server.model.Art;
import server.model.Auction;
import server.model.Electronics;
import server.model.Fashion;
import server.model.Item;
import server.model.Jewelry;
import server.model.SellerProductListing;
import server.model.User;
import server.model.Vehicle;
import server.pattern.ItemFactory;
import server.pattern.ItemTypeResolver;
import server.repository.AuctionRepository;
import server.repository.ItemRepository;
import server.repository.SellerProductListingRepository;
import server.repository.UserRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SellerProductService {

  private static final DateTimeFormatter DISPLAY_FORMATTER =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  private final ItemRepository itemRepository;

  private final AuctionRepository auctionRepository;

  private final UserRepository userRepository;

  private final SellerProductListingRepository listingRepository;

  private final AuctionRealtimePublisher realtimePublisher;

  private final AuctionSettlementService settlementService;

  @Transactional
  public SellerProductDTO saveProductAndPrepareAuction(SellerProductRequest request) {
    validateRequest(request);

    User seller =
        userRepository
            .findById(request.getSellerId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy seller hiện tại"));
    if (!seller.isActive()) {
      throw new AccountException("Tài khoản seller đã bị vô hiệu hóa");
    }

    if (userRepository.isUserSeller(request.getSellerId()) <= 0) {
      throw new AccountException("Tài khoản hiện tại chưa có quyền Seller");
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
      throw new InvalidOperationException("Chỉ có thể chỉnh sửa sản phẩm ở trạng thái OPEN");
    }

    ItemType currentType = ItemTypeResolver.resolve(item);
    if (request.getItemType() != currentType) {
      throw new InvalidOperationException("Không thể đổi loại sản phẩm sau khi đã tạo");
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
      throw new InvalidOperationException("Sản phẩm đã bị xóa khỏi danh sách quản lý");
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
                () -> new ResourceNotFoundException("Không tìm thấy phiên đấu giá của sản phẩm"));
    if (auction.getStatus() != AuctionState.OPEN) {
      throw new InvalidOperationException("Chỉ có thể bắt đầu sản phẩm ở trạng thái OPEN");
    }
    if (auction.getStartTime() == null
        || auction.getEndTime() == null
        || !auction.getEndTime().isAfter(auction.getStartTime())) {
      throw new ValidationException("Thời lượng phiên đấu giá không hợp lệ");
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
      throw new InvalidOperationException("Sản phẩm đã bị xóa khỏi danh sách quản lý");
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
      if (!settlementService.cancelAuction(auction.getId())) {
        throw new InvalidOperationException("Phiên đấu giá không còn ở trạng thái có thể xóa");
      }
    } else if (auction.getStatus() != AuctionState.CANCELLED) {
      throw new InvalidOperationException("Chỉ có thể xóa sản phẩm có phiên OPEN hoặc RUNNING");
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
      throw new ResourceNotFoundException("Seller hiện tại không tồn tại");
    }

    List<SellerProductListing> allListings =
        listingRepository.findBySellerIdOrderByIdDesc(sellerId);
    List<SellerProductListing> listings =
        status == null
            ? allListings
            : allListings.stream().filter(listing -> listing.getStatus() == status).toList();

    List<Item> sellerItems = itemRepository.findBySellerId(sellerId);
    List<SellerProductDTO> result = new ArrayList<>();
    Set<Long> listedItemIds =
        allListings.stream().map(SellerProductListing::getItemId).collect(Collectors.toSet());
    Set<Long> itemIds = new HashSet<>(listedItemIds);
    itemIds.addAll(sellerItems.stream().map(Item::getId).toList());

    Map<Long, Item> itemsById =
        sellerItems.stream().collect(Collectors.toMap(Item::getId, Function.identity()));
    List<Long> missingItemIds =
        itemIds.stream().filter(itemId -> !itemsById.containsKey(itemId)).toList();
    itemRepository.findAllById(missingItemIds).forEach(item -> itemsById.put(item.getId(), item));

    Map<Long, List<Auction>> auctionsByItemId =
        itemIds.isEmpty()
            ? Map.of()
            : auctionRepository.findByItemIdIn(new ArrayList<>(itemIds)).stream()
                .collect(Collectors.groupingBy(Auction::getItemId));
    Map<Long, Auction> latestAuctionByItemId =
        auctionsByItemId.values().stream()
            .flatMap(List::stream)
            .collect(
                Collectors.toMap(
                    Auction::getItemId,
                    Function.identity(),
                    (first, second) -> first.getId() > second.getId() ? first : second));

    for (SellerProductListing listing : listings) {
      if (listing.isHidden()) {
        continue;
      }
      Item item = itemsById.get(listing.getItemId());
      if (item != null) {
        SellerProductDTO dto = toDTO(listing, item, latestAuctionByItemId.get(item.getId()));
        if (matchesKeyword(dto, keyword)) {
          result.add(dto);
        }
      }
    }

    // Dữ liệu cũ và dữ liệu mẫu có thể đã có item + auction nhưng chưa có listing.
    // Vẫn hiển thị các phiên đó trong màn quản lý Seller mà không tạo bản ghi trùng.
    for (Item item : sellerItems) {
      if (listedItemIds.contains(item.getId())) {
        continue;
      }

      for (Auction auction : auctionsByItemId.getOrDefault(item.getId(), List.of())) {
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
      throw new ValidationException("Dữ liệu sản phẩm không hợp lệ");
    }
    if (request.getSellerId() == null) {
      throw new ResourceNotFoundException("Seller hiện tại không tồn tại");
    }
    if (request.getName() == null || request.getName().trim().isEmpty()) {
      throw new ValidationException("Tên sản phẩm không được để trống");
    }
    if (request.getStartingPrice() == null || request.getStartingPrice() <= 0) {
      throw new ValidationException("Giá khởi điểm phải lớn hơn 0");
    }
    if (request.getStartTime() == null) {
      throw new ValidationException("Thời gian bắt đầu không được để trống");
    }
    if (request.getStartTime().isBefore(LocalDateTime.now())) {
      throw new ValidationException("Thời gian bắt đầu không được ở quá khứ");
    }
    if (request.getEndTime() == null) {
      throw new ValidationException("Thời gian kết thúc không được để trống");
    }
    if (!request.getEndTime().isAfter(request.getStartTime())) {
      throw new ValidationException("Thời gian kết thúc phải sau thời gian bắt đầu");
    }
    if (!request.getEndTime().isAfter(LocalDateTime.now())) {
      throw new ValidationException("Thời gian kết thúc phải ở tương lai");
    }
    if (request.getItemType() == null) {
      throw new ValidationException("Loại sản phẩm không được để trống");
    }
    if (request.getBuyNowPrice() != null
        && request.getBuyNowPrice() <= request.getStartingPrice()) {
      throw new ValidationException("Giá mua đứt phải lớn hơn giá khởi điểm");
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
    Auction latestAuction =
        auctionRepository.findTopByItemIdOrderByIdDesc(item.getId()).orElse(null);
    return toDTO(listing, item, latestAuction);
  }

  private SellerProductDTO toDTO(SellerProductListing listing, Item item, Auction latestAuction) {
    SellerProductDTO dto = new SellerProductDTO();
    dto.setListingId(listing.getId());
    dto.setItemId(item.getId());
    dto.setSellerId(listing.getSellerId());
    dto.setItemName(item.getName());
    dto.setDescription(item.getDescription());
    dto.setItemType(listing.getItemType());
    dto.setStartingPrice(item.getStartingPrice());
    dto.setCurrentPrice(item.getCurrentPrice());
    dto.setSoldPrice(findSoldPrice(latestAuction, listing.getStatus()));
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
    dto.setItemType(ItemTypeResolver.resolve(item));
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
      throw new ValidationException("Sản phẩm hoặc seller không hợp lệ");
    }

    Item item =
        itemRepository
            .findById(itemId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
    if (item.getSeller() == null || !sellerId.equals(item.getSeller().getId())) {
      throw new InvalidOperationException("Seller không có quyền thao tác sản phẩm này");
    }
    return item;
  }

  private Auction getLatestAuction(Long itemId) {
    return auctionRepository
        .findTopByItemIdOrderByIdDesc(itemId)
        .orElseThrow(
            () -> new ResourceNotFoundException("Không tìm thấy phiên đấu giá của sản phẩm"));
  }

  private SellerProductListing listingFor(Item item, Auction auction, Long sellerId) {
    SellerProductListing listing = new SellerProductListing();
    listing.setItemId(item.getId());
    listing.setSellerId(sellerId);
    listing.setStartTime(auction.getStartTime());
    listing.setEndTime(auction.getEndTime());
    listing.setItemType(ItemTypeResolver.resolve(item));
    listing.setStatus(auction.getStatus());
    return listing;
  }

  private Double findSoldPrice(Auction auction, AuctionState listingStatus) {
    if (listingStatus != AuctionState.FINISHED) {
      return null;
    }
    return auction == null ? null : getSoldPrice(auction);
  }

  private Double getSoldPrice(Auction auction) {
    if (auction.getStatus() != AuctionState.FINISHED || auction.getWinnerId() == null) {
      return null;
    }
    return auction.getFinalPrice();
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
