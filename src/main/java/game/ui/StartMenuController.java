package game.ui;

import game.Main;
import javafx.animation.ScaleTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public class StartMenuController implements Initializable {

    @FXML private Pane      backgroundPane;
    @FXML private Button    playButton;
    @FXML private Button    quitButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadBackground();
        setupHoverEffects();
    }

    // ==========================================
    // Background
    // ==========================================

    private void loadBackground() {
        try (InputStream stream = StartMenuController.class
                .getResourceAsStream("/images/environment/startMenuBackground.JPG")) {
            if (stream != null) {
                Image img = new Image(stream);
                if (!img.isError()) {
                    // This configuration perfectly mimics CSS "background-size: cover"
                    BackgroundImage bgImg = new BackgroundImage(
                            img,
                            BackgroundRepeat.NO_REPEAT,
                            BackgroundRepeat.NO_REPEAT,
                            BackgroundPosition.CENTER,
                            new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, false, true)
                    );
                    backgroundPane.setBackground(new Background(bgImg));
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not load start menu background: " + e.getMessage());
        }
    }

    // ==========================================
    // Hover Effects
    // ==========================================

    private void setupHoverEffects() {
        setupPlayButtonHover();
        setupQuitButtonHover();
    }

    private void setupPlayButtonHover() {
        String base = "-fx-background-color: #e8a020;" +
                "-fx-text-fill: #0a0c0f;" +
                "-fx-font-family: 'Courier New';" +
                "-fx-font-size: 18px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 2;" +
                "-fx-cursor: hand;";

        String hovered = "-fx-background-color: #ffb830;" +
                "-fx-text-fill: #0a0c0f;" +
                "-fx-font-family: 'Courier New';" +
                "-fx-font-size: 18px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 2;" +
                "-fx-cursor: hand;" +
                "-fx-effect: dropshadow(gaussian, rgba(255,184,48,0.8), 24, 0.4, 0, 0);";

        playButton.setOnMouseEntered(e -> {
            playButton.setStyle(hovered);
            scaleButton(playButton, 1.0, 1.04);
        });
        playButton.setOnMouseExited(e -> {
            playButton.setStyle(base +
                    "-fx-effect: dropshadow(gaussian, rgba(232,160,32,0.55), 18, 0.3, 0, 0);");
            scaleButton(playButton, 1.04, 1.0);
        });
    }

    private void setupQuitButtonHover() {
        String base = "-fx-background-color: transparent;" +
                "-fx-text-fill: #a0a8b0;" +
                "-fx-font-family: 'Courier New';" +
                "-fx-font-size: 18px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 2;" +
                "-fx-border-color: #3a4450;" +
                "-fx-border-width: 1.5;" +
                "-fx-border-radius: 2;" +
                "-fx-cursor: hand;";

        String hovered = "-fx-background-color: rgba(180,50,40,0.15);" +
                "-fx-text-fill: #e05545;" +
                "-fx-font-family: 'Courier New';" +
                "-fx-font-size: 18px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 2;" +
                "-fx-border-color: #e05545;" +
                "-fx-border-width: 1.5;" +
                "-fx-border-radius: 2;" +
                "-fx-cursor: hand;";

        quitButton.setOnMouseEntered(e -> {
            quitButton.setStyle(hovered);
            scaleButton(quitButton, 1.0, 1.04);
        });
        quitButton.setOnMouseExited(e -> {
            quitButton.setStyle(base);
            scaleButton(quitButton, 1.04, 1.0);
        });
    }

    private void scaleButton(Button button, double from, double to) {
        ScaleTransition st = new ScaleTransition(Duration.millis(100), button);
        st.setFromX(from);
        st.setFromY(from);
        st.setToX(to);
        st.setToY(to);
        st.play();
    }

    // ==========================================
    // Actions
    // ==========================================

    @FXML
    void startGame(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(Objects.requireNonNull(
                getClass().getResource("/layout.fxml")));

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        double width  = stage.getWidth()  > 0 ? stage.getWidth()  : Main.SCENE_WIDTH;
        double height = stage.getHeight() > 0 ? stage.getHeight() : Main.SCENE_HEIGHT;

        stage.setScene(new Scene(root, width, height));
    }

    @FXML
    void quitGame(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
        javafx.application.Platform.exit();
    }
}