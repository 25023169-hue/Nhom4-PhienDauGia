package server.config;

import common.enums.AuctionState;
import common.enums.NotificationType;
import server.model.*;
import server.pattern.ItemTypeResolver;
import server.repository.AuctionRepository;
import server.repository.BidRepository;
import server.repository.ItemRepository;
import server.repository.NotificationRepository;
import server.repository.SellerProductListingRepository;
import server.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(DataInitializer.class);

  private final ItemRepository itemRepository;

  private final AuctionRepository auctionRepository;

  private final BidRepository bidRepository;

  private final UserRepository userRepository;

  private final NotificationRepository notificationRepository;

  private final SellerProductListingRepository sellerProductListingRepository;

  private final Environment environment;

  @Override
  public void run(String... args) throws Exception {
    createBootstrapAdminIfConfigured();
    createSampleNotificationsIfEmpty();

    // LỖI ĐÃ SỬA: Trước đây deleteAll() được gọi KHÔNG CÓ ĐIỀU KIỆN mỗi lần server khởi động
    // → xóa sạch toàn bộ dữ liệu thật (bid, auction, item) mỗi lần restart.
    // Sửa: Chỉ nạp dữ liệu mẫu khi database đang TRỐNG (item chưa có dòng nào).
    // Điều này giữ an toàn dữ liệu khi server restart trong môi trường thực tế.
    if (itemRepository.count() > 0) {
      LOGGER.info("Database đã có dữ liệu, bỏ qua khởi tạo mẫu.");
      return;
    }

    LOGGER.info("Database đang trống, bắt đầu nạp dữ liệu mẫu.");

    User sampleSeller = findExistingSeller();
    if (sampleSeller == null) {
      LOGGER.warn("Không có seller mẫu, bỏ qua nạp sản phẩm mẫu.");
      return;
    }

    // 1. ĐỒ ĐIỆN TỬ (ELECTRONICS)
    Electronics macbook = new Electronics();
    macbook.setName("Siêu phẩm MacBook Pro M3 Max");
    macbook.setDescription("Cấu hình khủng, code Java bao mượt không giật lag.");
    macbook.setStartingPrice(55000000.0);
    macbook.setCurrentPrice(55000000.0);
    macbook.setBrand("Apple");
    macbook = saveSampleItem(macbook, sampleSeller);
    createAuction(macbook, 2, AuctionState.RUNNING);

    Electronics iphone = new Electronics();
    iphone.setName("iPhone 15 Pro Max 1TB Titan");
    iphone.setDescription("Hàng mới nguyên seal, bản VN/A.");
    iphone.setStartingPrice(35000000.0);
    iphone.setCurrentPrice(35000000.0);
    iphone.setBrand("Apple");
    iphone = saveSampleItem(iphone, sampleSeller);
    createAuction(iphone, 1, AuctionState.RUNNING);

    // 2. NGHỆ THUẬT (ART)
    Art painting = new Art();
    painting.setName("Tranh sơn dầu: Đêm Đầy Sao (Bản sao)");
    painting.setDescription("Bản chép tay cực kỳ tinh xảo từ kiệt tác của Van Gogh.");
    painting.setStartingPrice(12000000.0);
    painting.setCurrentPrice(12000000.0);
    painting.setArtistName("Vincent van Gogh");
    painting.setMedium("Sơn dầu");
    painting.setDimensions("73x92 cm");
    painting = saveSampleItem(painting, sampleSeller);
    createAuction(painting, 5, AuctionState.RUNNING);

    // 3. PHƯƠNG TIỆN (VEHICLE)
    Vehicle oto = new Vehicle();
    oto.setName("VinFast VF 8 Plus");
    oto.setDescription("Xe điện thông minh, màu xanh dương, mới chạy 5000km.");
    oto.setStartingPrice(850000000.0);
    oto.setCurrentPrice(850000000.0);
    oto.setFuelType("Điện");
    oto.setManufactureYear(2023);
    oto = saveSampleItem(oto, sampleSeller);
    createAuction(oto, 7, AuctionState.RUNNING);

    Vehicle xemay = new Vehicle();
    xemay.setName("Honda SH 150i ABS");
    xemay.setDescription("Xe zin nguyên bản, biển số đẹp Hà Nội.");
    xemay.setStartingPrice(110000000.0);
    xemay.setCurrentPrice(110000000.0);
    xemay.setFuelType("Xăng");
    xemay.setManufactureYear(2022);
    xemay = saveSampleItem(xemay, sampleSeller);
    createAuction(xemay, 3, AuctionState.RUNNING);

    // 4. THỜI TRANG (FASHION)
    Fashion aoDai = new Fashion();
    aoDai.setName("Áo dài lụa tơ tằm đính đá");
    aoDai.setDescription("Thiết kế thủ công độc bản cho bộ sưu tập Tết.");
    aoDai.setStartingPrice(4500000.0);
    aoDai.setCurrentPrice(4500000.0);
    aoDai.setBrand("Thái Tuấn");
    aoDai.setGender("Nữ");
    aoDai.setSize("M");
    aoDai.setMaterial("Lụa tơ tằm");
    aoDai = saveSampleItem(aoDai, sampleSeller);
    createAuction(aoDai, 4, AuctionState.RUNNING);

    Fashion tuiXach = new Fashion();
    tuiXach.setName("Túi xách Hermès Birkin 25");
    tuiXach.setDescription("Da Togo màu đen, phụ kiện mạ vàng (Gold Hardware).");
    tuiXach.setStartingPrice(450000000.0);
    tuiXach.setCurrentPrice(450000000.0);
    tuiXach.setBrand("Hermès");
    tuiXach.setGender("Nữ");
    tuiXach.setMaterial("Da bò Togo");
    tuiXach = saveSampleItem(tuiXach, sampleSeller);
    createAuction(tuiXach, 10, AuctionState.RUNNING);

    // 5. TRANG SỨC (JEWELRY)
    Jewelry nhanKC = new Jewelry();
    nhanKC.setName("Nhẫn đính hôn Kim Cương 2 Carat");
    nhanKC.setDescription("Nước D, độ tinh khiết VVS1, có giấy kiểm định GIA.");
    nhanKC.setStartingPrice(250000000.0);
    nhanKC.setCurrentPrice(250000000.0);
    nhanKC.setMaterial("Bạch kim");
    nhanKC.setGemstone("Kim cương tự nhiên");
    nhanKC.setWeight(4.5);
    nhanKC = saveSampleItem(nhanKC, sampleSeller);
    createAuction(nhanKC, 6, AuctionState.RUNNING);

    Jewelry dayChuyen = new Jewelry();
    dayChuyen.setName("Dây chuyền ngọc trai Akoya");
    dayChuyen.setDescription("Chuỗi ngọc trai tự nhiên tuyệt đẹp, ánh hồng tinh tế.");
    dayChuyen.setStartingPrice(18000000.0);
    dayChuyen.setCurrentPrice(18000000.0);
    dayChuyen.setMaterial("Vàng 18K");
    dayChuyen.setGemstone("Ngọc trai Akoya");
    dayChuyen = saveSampleItem(dayChuyen, sampleSeller);
    createAuction(dayChuyen, 2, AuctionState.RUNNING);

    LOGGER.info("Đã nạp xong toàn bộ sản phẩm mẫu.");
  }

  private void createSampleNotificationsIfEmpty() {
    if (notificationRepository.count() > 0) {
      return;
    }

    List<User> users = userRepository.findAllClients();
    User seller =
        users.stream().filter(user -> userRepository.isUserSeller(user.getId()) > 0).findFirst().orElse(null);
    User bidder =
        users.stream().filter(user -> userRepository.isUserSeller(user.getId()) == 0).findFirst().orElse(seller);
    if (bidder != null) {
      notificationRepository.save(
          new Notification(
              bidder,
              "Bạn đã bị vượt giá tại phiên sản phẩm #123!",
              NotificationType.BID_OUTBID));
      notificationRepository.save(
          new Notification(
              bidder, "Tuyệt vời! Bạn đã thắng phiên đấu giá #123!", NotificationType.BID_WON));
      notificationRepository.save(
          new Notification(
              bidder,
              "Phiên #123 đã kết thúc. Bạn không trúng giải.",
              NotificationType.BID_LOST));
      notificationRepository.save(
          new Notification(
              bidder, "Phiên đấu giá #123 đã bị hủy.", NotificationType.AUCTION_CANCELED));
    }
    if (seller != null) {
      notificationRepository.save(
          new Notification(
              seller,
              "Có lượt đặt giá mới: 1.000.000đ cho sản phẩm của bạn.",
              NotificationType.NEW_BID));
      notificationRepository.save(
          new Notification(
              seller,
              "Chúc mừng! Sản phẩm của bạn đã bán thành công với giá 1.000.000đ.",
              NotificationType.AUCTION_SOLD));
      notificationRepository.save(
          new Notification(
              seller,
              "Phiên đấu giá đã kết thúc nhưng không có ai mua (Ế).",
              NotificationType.AUCTION_EXPIRED));
      notificationRepository.save(
          new Notification(
              seller,
              "Bạn đã chủ động hủy phiên đấu giá của chính mình.",
              NotificationType.AUCTION_CANCELED));
    }
    if (bidder != null || seller != null) {
      LOGGER.info("Đã nạp bộ thông báo mẫu.");
    }
  }

  private void createBootstrapAdminIfConfigured() {
    String username = environment.getProperty("AUCTION_ADMIN_USERNAME", "admin");
    if (userRepository.existsByUsername(username)) {
      return;
    }

    String password = environment.getProperty("AUCTION_ADMIN_PASSWORD");
    if (!StringUtils.hasText(password)) {
      LOGGER.warn(
          "Bỏ qua tạo admin bootstrap vì chưa cấu hình biến môi trường AUCTION_ADMIN_PASSWORD.");
      return;
    }

    Admin admin = new Admin();
    admin.setUsername(username);
    admin.setPassword(password);
    admin.setFirstname("Trị");
    admin.setLastname("Quản");
    admin.setEmployeeCode("ADMIN-001");
    userRepository.save(admin);
    LOGGER.info("Đã tạo tài khoản admin bootstrap '{}'.", username);
  }

  private User findExistingSeller() {
    return userRepository.findAll().stream()
        .filter(user -> userRepository.isUserSeller(user.getId()) > 0)
        .findFirst()
        .orElse(null);
  }

  private <T extends Item> T saveSampleItem(T item, User seller) {
    item.setSeller(seller);
    return itemRepository.save(item);
  }

  private void createAuction(Item item, int daysToAdd, AuctionState status) {
    Auction auction = new Auction();
    auction.setItemId(item.getId());
    auction.setStartTime(LocalDateTime.now());
    auction.setEndTime(LocalDateTime.now().plusDays(daysToAdd));
    auction.setStatus(status);
    auctionRepository.save(auction);

    if (item.getSeller() != null) {
      SellerProductListing listing = new SellerProductListing();
      listing.setItemId(item.getId());
      listing.setSellerId(item.getSeller().getId());
      listing.setStartTime(auction.getStartTime());
      listing.setEndTime(auction.getEndTime());
      listing.setStatus(status);
      listing.setItemType(ItemTypeResolver.resolve(item));

      Double startingPrice = item.getStartingPrice();
      if (startingPrice != null && startingPrice > 0) {
        listing.setBuyNowPrice(startingPrice * 1.2);
      }
      sellerProductListingRepository.save(listing);
    }
  }
}
