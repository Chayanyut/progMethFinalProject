package game.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public class StartMenuController implements Initializable {

    @FXML
    private ImageView backgroundImageView;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try (InputStream stream = StartMenuController.class.getResourceAsStream("/images/menu_bg.png")) {
            if (stream != null) {
                Image img = new Image(stream);
                if (!img.isError()) {
                    backgroundImageView.setImage(img);
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not load start menu background: " + e.getMessage());
        }
    }

    @FXML
    void startGame(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/layout.fxml")));

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        double width = stage.getWidth() > 0 ? stage.getWidth() : Main.SCENE_WIDTH;
        double height = stage.getHeight() > 0 ? stage.getHeight() : Main.SCENE_HEIGHT;

        Scene newScene = new Scene(root, width, height);
        stage.setScene(newScene);
    }
}