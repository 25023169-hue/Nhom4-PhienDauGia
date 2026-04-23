package io.auctionsystem.client;

import io.auctionsystem.client.pattern.SceneManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // 1. Chỉ cần nạp file fxml
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/fxml/login.fxml"));

        // KHÔNG CẦN loader.setControllerFactory(...) nữa.
        // JavaFX sẽ tự động tìm và khởi tạo LoginController của nó.

        Parent root = loader.load();
        primaryStage.setTitle("Auction System");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
        SceneManager.getInstance().setStage(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}