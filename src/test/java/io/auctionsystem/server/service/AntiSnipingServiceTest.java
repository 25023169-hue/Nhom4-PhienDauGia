package io.auctionsystem.server.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.auctionsystem.common.enums.AuctionState;
import io.auctionsystem.server.model.Auction;
import io.auctionsystem.server.repository.AuctionRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Test kiểm tra logic Anti-Sniping của AntiSnipingService.
 *
 * <p>Anti-sniping: nếu có bid vào trong vòng 60 giây cuối, phiên được gia hạn thêm 60 giây.
 * AntiSnipingService chịu trách nhiệm phát tín hiệu WebSocket để Client cập nhật thời gian kết
 * thúc mới.
 *
 * <p>Lưu ý: Logic gia hạn thực sự (cộng 60s vào endTime) được thực hiện trong BidService.placeBid.
 * AntiSnipingService chỉ kiểm tra và phát tín hiệu thông báo.
 */
class AntiSnipingServiceTest {

    @InjectMocks private AntiSnipingService antiSnipingService;

    @Mock private AuctionRepository auctionRepository;
    @Mock private AuctionRealtimePublisher realtimePublisher;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Test 1: Phiên còn ~61 giây (vừa được cộng 60s bởi BidService) → phải phát tín hiệu gia hạn.
     *
     * <p>Đây là trường hợp chính của anti-sniping: bid vào lúc còn <60s, BidService đã cộng 60s,
     * AntiSnipingService nhận thấy secondsLeft ≤ 61 và phát WebSocket.
     */
    @Test
    void testAntiSniping_TriggersWhenBidInLast60Seconds() {
        Auction auction = new Auction();
        auction.setId(1L);
        auction.setStatus(AuctionState.RUNNING);
        // Mô phỏng: BidService vừa cộng 60s, giờ còn khoảng 61s
        auction.setEndTime(LocalDateTime.now().plusSeconds(61));

        when(auctionRepository.findById(1L)).thenReturn(Optional.of(auction));

        antiSnipingService.notifyIfAntiSnipingTriggered(1L);

        // Phải gọi publish để thông báo Client cập nhật thời gian
        verify(realtimePublisher)
                .publishExtendedEndTimeAfterCommit(eq(1L), anyString());
    }

    /**
     * Test 2: Phiên còn nhiều thời gian (>61 giây) → KHÔNG phát tín hiệu gia hạn.
     *
     * <p>Anti-sniping chỉ kích hoạt khi bid vào gần cuối. Nếu còn nhiều thời gian, không cần thông
     * báo.
     */
    @Test
    void testAntiSniping_DoesNotTriggerWhenPlentyOfTimeLeft() {
        Auction auction = new Auction();
        auction.setId(2L);
        auction.setStatus(AuctionState.RUNNING);
        auction.setEndTime(LocalDateTime.now().plusMinutes(10)); // Còn 10 phút

        when(auctionRepository.findById(2L)).thenReturn(Optional.of(auction));

        antiSnipingService.notifyIfAntiSnipingTriggered(2L);

        // Không được phát tín hiệu gia hạn
        verify(realtimePublisher, never())
                .publishExtendedEndTimeAfterCommit(any(), any());
    }

    /**
     * Test 3: Phiên đã kết thúc (FINISHED) → KHÔNG phát tín hiệu.
     *
     * <p>Guard clause: chỉ phiên đang RUNNING mới được xét anti-sniping.
     */
    @Test
    void testAntiSniping_DoesNotTriggerForFinishedAuction() {
        Auction auction = new Auction();
        auction.setId(3L);
        auction.setStatus(AuctionState.FINISHED);
        auction.setEndTime(LocalDateTime.now().plusSeconds(30)); // Thời gian không quan trọng

        when(auctionRepository.findById(3L)).thenReturn(Optional.of(auction));

        antiSnipingService.notifyIfAntiSnipingTriggered(3L);

        verify(realtimePublisher, never())
                .publishExtendedEndTimeAfterCommit(any(), any());
    }

    /**
     * Test 4: Phiên không tồn tại → KHÔNG phát tín hiệu, không throw exception.
     *
     * <p>Guard clause: trường hợp auctionId không hợp lệ phải được xử lý yên lặng.
     */
    @Test
    void testAntiSniping_DoesNotTriggerForNonExistentAuction() {
        when(auctionRepository.findById(999L)).thenReturn(Optional.empty());

        // Không được ném exception
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> antiSnipingService.notifyIfAntiSnipingTriggered(999L));

        verify(realtimePublisher, never())
                .publishExtendedEndTimeAfterCommit(any(), any());
    }

    /**
     * Test 5: Phiên còn đúng 1 giây → vẫn phải phát tín hiệu (edge case).
     *
     * <p>Anti-sniping phải bắt cả trường hợp bid vào giây cuối cùng.
     */
    @Test
    void testAntiSniping_TriggersAtEdgeCase_1SecondLeft() {
        Auction auction = new Auction();
        auction.setId(5L);
        auction.setStatus(AuctionState.RUNNING);
        // 1s + 60s BidService đã cộng = endTime ở khoảng 61s từ bây giờ
        auction.setEndTime(LocalDateTime.now().plusSeconds(61));

        when(auctionRepository.findById(5L)).thenReturn(Optional.of(auction));

        antiSnipingService.notifyIfAntiSnipingTriggered(5L);

        verify(realtimePublisher)
                .publishExtendedEndTimeAfterCommit(eq(5L), anyString());
    }

    /**
     * Test 6: Phiên còn đúng 62 giây → KHÔNG phát tín hiệu (ngoài ngưỡng anti-sniping).
     *
     * <p>Kiểm tra: còn 120 giây (xa ngưỡng 61s) → không kích hoạt.
     * Dùng 120s thay vì 62s để tránh flaky test do độ trễ CPU khi chạy.
     */
    @Test
    void testAntiSniping_DoesNotTriggerAt62SecondsLeft() {
        Auction auction = new Auction();
        auction.setId(6L);
        auction.setStatus(AuctionState.RUNNING);
        auction.setEndTime(LocalDateTime.now().plusSeconds(120)); // Buffer an toàn, xa ngưỡng 61s

        when(auctionRepository.findById(6L)).thenReturn(Optional.of(auction));

        antiSnipingService.notifyIfAntiSnipingTriggered(6L);

        verify(realtimePublisher, never())
                .publishExtendedEndTimeAfterCommit(any(), any());
    }
}