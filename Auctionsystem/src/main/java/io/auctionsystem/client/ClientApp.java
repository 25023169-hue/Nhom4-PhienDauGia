package io.auctionsystem.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import io.auctionsystem.server.ServerApp; // Để khởi tạo context nếu chạy chung

public class ClientApp extends Application {

    private ConfigurableApplicationContext context;

    @Override
    public void init() {
        // Khởi tạo ngữ cảnh Spring để có thể dùng @Autowired trong Controller của JavaFX
        this.context = new SpringApplicationBuilder(ServerApp.class).run();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Đường dẫn mới theo cấu trúc thư mục bạn đã sửa
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/fxml/login.fxml"));

        // Dòng này cực kỳ quan trọng: Để Spring "bơm" Bean vào LoginController
        loader.setControllerFactory(context::getBean);

        Parent root = loader.load();
        primaryStage.setTitle("Auction System - Client");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    @Override
    public void stop() {
        context.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}