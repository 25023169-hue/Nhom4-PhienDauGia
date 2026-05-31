package io.auctionsystem.server.config;

import io.auctionsystem.common.response.BidResponse;
import io.auctionsystem.server.service.AntiSnipingService;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AntiSnipingAspect {

  @Autowired private AntiSnipingService antiSnipingService;

  // Kích hoạt ngay sau khi hàm placeBid của BidService chạy xong và trả về kết quả
  @AfterReturning(
      pointcut = "execution(* io.auctionsystem.server.service.BidService.placeBid(..))",
      returning = "result")
  public void afterBidPlacedCheckSniping(Object result) {
    if (result instanceof BidResponse) {
      BidResponse response = (BidResponse) result;
      if (response.isAccepted()) {
        // Kiểm tra và thông báo gia hạn thời gian hoàn toàn độc lập với BidService
        antiSnipingService.notifyIfAntiSnipingTriggered(response.getAuctionId());
      }
    }
  }
}
