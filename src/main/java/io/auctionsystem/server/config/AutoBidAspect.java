package io.auctionsystem.server.config;

import io.auctionsystem.common.response.BidResponse;
import io.auctionsystem.server.service.AutoBidService;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AutoBidAspect {

    @Autowired
    private AutoBidService autoBidService;

    // Lắng nghe: Cứ khi nào hàm placeBid của BidService chạy xong (không văng lỗi) và trả về kết quả
    @AfterReturning(pointcut = "execution(* io.auctionsystem.server.service.BidService.placeBid(..))", returning = "result")
    public void afterNormalBidPlaced(Object result) {
        // Nếu trả về được BidResponse -> Chắc chắn 100% đã đặt giá thành công
        if (result instanceof BidResponse) {
            BidResponse response = (BidResponse) result;

            // Bỏ qua check isSuccess() hay getSuccess(), kích hoạt Robot luôn!
            autoBidService.triggerAutoBids(response.getAuctionId());
        }
    }
}