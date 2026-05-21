package io.auctionsystem.server.config;

import io.auctionsystem.common.enums.AuctionState;
import io.auctionsystem.server.model.*;
import io.auctionsystem.server.repository.AuctionRepository;
import io.auctionsystem.server.repository.BidRepository;
import io.auctionsystem.server.repository.ItemRepository;
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

    @Override
    public void run(String... args) throws Exception {
        System.out.println(">>> ĐANG KIỂM TRA VÀ DỌN DẸP DỮ LIỆU CŨ...");

        try {
            bidRepository.deleteAll();
            auctionRepository.deleteAll();
            itemRepository.deleteAll();
            System.out.println(">>> ĐÃ DỌN SẠCH DỮ LIỆU RÁC!");
        } catch (Exception e) {
            System.err.println(">>> BỎ QUA DỌN DẸP VÌ DATABASE ĐÃ SẠCH.");
        }

        System.out.println(">>> ĐANG BƠM DỮ LIỆU MẪU ĐA DẠNG VÀO HỆ THỐNG...");

        // ==========================================
        // 1. ĐỒ ĐIỆN TỬ (ELECTRONICS)
        // ==========================================
        Electronics macbook = new Electronics();
        macbook.setName("Siêu phẩm MacBook Pro M3 Max");
        macbook.setDescription("Cấu hình khủng, code Java bao mượt không giật lag.");
        macbook.setStartingPrice(55000000.0);
        macbook.setCurrentPrice(55000000.0);
        macbook.setBrand("Apple");
        macbook = itemRepository.save(macbook);
        createAuction(macbook.getId(), 2, AuctionState.RUNNING);

        Electronics iphone = new Electronics();
        iphone.setName("iPhone 15 Pro Max 1TB Titan");
        iphone.setDescription("Hàng mới nguyên seal, bản VN/A.");
        iphone.setStartingPrice(35000000.0);
        iphone.setCurrentPrice(35000000.0);
        iphone.setBrand("Apple");
        iphone = itemRepository.save(iphone);
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
        painting = itemRepository.save(painting);
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
        oto = itemRepository.save(oto);
        createAuction(oto.getId(), 7, AuctionState.RUNNING);

        Vehicle xemay = new Vehicle();
        xemay.setName("Honda SH 150i ABS");
        xemay.setDescription("Xe zin nguyên bản, biển số đẹp Hà Nội.");
        xemay.setStartingPrice(110000000.0);
        xemay.setCurrentPrice(110000000.0);
        xemay.setFuelType("Xăng");
        xemay.setManufactureYear(2022);
        xemay = itemRepository.save(xemay);
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
        aoDai = itemRepository.save(aoDai);
        createAuction(aoDai.getId(), 4, AuctionState.RUNNING);

        Fashion tuiXach = new Fashion();
        tuiXach.setName("Túi xách Hermès Birkin 25");
        tuiXach.setDescription("Da Togo màu đen, phụ kiện mạ vàng (Gold Hardware).");
        tuiXach.setStartingPrice(450000000.0);
        tuiXach.setCurrentPrice(450000000.0);
        tuiXach.setBrand("Hermès");
        tuiXach.setGender("Nữ");
        tuiXach.setMaterial("Da bò Togo");
        tuiXach = itemRepository.save(tuiXach);
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
        nhanKC.setWeight(4.5); // 4.5 gram
        nhanKC = itemRepository.save(nhanKC);
        createAuction(nhanKC.getId(), 6, AuctionState.RUNNING);

        Jewelry dayChuyen = new Jewelry();
        dayChuyen.setName("Dây chuyền ngọc trai Akoya");
        dayChuyen.setDescription("Chuỗi ngọc trai tự nhiên tuyệt đẹp, ánh hồng tinh tế.");
        dayChuyen.setStartingPrice(18000000.0);
        dayChuyen.setCurrentPrice(18000000.0);
        dayChuyen.setMaterial("Vàng 18K");
        dayChuyen.setGemstone("Ngọc trai Akoya");
        dayChuyen = itemRepository.save(dayChuyen);
        createAuction(dayChuyen.getId(), 2, AuctionState.RUNNING);

        System.out.println(">>> ĐÃ NẠP XONG TOÀN BỘ SẢN PHẨM MẪU VÀO SÀN ĐẤU GIÁ!");
    }

    // Hàm hỗ trợ tạo nhanh phiên đấu giá để code ở trên ngắn gọn hơn
    private void createAuction(Long itemId, int daysToAdd, AuctionState status) {
        Auction auction = new Auction();
        auction.setItemId(itemId);
        auction.setStartTime(LocalDateTime.now());
        auction.setEndTime(LocalDateTime.now().plusDays(daysToAdd));
        auction.setStatus(status);
        auctionRepository.save(auction);
    }
}