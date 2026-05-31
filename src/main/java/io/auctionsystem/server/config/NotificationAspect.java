package io.auctionsystem.server.config;

import io.auctionsystem.common.request.BidRequest;
import io.auctionsystem.common.response.BidResponse;
import io.auctionsystem.server.service.NotificationService;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class NotificationAspect {

  @Autowired private NotificationService notificationService;

  // Bắt sự kiện ngay sau khi hàm placeBid chạy thành công và trả về kết quả
  @AfterReturning(
      pointcut =
          "execution(* io.auctionsystem.server.service.BidService.placeBid(io.auctionsystem.common.request.BidRequest)) && args(request)",
      returning = "result")
  public void afterBidPlaced(BidRequest request, Object result) {
    // Kiểm tra xem kết quả trả về có đúng là BidResponse không
    if (result instanceof BidResponse) {
      BidResponse response = (BidResponse) result;

      // Nếu đặt giá thành công (isAccepted = true)
      if (response.isAccepted()) {
        String message =
            "Bạn đã đặt giá thành công mức "
                + response.getNewCurrentPrice()
                + " VNĐ cho phiên đấu giá #"
                + response.getAuctionId();
        // Gọi NotificationService để lưu thông báo vào DB
        notificationService.createNotification(request.getBidderId(), message, "BID_SUCCESS");
      }
    }
  }
}
