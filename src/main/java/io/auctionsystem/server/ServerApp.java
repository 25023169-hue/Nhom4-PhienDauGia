package io.auctionsystem.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
// QUAN TRỌNG: Quét toàn bộ để nhận diện Service, Repository và Controller
@ComponentScan(basePackages = "io.auctionsystem")
public class ServerApp {

  public static void main(String[] args) {
    // Chạy Spring Boot ở cổng 8080 (mặc định)
    // Không liên quan gì đến giao diện ở đây
    SpringApplication.run(ServerApp.class, args);
    System.out.println("=== SERVER ĐÃ SẴN SÀNG (Port 8080) ===");
  }
}
