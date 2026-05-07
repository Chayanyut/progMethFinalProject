package game.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class Main extends Application {

    public static final int SCENE_WIDTH = 1024;
    public static final int SCENE_HEIGHT = 768;

    @Override
    public void start(Stage primaryStage) throws IOException {
        primaryStage.setTitle("Tycoon Game");

        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);

        FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/start_menu.fxml")));
        Parent root = loader.load();

        primaryStage.setScene(new Scene(root, SCENE_WIDTH, SCENE_HEIGHT));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}