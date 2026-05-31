package io.auctionsystem.server.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handleAllExceptions(Exception e) {
    // CÁI NÀY LÀ QUAN TRỌNG NHẤT: Bắt nó in lỗi đỏ ra tab ServerApp
    System.err.println("=== LỖI BỊ BẮT BỞI GLOBAL EXCEPTION HANDLER ===");
    e.printStackTrace();
    System.err.println("===============================================");

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("timestamp", Instant.now());
    body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
    body.put("error", "Internal Server Error: " + e.getMessage());

    return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
