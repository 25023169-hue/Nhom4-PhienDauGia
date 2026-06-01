package io.auctionsystem.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "io.auctionsystem")
public class ServerApp {

  private static final Logger LOGGER = LoggerFactory.getLogger(ServerApp.class);

  public static void main(String[] args) {
    SpringApplication.run(ServerApp.class, args);
    LOGGER.info("Server đã sẵn sàng");
  }
}
