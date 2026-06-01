package io.auctionsystem.server.service;

import io.auctionsystem.common.dto.AuctionItemDTO;
import io.auctionsystem.common.enums.AuctionState;
import io.auctionsystem.server.exception.InvalidOperationException;
import io.auctionsystem.server.exception.ResourceNotFoundException;
import io.auctionsystem.server.model.Auction;
import io.auctionsystem.server.model.Item;
import io.auctionsystem.server.model.User;
import io.auctionsystem.server.repository.AuctionRepository;
import io.auctionsystem.server.repository.ItemRepository;
import io.auctionsystem.server.repository.UserRepository;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminService {

  private static final DateTimeFormatter DISPLAY_FORMATTER =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  private final UserRepository userRepository;

  private final AuctionRepository auctionRepository;

  private final ItemRepository itemRepository;

  private final AuctionSettlementService settlementService;

  // Chỉ hiển thị tài khoản khách hàng trong bảng quản lý, không đưa admin vào danh sách.
  public List<User> findAllUsers() {
    return userRepository.findAllClients();
  }

  // 2. Hàm Khóa/Mở khóa tài khoản
  public boolean toggleBanUser(Long id) {
    Optional<User> userOpt = userRepository.findById(id);
    if (userOpt.isPresent()) {
      User user = userOpt.get();
      // Đảo ngược trạng thái ban (nếu đang true thì thành false và ngược lại)
      // Lưu ý: Trong Model User của bạn phải có biến boolean isBanned hoặc tương tự
      user.setBanned(!user.isBanned());
      userRepository.save(user);
      return true;
    }
    return false;
  }

  // 3. Hàm xóa User
  public void deleteUser(Long id) {
    userRepository.deleteById(id);
  }

  public Set<Long> findSellerIds() {
    return Set.copyOf(userRepository.findSellerIds());
  }

  public List<AuctionItemDTO> findAllAuctions() {
    List<Auction> auctions =
        auctionRepository.findAll().stream()
            .filter(auction -> auction.getStatus() != AuctionState.CANCELLED)
            .toList();
    Map<Long, Item> itemsById =
        itemRepository
            .findAllById(auctions.stream().map(Auction::getItemId).distinct().toList())
            .stream()
            .collect(Collectors.toMap(Item::getId, Function.identity()));

    return auctions.stream()
        .map(auction -> toDTO(auction, itemsById.get(auction.getItemId())))
        .flatMap(Optional::stream)
        .toList();
  }

  public void deleteAuction(Long auctionId) {
    Auction auction =
        auctionRepository
            .findById(auctionId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên đấu giá."));

    if (auction.getStatus() != AuctionState.OPEN && auction.getStatus() != AuctionState.RUNNING) {
      throw new InvalidOperationException("Chỉ có thể xóa phiên OPEN hoặc RUNNING.");
    }
    if (!settlementService.cancelAuction(auctionId)) {
      throw new InvalidOperationException("Phiên đấu giá không còn ở trạng thái có thể xóa.");
    }
  }

  private Optional<AuctionItemDTO> toDTO(Auction auction, Item item) {
    if (item == null) {
      return Optional.empty();
    }

    AuctionItemDTO dto = new AuctionItemDTO();
    dto.setId(auction.getId());
    dto.setItemId(item.getId());
    dto.setName(item.getName());
    dto.setCurrentPrice(item.getCurrentPrice());
    dto.setStartTime(format(auction.getStartTime()));
    dto.setEndTime(format(auction.getEndTime()));
    dto.setStatus(auction.getStatus().name());
    return Optional.of(dto);
  }

  private String format(java.time.LocalDateTime time) {
    return time == null ? "" : time.format(DISPLAY_FORMATTER);
  }
}
