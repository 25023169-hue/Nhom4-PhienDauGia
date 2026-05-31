package io.auctionsystem.client.pattern;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.function.Consumer;

public class SceneManager {
    private static SceneManager instance;
    private Stage stage;

    private SceneManager() {}

    public static SceneManager getInstance() {
        if (instance == null) instance = new SceneManager();
        return instance;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void switchScene(String fxmlPath) {
        switchScene(fxmlPath, controller -> {});
    }

    public void switchScene(String fxmlPath, Consumer<Object> configureController) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            configureController.accept(loader.getController());

            // Lấy Scene hiện tại của cửa sổ
            Scene currentScene = stage.getScene();

            if (currentScene == null) {
                // Nếu là lần đầu tiên mở app (chưa có Scene), thì tạo mới
                stage.setScene(new Scene(root));
            } else {
                // CHÌA KHÓA Ở ĐÂY: Nếu đã có Scene rồi, chỉ cần thay "ruột" (Root)
                // Cửa sổ sẽ không bị chớp và giữ nguyên được kích thước hiện tại
                currentScene.setRoot(root);
            }

            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
