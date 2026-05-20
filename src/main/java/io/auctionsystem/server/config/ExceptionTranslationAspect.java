package io.auctionsystem.server.config;

import io.auctionsystem.server.exception.AuctionClosedException;
import io.auctionsystem.server.exception.InvalidBidException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ExceptionTranslationAspect {

    @Around("execution(* io.auctionsystem.server.service.BidService.placeBid(..))")
    public Object translateExceptions(ProceedingJoinPoint pjp) throws Throwable {
        try {
            // Cho phép hàm placeBid cũ chạy bình thường
            return pjp.proceed();
        } catch (IllegalArgumentException ex) {
            // Nếu phát hiện lỗi từ code cũ, tự động chuyển đổi sang Custom Exception của môn học
            String msg = ex.getMessage();
            if (msg.contains("không trong trạng thái mở")) {
                throw new AuctionClosedException(msg);
            } else if (msg.contains("lớn hơn") || msg.contains("không đủ")) {
                throw new InvalidBidException(msg);
            }
            throw ex;
        }
    }
}