package io.auctionsystem.server.config;

import io.auctionsystem.common.enums.AuctionState;
import io.auctionsystem.server.model.Auction;
import io.auctionsystem.server.model.Electronics;
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
        System.out.println(">>> ĐANG KIỂM TRA VÀ DỌN DẸP DỮ LIỆU LỖI...");

        // 1. Dọn sạch rác do câu lệnh SQL thủ công trước đó gây ra
        // (Phải xóa Bid trước, rồi xóa Auction, cuối cùng mới xóa Item để không bị lỗi khóa ngoại)
        try {
            bidRepository.deleteAll();
            auctionRepository.deleteAll();
            itemRepository.deleteAll();
            System.out.println(">>> ĐÃ DỌN SẠCH DỮ LIỆU RÁC!");
        } catch (Exception e) {
            System.err.println(">>> BỎ QUA DỌN DẸP VÌ DATABASE ĐÃ SẠCH.");
        }

        // 2. Bơm lại dữ liệu chuẩn thông qua Hibernate
        System.out.println(">>> ĐANG TẠO LẠI PHIÊN ĐẤU GIÁ CHUẨN...");

        // Khởi tạo một đối tượng cụ thể (Electronics) thay vì Item trừu tượng
        Electronics macbook = new Electronics();
        macbook.setName("Siêu phẩm MacBook Pro M3");
        macbook.setDescription("Dùng để code Java bao mượt");
        macbook.setStartingPrice(20000000.0);
        macbook.setCurrentPrice(20000000.0);
        macbook.setBrand("Apple");

        // Lưu qua Hibernate sẽ tự động sinh ra dữ liệu khớp ở cả 2 bảng (items và electronics_items)
        macbook = itemRepository.save(macbook);

        Auction auction = new Auction();
        auction.setItemId(macbook.getId());
        auction.setStartTime(LocalDateTime.now());
        auction.setEndTime(LocalDateTime.now().plusDays(2));
        auction.setStatus(AuctionState.RUNNING);

        auctionRepository.save(auction);

        System.out.println(">>> ĐÃ KHÔI PHỤC VÀ TẠO PHIÊN ĐẤU GIÁ THÀNH CÔNG (ID: " + auction.getId() + ")");
    }
}