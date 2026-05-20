package io.auctionsystem.server.service;

import io.auctionsystem.common.request.BidRequest;
import io.auctionsystem.server.model.Auction;
import io.auctionsystem.server.model.AutoBid;
import io.auctionsystem.server.model.Item;
import io.auctionsystem.server.repository.AuctionRepository;
import io.auctionsystem.server.repository.AutoBidRepository;
import io.auctionsystem.server.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AutoBidService {

    @Autowired
    private AutoBidRepository autoBidRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private BidService bidService;

    // Lưu hoặc cập nhật cấu hình đặt giá tự động
    public void registerAutoBid(Long auctionId, Long bidderId, Double maxAmount, Double incrementAmount) {
        AutoBid autoBid = autoBidRepository.findByAuctionIdAndBidderId(auctionId, bidderId)
                .orElse(new AutoBid());

        autoBid.setAuctionId(auctionId);
        autoBid.setBidderId(bidderId);
        autoBid.setMaxAmount(maxAmount);
        autoBid.setIncrementAmount(incrementAmount);
        autoBid.setActive(true);

        autoBidRepository.save(autoBid);
    }

    // Hàm tự động quét và kích hoạt nâng giá ngầm
    @Transactional
    public synchronized void triggerAutoBids(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId).orElse(null);
        if (auction == null || !auction.getStatus().name().equals("RUNNING")) return;

        Item item = itemRepository.findById(auction.getItemId()).orElse(null);
        if (item == null) return;

        double currentPrice = item.getCurrentPrice();
        Long currentWinnerId = auction.getWinnerId();

        // Lấy tất cả robot đang bật của phiên này
        List<AutoBid> activeRobots = autoBidRepository.findByAuctionIdAndActiveTrue(auctionId);

        for (AutoBid robot : activeRobots) {
            // Nếu người này đang dẫn đầu rồi thì không cần tự đè giá chính mình
            if (robot.getBidderId().equals(currentWinnerId)) continue;

            // Tính mức giá tiếp theo robot cần đặt = Giá hiện tại + Bước tăng của họ
            double nextBid = currentPrice + robot.getIncrementAmount();

            // Nếu vẫn nằm trong ngân sách cho phép
            if (nextBid <= robot.getMaxAmount()) {
                try {
                    System.out.println(">>> [ROBOT] Tài khoản ID " + robot.getBidderId() + " tự động nâng giá lên: " + nextBid);
                    BidRequest request = new BidRequest(auctionId, robot.getBidderId(), nextBid);

                    // Gọi trực tiếp luồng đặt giá gốc (tiền tệ, ví, hoàn tiền, websocket sẽ tự chạy theo)
                    bidService.placeBid(request);

                    // Ngắt luồng tại đây vì hàm placeBid chạy xong sẽ lại kích hoạt Aspect quét lại từ đầu
                    break;
                } catch (Exception e) {
                    // Nếu lỗi (hết tiền ví...), tắt robot của người này
                    robot.setActive(false);
                    autoBidRepository.save(robot);
                }
            } else {
                // Vượt quá ngân sách tối đa -> Tắt cấu hình tự động
                robot.setActive(false);
                autoBidRepository.save(robot);
                System.out.println(">>> [ROBOT] Tài khoản ID " + robot.getBidderId() + " đã chạm hạn mức tối đa.");
            }
        }
    }
}