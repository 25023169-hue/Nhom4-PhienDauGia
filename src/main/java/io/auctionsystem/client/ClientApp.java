package io.auctionsystem.client;

import io.auctionsystem.client.pattern.SceneManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class ClientApp extends Application {

  private static final double DEFAULT_WIDTH = 1280;
  private static final double DEFAULT_HEIGHT = 800;

  @Override
  public void start(Stage primaryStage) throws Exception {
    // 1. Chỉ cần nạp file fxml
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/fxml/user/login.fxml"));

    // KHÔNG CẦN loader.setControllerFactory(...) nữa.
    // JavaFX sẽ tự động tìm và khởi tạo LoginController của nó.

    Parent root = loader.load();
    primaryStage.setTitle("Auction System");
    primaryStage.setScene(new Scene(root));
    primaryStage.setWidth(DEFAULT_WIDTH);
    primaryStage.setHeight(DEFAULT_HEIGHT);
    Rectangle2D screenBounds = Screen.getPrimary().getBounds();
    primaryStage.setX(screenBounds.getMinX() + (screenBounds.getWidth() - DEFAULT_WIDTH) / 2);
    primaryStage.setY(screenBounds.getMinY() + (screenBounds.getHeight() - DEFAULT_HEIGHT) / 2);
    primaryStage.show();
    SceneManager.getInstance().setStage(primaryStage);
  }

  public static void main(String[] args) {
    launch(args);
  }
}
