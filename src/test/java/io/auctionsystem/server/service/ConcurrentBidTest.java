package io.auctionsystem.server.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.auctionsystem.common.enums.AuctionState;
import io.auctionsystem.common.request.BidRequest;
import io.auctionsystem.common.response.BidResponse;
import io.auctionsystem.server.model.Auction;
import io.auctionsystem.server.model.Bidder;
import io.auctionsystem.server.model.Item;
import io.auctionsystem.server.repository.AuctionRepository;
import io.auctionsystem.server.repository.BidCommitmentRepository;
import io.auctionsystem.server.repository.BidRepository;
import io.auctionsystem.server.repository.ItemRepository;
import io.auctionsystem.server.repository.SellerProductListingRepository;
import io.auctionsystem.server.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Test kiểm tra tính an toàn đồng thời (concurrency) của BidService.
 *
 * <p>Mục tiêu: đảm bảo khi nhiều Bidder đặt giá cùng lúc, hệ thống không bị race condition,
 * lost update, hay cho phép giá thấp hơn giá hiện tại thắng.
 */
class ConcurrentBidTest {

    @InjectMocks private BidService bidService;

    @Mock private AuctionRepository auctionRepository;
    @Mock private UserRepository userRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private BidRepository bidRepository;
    @Mock private BidCommitmentRepository bidCommitmentRepository;
    @Mock private SellerProductListingRepository listingRepository;
    @Mock private AuctionRealtimePublisher realtimePublisher;
    @Mock private AuctionSettlementService settlementService;

    private Auction runningAuction;
    private Item item;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        runningAuction = new Auction();
        runningAuction.setId(1L);
        runningAuction.setStatus(AuctionState.RUNNING);
        runningAuction.setItemId(10L);
        runningAuction.setStartTime(LocalDateTime.now().minusMinutes(5));
        runningAuction.setEndTime(LocalDateTime.now().plusHours(1));

        item = new Item() {};
        item.setId(10L);
        item.setCurrentPrice(100_000.0);

        when(auctionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(runningAuction));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(bidRepository.save(any())).thenReturn(null);
        when(bidCommitmentRepository.findByAuctionIdAndBidderId(any(), any()))
                .thenReturn(Optional.empty());
        when(listingRepository.findByItemId(any())).thenReturn(Optional.empty());
        doNothing().when(realtimePublisher).publishPriceAfterCommit(any(), any());
    }

    /**
     * Test 1: Nhiều thread đặt giá tăng dần — chỉ thread đặt cao nhất được chấp nhận ở mỗi bước.
     *
     * <p>Vì placeBid() dùng synchronized, các lời gọi phải được xử lý tuần tự. Kết quả cuối cùng
     * phải là giá cao nhất trong số các bid đã thắng.
     */
    @Test
    void testConcurrentBids_OnlyHighestPriceWins() throws InterruptedException {
        int threadCount = 5;
        // Mỗi bidder có số dư đủ lớn và đặt các mức giá khác nhau
        double[] amounts = {200_000.0, 350_000.0, 500_000.0, 280_000.0, 420_000.0};
        long[] bidderIds = {101L, 102L, 103L, 104L, 105L};

        for (int i = 0; i < threadCount; i++) {
            final long bidderId = bidderIds[i];
            Bidder bidder = new Bidder();
            bidder.setId(bidderId);
            bidder.setBalance(1_000_000.0);
            bidder.setHeldBalance(0.0);
            bidder.setUsername("bidder" + bidderId);
            bidder.setActive(true);
            when(userRepository.findByIdForUpdate(bidderId)).thenReturn(Optional.of(bidder));
        }

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1); // Cho tất cả thread khởi động đồng thời
        List<Future<Object>> futures = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final BidRequest req = new BidRequest(1L, bidderIds[i], amounts[i]);
            futures.add(
                    executor.submit(
                            () -> {
                                startLatch.await(); // Chờ tín hiệu xuất phát cùng lúc
                                try {
                                    BidResponse response = bidService.placeBid(req);
                                    if (response.isAccepted()) successCount.incrementAndGet();
                                } catch (IllegalArgumentException e) {
                                    // Bid thấp hơn giá hiện tại → bị từ chối → đây là hành vi đúng
                                    failCount.incrementAndGet();
                                }
                                return null;
                            }));
        }

        startLatch.countDown(); // Bắn tín hiệu, tất cả thread bắt đầu cùng lúc
        executor.shutdown();
        while (!executor.isTerminated()) {
            Thread.sleep(50);
        }

        // Tổng số bid = thành công + thất bại
        assertEquals(threadCount, successCount.get() + failCount.get(), "Phải xử lý đủ tất cả bid");

        // Giá cuối phải là giá cao nhất trong số các bid đã được chấp nhận
        double maxAmount = 0;
        for (double a : amounts) maxAmount = Math.max(maxAmount, a);
        // item.currentPrice phải bằng maxAmount vì bid cao nhất cuối cùng sẽ thắng
        assertEquals(
                maxAmount,
                item.getCurrentPrice(),
                0.01,
                "Giá cuối phải là mức cao nhất đã được chấp nhận");
    }

    /**
     * Test 2: Hai bidder đặt cùng một mức giá đồng thời — chỉ một người được chấp nhận.
     *
     * <p>Race condition điển hình: nếu không có synchronized, cả 2 bid có thể đều vượt qua kiểm tra
     * currentPrice > existingPrice. Với synchronized, chỉ người vào trước được chấp nhận, người sau
     * bị từ chối vì giá đã bằng nhau (không lớn hơn).
     */
    @Test
    void testConcurrentBids_SameAmount_OnlyOneAccepted() throws InterruptedException {
        double sameAmount = 200_000.0; // Cả 2 đặt cùng mức giá

        Bidder bidderA = new Bidder();
        bidderA.setId(201L);
        bidderA.setBalance(1_000_000.0);
        bidderA.setHeldBalance(0.0);
        bidderA.setUsername("bidderA");
        bidderA.setActive(true);

        Bidder bidderB = new Bidder();
        bidderB.setId(202L);
        bidderB.setBalance(1_000_000.0);
        bidderB.setHeldBalance(0.0);
        bidderB.setUsername("bidderB");
        bidderB.setActive(true);

        when(userRepository.findByIdForUpdate(201L)).thenReturn(Optional.of(bidderA));
        when(userRepository.findByIdForUpdate(202L)).thenReturn(Optional.of(bidderB));

        BidRequest reqA = new BidRequest(1L, 201L, sameAmount);
        BidRequest reqB = new BidRequest(1L, 202L, sameAmount);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<String> results = Collections.synchronizedList(new ArrayList<>());

        executor.submit(
                () -> {
                    startLatch.await();
                    try {
                        bidService.placeBid(reqA);
                        results.add("A_SUCCESS");
                    } catch (IllegalArgumentException e) {
                        results.add("A_FAIL");
                    }
                    return null;
                });

        executor.submit(
                () -> {
                    startLatch.await();
                    try {
                        bidService.placeBid(reqB);
                        results.add("B_SUCCESS");
                    } catch (IllegalArgumentException e) {
                        results.add("B_FAIL");
                    }
                    return null;
                });

        startLatch.countDown();
        executor.shutdown();
        while (!executor.isTerminated()) {
            Thread.sleep(50);
        }

        long successCount = results.stream().filter(r -> r.endsWith("SUCCESS")).count();
        long failCount = results.stream().filter(r -> r.endsWith("FAIL")).count();

        assertEquals(2, results.size(), "Phải xử lý đủ 2 bid");
        assertEquals(1, successCount, "Chỉ đúng 1 bid được chấp nhận khi cùng mức giá");
        assertEquals(1, failCount, "Bid còn lại phải bị từ chối");
    }

    /**
     * Test 3: Kiểm tra không có Lost Update — giá item không bị ghi đè bởi bid thấp hơn.
     *
     * <p>Kịch bản: bid 500k vào trước, bid 300k vào sau (thấp hơn). Với synchronized, bid 300k phải
     * thất bại và giá item phải giữ nguyên là 500k, không bị overwrite về 300k.
     */
    @Test
    void testNoConcurrentLostUpdate_LowerBidCannotOverwriteHigherPrice()
            throws InterruptedException {
        double highBid = 500_000.0;
        double lowBid = 300_000.0;

        Bidder highBidder = new Bidder();
        highBidder.setId(301L);
        highBidder.setBalance(2_000_000.0);
        highBidder.setHeldBalance(0.0);
        highBidder.setUsername("highBidder");
        highBidder.setActive(true);

        Bidder lowBidder = new Bidder();
        lowBidder.setId(302L);
        lowBidder.setBalance(2_000_000.0);
        lowBidder.setHeldBalance(0.0);
        lowBidder.setUsername("lowBidder");
        lowBidder.setActive(true);

        when(userRepository.findByIdForUpdate(301L)).thenReturn(Optional.of(highBidder));
        when(userRepository.findByIdForUpdate(302L)).thenReturn(Optional.of(lowBidder));

        BidRequest highReq = new BidRequest(1L, 301L, highBid);
        BidRequest lowReq = new BidRequest(1L, 302L, lowBid);

        // Đặt bid cao trước (tuần tự), sau đó thử đặt bid thấp
        bidService.placeBid(highReq); // Thành công, giá lên 500k

        Exception ex =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> bidService.placeBid(lowReq),
                        "Bid 300k phải bị từ chối vì giá hiện tại đã là 500k");

        assertTrue(
                ex.getMessage().contains("phải lớn hơn giá hiện tại"),
                "Thông báo lỗi phải nêu rõ lý do giá không hợp lệ");

        // Quan trọng: giá item phải vẫn là 500k, không bị overwrite
        assertEquals(
                highBid, item.getCurrentPrice(), 0.01, "Giá item không được bị ghi đè bởi bid thấp hơn");
    }
}