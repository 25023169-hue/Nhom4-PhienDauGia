package io.auctionsystem.server.config;

import io.auctionsystem.common.enums.AuctionState;
import io.auctionsystem.server.model.*;
import io.auctionsystem.server.repository.AuctionRepository;
import io.auctionsystem.server.repository.BidRepository;
import io.auctionsystem.server.repository.ItemRepository;
import io.auctionsystem.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

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

    @Override
    public void run(String... args) throws Exception {
        if (!userRepository.existsByUsername("admin")) {
            Admin admin = new Admin();
            admin.setUsername("admin");
            admin.setPassword("admin123");
            admin.setFirstname("Quản");
            admin.setLastname("Trị");

            // Set Employee Code như bạn muốn
            admin.setEmployeeCode("ADMIN-001");

            userRepository.save(admin);
            System.out.println(">>> HỆ THỐNG: Đã tạo tài khoản Admin mặc định (admin / admin123)");
        }

        // LỖI ĐÃ SỬA: Trước đây deleteAll() được gọi KHÔNG CÓ ĐIỀU KIỆN mỗi lần server khởi động
        // → xóa sạch toàn bộ dữ liệu thật (bid, auction, item) mỗi lần restart.
        // Sửa: Chỉ nạp dữ liệu mẫu khi database đang TRỐNG (item chưa có dòng nào).
        // Điều này giữ an toàn dữ liệu khi server restart trong môi trường thực tế.
        if (itemRepository.count() > 0) {
            System.out.println(">>> DATABASE ĐÃ CÓ DỮ LIỆU, BỎ QUA KHỞI TẠO MẪU.");
            return;
        }

        System.out.println(">>> DATABASE ĐANG TRỐNG, BẮT ĐẦU NẠP DỮ LIỆU MẪU...");

        User sampleSeller = findExistingSeller();
        if (sampleSeller == null) {
            System.err.println(">>> KHONG CO SELLER MAU, BO QUA NAP SAN PHAM MAU DE SERVER KHOI DONG BINH THUONG.");
            return;
        }

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
        createAuction(macbook.getId(), 2, AuctionState.RUNNING);

        Electronics iphone = new Electronics();
        iphone.setName("iPhone 15 Pro Max 1TB Titan");
        iphone.setDescription("Hàng mới nguyên seal, bản VN/A.");
        iphone.setStartingPrice(35000000.0);
        iphone.setCurrentPrice(35000000.0);
        iphone.setBrand("Apple");
        iphone = saveSampleItem(iphone, sampleSeller);
        createAuction(iphone.getId(), 1, AuctionState.RUNNING);

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
        createAuction(painting.getId(), 5, AuctionState.RUNNING);

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
        createAuction(oto.getId(), 7, AuctionState.RUNNING);

        Vehicle xemay = new Vehicle();
        xemay.setName("Honda SH 150i ABS");
        xemay.setDescription("Xe zin nguyên bản, biển số đẹp Hà Nội.");
        xemay.setStartingPrice(110000000.0);
        xemay.setCurrentPrice(110000000.0);
        xemay.setFuelType("Xăng");
        xemay.setManufactureYear(2022);
        xemay = saveSampleItem(xemay, sampleSeller);
        createAuction(xemay.getId(), 3, AuctionState.RUNNING);

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
        createAuction(aoDai.getId(), 4, AuctionState.RUNNING);

        Fashion tuiXach = new Fashion();
        tuiXach.setName("Túi xách Hermès Birkin 25");
        tuiXach.setDescription("Da Togo màu đen, phụ kiện mạ vàng (Gold Hardware).");
        tuiXach.setStartingPrice(450000000.0);
        tuiXach.setCurrentPrice(450000000.0);
        tuiXach.setBrand("Hermès");
        tuiXach.setGender("Nữ");
        tuiXach.setMaterial("Da bò Togo");
        tuiXach = saveSampleItem(tuiXach, sampleSeller);
        createAuction(tuiXach.getId(), 10, AuctionState.RUNNING);

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
        createAuction(nhanKC.getId(), 6, AuctionState.RUNNING);

        Jewelry dayChuyen = new Jewelry();
        dayChuyen.setName("Dây chuyền ngọc trai Akoya");
        dayChuyen.setDescription("Chuỗi ngọc trai tự nhiên tuyệt đẹp, ánh hồng tinh tế.");
        dayChuyen.setStartingPrice(18000000.0);
        dayChuyen.setCurrentPrice(18000000.0);
        dayChuyen.setMaterial("Vàng 18K");
        dayChuyen.setGemstone("Ngọc trai Akoya");
        dayChuyen = saveSampleItem(dayChuyen, sampleSeller);
        createAuction(dayChuyen.getId(), 2, AuctionState.RUNNING);

        System.out.println(">>> ĐÃ NẠP XONG TOÀN BỘ SẢN PHẨM MẪU VÀO SÀN ĐẤU GIÁ!");
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

    private void createAuction(Long itemId, int daysToAdd, AuctionState status) {
        Auction auction = new Auction();
        auction.setItemId(itemId);
        auction.setStartTime(LocalDateTime.now());
        auction.setEndTime(LocalDateTime.now().plusDays(daysToAdd));
        auction.setStatus(status);
        auctionRepository.save(auction);
    }
}