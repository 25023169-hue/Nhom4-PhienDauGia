package io.auctionsystem.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulingConfig {
    // Chỉ cần 2 annotation @Configuration và @EnableScheduling
    // Hệ thống sẽ tự động cho phép các hàm @Scheduled hoạt động ngầm.
}