package io.auctionsystem.server.service;

import io.auctionsystem.common.dto.AuctionItemDTO;
import io.auctionsystem.common.enums.AuctionState;
import io.auctionsystem.server.model.Auction;
import io.auctionsystem.server.model.User;
import io.auctionsystem.server.repository.AuctionRepository;
import io.auctionsystem.server.repository.ItemRepository;
import io.auctionsystem.server.repository.UserRepository;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

  @Autowired private UserRepository userRepository;

  @Autowired private AuctionRepository auctionRepository;

  @Autowired private ItemRepository itemRepository;

  @Autowired private AuctionSettlementService settlementService;

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

  public boolean isSeller(Long userId) {
    return userRepository.isUserSeller(userId) > 0;
  }

  public List<AuctionItemDTO> findAllAuctions() {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    List<AuctionItemDTO> result = new ArrayList<>();

    for (Auction auction : auctionRepository.findAll()) {
      if (auction.getStatus() == AuctionState.CANCELLED) {
        continue;
      }
      itemRepository
          .findById(auction.getItemId())
          .map(
              item -> {
                AuctionItemDTO dto = new AuctionItemDTO();
                dto.setId(auction.getId());
                dto.setItemId(item.getId());
                dto.setName(item.getName());
                dto.setCurrentPrice(item.getCurrentPrice());
                dto.setStartTime(format(auction.getStartTime(), formatter));
                dto.setEndTime(format(auction.getEndTime(), formatter));
                dto.setStatus(auction.getStatus().name());
                return dto;
              })
          .ifPresent(result::add);
    }
    return result;
  }

  public void deleteAuction(Long auctionId) {
    Auction auction =
        auctionRepository
            .findById(auctionId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên đấu giá."));

    if (auction.getStatus() != AuctionState.OPEN && auction.getStatus() != AuctionState.RUNNING) {
      throw new IllegalArgumentException("Chỉ có thể xóa phiên OPEN hoặc RUNNING.");
    }
    if (!settlementService.cancelAuction(auctionId)) {
      throw new IllegalArgumentException("Phiên đấu giá không còn ở trạng thái có thể xóa.");
    }
  }

  private String format(java.time.LocalDateTime time, DateTimeFormatter formatter) {
    return time == null ? "" : time.format(formatter);
  }
}
