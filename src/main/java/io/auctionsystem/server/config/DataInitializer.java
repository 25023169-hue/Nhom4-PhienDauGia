package io.auctionsystem.server.config;

import io.auctionsystem.common.enums.AuctionState;
import io.auctionsystem.common.enums.ItemType;
import io.auctionsystem.server.model.*;
import io.auctionsystem.server.repository.AuctionRepository;
import io.auctionsystem.server.repository.BidRepository;
import io.auctionsystem.server.repository.ItemRepository;
import io.auctionsystem.server.repository.SellerProductListingRepository;
import io.auctionsystem.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SellerProductListingRepository sellerProductListingRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {

        // =========================================================
        // BƯỚC 1: Tạo Admin mặc định nếu chưa có
        // =========================================================
        if (!userRepository.existsByUsername("admin")) {
            Admin admin = new Admin();
            admin.setUsername("admin");
            admin.setPassword("admin123");
            admin.setFirstname("Quản");
            admin.setLastname("Trị");
            admin.setEmployeeCode("ADMIN-001");
            userRepository.save(admin);
            System.out.println(">>> HỆ THỐNG: Đã tạo tài khoản Admin mặc định (admin / admin123)");
        }

        // =========================================================
        // BƯỚC 2: Luôn đảm bảo có Seller mẫu
        // =========================================================
        User sampleSeller = findOrCreateSampleSeller();

        // =========================================================
        // BƯỚC 3: Kiểm tra items cũ có hợp lệ không
        // Nếu có items nhưng tất cả đều không có seller → xóa sạch để nạp lại
        // =========================================================
        long totalItems = itemRepository.count();
        if (totalItems > 0) {
            long itemsWithSeller = itemRepository.findAll().stream()
                    .filter(item -> item.getSeller() != null)
                    .count();

            if (itemsWithSeller == 0) {
                // Toàn bộ items cũ bị lỗi (không có seller) → xóa sạch để nạp lại
                System.out.println(">>> PHÁT HIỆN " + totalItems + " items cũ bị lỗi (không có seller). Đang xóa để nạp lại...");
                sellerProductListingRepository.deleteAll();
                auctionRepository.deleteAll();
                bidRepository.deleteAll();
                itemRepository.deleteAll();
                System.out.println(">>> Đã xóa sạch dữ liệu lỗi. Tiến hành nạp lại...");
            } else {
                System.out.println(">>> DATABASE ĐÃ CÓ " + itemsWithSeller + " SẢN PHẨM HỢP LỆ, BỎ QUA NẠP MẪU.");
                return;
            }
        }

        // =========================================================
        // BƯỚC 4: Nạp dữ liệu mẫu
        // =========================================================
        if (sampleSeller == null) {
            System.err.println(">>> KHONG CO SELLER MAU, BO QUA NAP SAN PHAM MAU.");
            return;
        }

        System.out.println(">>> BẮT ĐẦU NẠP DỮ LIỆU MẪU...");

        // ==========================================
        // 1. ĐỒ ĐIỆN TỬ (ELECTRONICS)
        // ==========================================
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

        // ==========================================
        // 2. NGHỆ THUẬT (ART)
        // ==========================================
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

        // ==========================================
        // 3. PHƯƠNG TIỆN (VEHICLE)
        // ==========================================
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

        // ==========================================
        // 4. THỜI TRANG (FASHION)
        // ==========================================
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

        // ==========================================
        // 5. TRANG SỨC (JEWELRY)
        // ==========================================
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

        System.out.println(">>> ĐÃ NẠP XONG TOÀN BỘ SẢN PHẨM MẪU VÀO SÀN ĐẤU GIÁ!");
    }

    private User findExistingSeller() {
        return userRepository.findAll().stream()
                .filter(user -> userRepository.isUserSeller(user.getId()) > 0)
                .findFirst()
                .orElse(null);
    }

    private User findOrCreateSampleSeller() {
        User existing = findExistingSeller();
        if (existing != null) {
            System.out.println(">>> Đã có Seller trong hệ thống, dùng Seller ID: " + existing.getId());
            return existing;
        }

        if (userRepository.existsByUsername("seller_sample")) {
            System.out.println(">>> Username 'seller_sample' đã tồn tại, thử upgrade lên Seller...");
            userRepository.findByUsername("seller_sample").ifPresent(u ->
                    userRepository.upgradeToSellerNative(u.getId(), "Cửa hàng Mẫu")
            );
            return findExistingSeller();
        }

        System.out.println(">>> Không có Seller, đang tạo tài khoản Seller mẫu (seller_sample / seller123)...");
        Bidder bidder = new Bidder();
        bidder.setUsername("seller_sample");
        bidder.setPassword("seller123");
        bidder.setFirstname("Seller");
        bidder.setLastname("Mẫu");
        bidder.setBalance(0.0);
        bidder.setHeldBalance(0.0);
        bidder.setActive(true);
        userRepository.save(bidder);
        userRepository.upgradeToSellerNative(bidder.getId(), "Cửa hàng Mẫu");
        System.out.println(">>> Đã tạo Seller mẫu ID: " + bidder.getId());
        return userRepository.findById(bidder.getId()).orElse(null);
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
            listing.setItemType(resolveItemType(item));
            Double startingPrice = item.getStartingPrice();
            if (startingPrice != null && startingPrice > 0) {
                listing.setBuyNowPrice(startingPrice * 1.2);
            }
            sellerProductListingRepository.save(listing);
        }
    }

    private ItemType resolveItemType(Item item) {
        if (item instanceof Art) return ItemType.ART;
        if (item instanceof Electronics) return ItemType.ELECTRONICS;
        if (item instanceof Vehicle) return ItemType.VEHICLE;
        if (item instanceof Fashion) return ItemType.FASHION;
        if (item instanceof Jewelry) return ItemType.JEWELRY;
        throw new IllegalArgumentException("Loại sản phẩm không được hỗ trợ");
    }
}