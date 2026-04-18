package io.auctionsystem;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class App extends Application {

    private ConfigurableApplicationContext springContext;

    @Override
    public void init() {
        // Khởi động Spring Boot ngầm trước
        springContext = SpringApplication.run(App.class);
    }

    @Override
    public void start(Stage stage) throws Exception {
        // Trỏ tới file FXML đầu tiên của bạn (ví dụ: login.fxml hoặc tên file bạn đang có)
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/io/auctionsystem/FXML/tên_file_giao_diện_của_bạn.fxml"));

        // Bắt buộc: Giao quyền tạo Controller cho Spring Boot
        loader.setControllerFactory(springContext::getBean);

        Parent root = loader.load();
        stage.setScene(new Scene(root));
        stage.setTitle("Hệ thống đấu giá");
        stage.show();
    }

    @Override
    public void stop() {
        // Tắt hoàn toàn ứng dụng khi đóng cửa sổ
        springContext.close();
        Platform.exit();
    }

    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
}