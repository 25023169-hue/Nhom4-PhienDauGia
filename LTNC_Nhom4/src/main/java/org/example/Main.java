package org.example;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        // 1. Tạo nút bấm
        Button btn = new Button("Bấm vào tôi!");
        btn.setOnAction(e -> System.out.println("Xin chào từ JavaFX!"));

        // 2. Tạo bố cục (Layout) và thêm nút vào
        StackPane root = new StackPane();
        root.getChildren().add(btn);

        // 3. Tạo Scene (Cửa sổ nội dung)
        Scene scene = new Scene(root, 300, 250);

        // 4. Thiết lập Stage (Cửa sổ ứng dụng)
        primaryStage.setTitle("Ứng dụng JavaFX của tôi");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

