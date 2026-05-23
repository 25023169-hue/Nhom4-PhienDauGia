package io.auctionsystem.server.service;

import io.auctionsystem.common.dto.SellerProductDTO;
import io.auctionsystem.common.enums.AuctionState;
import io.auctionsystem.common.enums.ItemType;
import io.auctionsystem.common.request.ItemRequest;
import io.auctionsystem.common.request.SellerProductRequest;
import io.auctionsystem.server.model.Art;
import io.auctionsystem.server.model.Electronics;
import io.auctionsystem.server.model.Fashion;
import io.auctionsystem.server.model.Item;
import io.auctionsystem.server.model.Jewelry;
import io.auctionsystem.server.model.SellerProductListing;
import io.auctionsystem.server.model.User;
import io.auctionsystem.server.model.Vehicle;
import io.auctionsystem.server.pattern.ItemFactory;
import io.auctionsystem.server.repository.ItemRepository;
import io.auctionsystem.server.repository.SellerProductListingRepository;
import io.auctionsystem.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class SellerProductService {

    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SellerProductListingRepository listingRepository;

    @Transactional
    public SellerProductDTO saveProductAndPrepareAuction(SellerProductRequest request) {
        validateRequest(request);

        User seller = userRepository.findById(request.getSellerId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy seller hiện tại"));

        if (userRepository.isUserSeller(request.getSellerId()) <= 0) {
            throw new IllegalArgumentException("Tài khoản hiện tại chưa có quyền Seller");
        }

        Item item = ItemFactory.createItem(toItemRequest(request));
        fillItemSpecificFields(item, request);
        item.setCurrentPrice(request.getStartingPrice());
        item.setSeller(seller);

        Item savedItem = itemRepository.save(item);

        SellerProductListing listing = new SellerProductListing();
        listing.setItemId(savedItem.getId());
        listing.setSellerId(seller.getId());
        listing.setBidIncrement(request.getBidIncrement());
        listing.setBuyNowPrice(request.getBuyNowPrice());
        listing.setImageUrl(blankToNull(request.getImageUrl()));
        listing.setStartTime(request.getStartTime());
        listing.setEndTime(request.getEndTime());
        listing.setItemType(request.getItemType());
        listing.setStatus(AuctionState.OPEN);

        SellerProductListing savedListing = listingRepository.save(listing);
        return toDTO(savedListing, savedItem);
    }

    public List<SellerProductDTO> getSellerProducts(Long sellerId, String keyword, AuctionState status) {
        if (sellerId == null) {
            throw new IllegalArgumentException("Seller hiện tại không tồn tại");
        }

        List<SellerProductListing> listings = status == null
                ? listingRepository.findBySellerIdOrderByIdDesc(sellerId)
                : listingRepository.findBySellerIdAndStatusOrderByIdDesc(sellerId, status);

        List<SellerProductDTO> result = new ArrayList<>();
        for (SellerProductListing listing : listings) {
            itemRepository.findById(listing.getItemId())
                    .map(item -> toDTO(listing, item))
                    .filter(dto -> matchesKeyword(dto, keyword))
                    .ifPresent(result::add);
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
        if (request.getBidIncrement() == null || request.getBidIncrement() <= 0) {
            throw new IllegalArgumentException("Bước giá phải lớn hơn 0");
        }
        if (request.getStartTime() == null) {
            throw new IllegalArgumentException("Thời gian bắt đầu không được để trống");
        }
        if (request.getEndTime() == null) {
            throw new IllegalArgumentException("Thời gian kết thúc không được để trống");
        }
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new IllegalArgumentException("Thời gian kết thúc phải sau thời gian bắt đầu");
        }
        if (request.getItemType() == null) {
            throw new IllegalArgumentException("Loại sản phẩm không được để trống");
        }
        if (request.getBuyNowPrice() != null && request.getBuyNowPrice() <= request.getStartingPrice()) {
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
            electronics.setWarrantyMonths(request.getWarrantyMonths());
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
        dto.setBidIncrement(listing.getBidIncrement());
        dto.setBuyNowPrice(listing.getBuyNowPrice());
        dto.setImageUrl(listing.getImageUrl());
        dto.setStartTime(listing.getStartTime() == null ? "" : listing.getStartTime().format(DISPLAY_FORMATTER));
        dto.setEndTime(listing.getEndTime() == null ? "" : listing.getEndTime().format(DISPLAY_FORMATTER));
        dto.setStatus(listing.getStatus());
        dto.setEditable(listing.getStatus() != AuctionState.RUNNING && listing.getStatus() != AuctionState.FINISHED);
        return dto;
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
