package game.ui;

import game.logic.Conveyor;
import game.logic.Direction;
import game.logic.Dropper;
import game.logic.Furnace;
import game.logic.GridSystem;
import game.logic.Machine;
import game.logic.PlayerBank;
import game.logic.Upgrader;
import javafx.animation.AnimationTimer;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.util.Duration;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GameController implements Initializable {

    private static final double TILE_SIZE = 50.0;
    private static final double ZOOM_MIN = 0.3;
    private static final double ZOOM_MAX = 3.0;
    private static final double LOGIC_INTERVAL_SEC = 0.5;
    private static final double PAN_SPEED_PX_PER_SEC = 280.0;

    private static final double COST_DROPPER = 50.0;
    private static final double COST_CONVEYOR = 10.0;
    private static final double COST_UPGRADER = 100.0;
    private static final double COST_FURNACE = 200.0;

    private static final double SHOP_OFFSET_HIDDEN = -250.0;
    private static final double SHOP_OFFSET_VISIBLE = 0.0;

    @FXML
    private Canvas gameCanvas;
    @FXML
    private VBox shopPanel;
    @FXML
    private Label moneyLabel;
    @FXML
    private Label shopHintLabel;

    private final GridSystem logicGrid = new GridSystem(20, 20);
    private final PlayerBank bank = new PlayerBank(500.0);

    private final Map<String, Image> imageCache = new HashMap<>();

    private double cameraX = 0.0;
    private double cameraY = 0.0;
    private double zoomLevel = 1.0;

    private final Set<KeyCode> activeKeys = ConcurrentHashMap.newKeySet();

    private enum ShopSelection {
        NONE, DROPPER, CONVEYOR, UPGRADER, FURNACE
    }

    private ShopSelection shopSelection = ShopSelection.NONE;

    private AnimationTimer gameLoop;
    private long lastFrameNanos;
    private long lastLogicTickNanos;
    private boolean shopVisible;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        preloadImages();
        gameCanvas.setOnMouseClicked(this::handleCanvasClick);

        gameCanvas.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null) {
                detachSceneHandlers(oldScene);
                if (gameLoop != null) {
                    gameLoop.stop();
                    gameLoop = null;
                }
            }
            if (newScene != null) {
                setupControls(newScene);
                startGameLoop();
            }
        });

        updateMoneyLabel();
        updateShopHint();
    }

    private void preloadImages() {
        imageCache.put("dropper.png", getImage("dropper.png", "Dr"));
        imageCache.put("conveyor.png", getImage("conveyor.png", "Cv"));
        imageCache.put("upgrader.png", getImage("upgrader.png", "Up"));
        imageCache.put("furnace.png", getImage("furnace.png", "Fn"));
        imageCache.put("item.png", getImage("item.png", "It"));
    }

    /**
     * Loads an image from {@code /images/} or returns a small placeholder with {@code fallbackText}.
     */
    private Image getImage(String filename, String fallbackText) {
        String path = "/images/" + filename;
        try (InputStream stream = GameController.class.getResourceAsStream(path)) {
            if (stream != null) {
                Image loaded = new Image(stream);
                if (!loaded.isError()) {
                    return loaded;
                }
            }
        } catch (Exception ignored) {
            // use placeholder
        }
        return createPlaceholderImage(fallbackText);
    }

    private Image createPlaceholderImage(String text) {
        int w = 50;
        int h = 50;
        Canvas c = new Canvas(w, h);
        GraphicsContext g = c.getGraphicsContext2D();
        g.setFill(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.setStroke(Color.BLACK);
        g.strokeRect(0.5, 0.5, w - 1, h - 1);
        g.setFill(Color.BLACK);
        g.setFont(Font.font(11));
        String t = text == null ? "?" : text;
        g.fillText(t, 6, 28);
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        WritableImage snap = c.snapshot(params, null);
        return snap == null ? new WritableImage(w, h) : snap;
    }

    private void setupControls(Scene scene) {
        scene.addEventHandler(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
        scene.addEventHandler(KeyEvent.KEY_RELEASED, this::handleKeyReleased);
        scene.addEventHandler(ScrollEvent.SCROLL, this::handleScroll);
    }

    private void detachSceneHandlers(Scene scene) {
        scene.removeEventHandler(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
        scene.removeEventHandler(KeyEvent.KEY_RELEASED, this::handleKeyReleased);
        scene.removeEventHandler(ScrollEvent.SCROLL, this::handleScroll);
    }

    private void handleKeyPressed(KeyEvent event) {
        activeKeys.add(event.getCode());
    }

    private void handleKeyReleased(KeyEvent event) {
        activeKeys.remove(event.getCode());
    }

    private void handleScroll(ScrollEvent event) {
        double factor = event.getDeltaY() > 0 ? 1.08 : 1 / 1.08;
        zoomLevel = zoomLevel * factor;
        zoomLevel = Math.min(ZOOM_MAX, Math.max(ZOOM_MIN, zoomLevel));
        event.consume();
    }

    private void startGameLoop() {
        lastFrameNanos = 0;
        lastLogicTickNanos = 0;

        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double dtSec = 0.0;
                if (lastFrameNanos != 0) {
                    dtSec = (now - lastFrameNanos) / 1_000_000_000.0;
                }
                lastFrameNanos = now;

                if (dtSec > 0) {
                    double pan = PAN_SPEED_PX_PER_SEC * dtSec;
                    if (activeKeys.contains(KeyCode.W)) {
                        cameraY += pan;
                    }
                    if (activeKeys.contains(KeyCode.S)) {
                        cameraY -= pan;
                    }
                    if (activeKeys.contains(KeyCode.A)) {
                        cameraX += pan;
                    }
                    if (activeKeys.contains(KeyCode.D)) {
                        cameraX -= pan;
                    }
                }

                gameCanvas.getTransforms().clear();
                gameCanvas.getTransforms().addAll(
                        new Scale(zoomLevel, zoomLevel, 0, 0),
                        new Translate(cameraX, cameraY)
                );

                if (lastLogicTickNanos == 0) {
                    lastLogicTickNanos = now;
                }
                if ((now - lastLogicTickNanos) / 1_000_000_000.0 >= LOGIC_INTERVAL_SEC) {
                    logicGrid.tick();
                    lastLogicTickNanos = now;
                    updateMoneyLabel();
                }

                renderGraphics();
            }
        };
        gameLoop.start();
    }

    private void renderGraphics() {
        double cw = gameCanvas.getWidth();
        double ch = gameCanvas.getHeight();
        GraphicsContext gc = gameCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, cw, ch);

        gc.setStroke(Color.color(0.25, 0.28, 0.32));
        gc.setLineWidth(1);
        int maxX = (int) Math.ceil(cw / TILE_SIZE);
        int maxY = (int) Math.ceil(ch / TILE_SIZE);
        for (int gx = 0; gx <= maxX; gx++) {
            double x = gx * TILE_SIZE;
            gc.strokeLine(x, 0, x, ch);
        }
        for (int gy = 0; gy <= maxY; gy++) {
            double y = gy * TILE_SIZE;
            gc.strokeLine(0, y, cw, y);
        }

        Image itemImg = imageCache.get("item.png");

        for (int x = 0; x < logicGrid.getWidth(); x++) {
            for (int y = 0; y < logicGrid.getHeight(); y++) {
                Machine m = logicGrid.getMachine(x, y);
                if (m == null) {
                    continue;
                }
                double px = x * TILE_SIZE;
                double py = y * TILE_SIZE;

                Image machineImg = imageForMachine(m);
                drawImageRotated(gc, machineImg, px, py, TILE_SIZE, TILE_SIZE, facingAngle(m.getFacing()));

                if (m.getCurrentItem() != null) {
                    double inset = TILE_SIZE * 0.2;
                    drawImageRotated(
                            gc,
                            itemImg,
                            px + inset * 0.5,
                            py + inset * 0.5,
                            TILE_SIZE - inset,
                            TILE_SIZE - inset,
                            0
                    );
                }
            }
        }
    }

    private Image imageForMachine(Machine m) {
        if (m instanceof Upgrader) {
            return imageCache.get("upgrader.png");
        }
        if (m instanceof Dropper) {
            return imageCache.get("dropper.png");
        }
        if (m instanceof Furnace) {
            return imageCache.get("furnace.png");
        }
        if (m instanceof Conveyor) {
            return imageCache.get("conveyor.png");
        }
        return imageCache.get("conveyor.png");
    }

    private double facingAngle(Direction d) {
        return switch (d) {
            case RIGHT -> 0;
            case DOWN -> 90;
            case LEFT -> 180;
            case UP -> 270;
        };
    }

    private void drawImageRotated(GraphicsContext gc, Image img, double x, double y, double w, double h, double degrees) {
        if (img == null) {
            return;
        }
        double cx = x + w / 2.0;
        double cy = y + h / 2.0;
        gc.save();
        gc.translate(cx, cy);
        gc.rotate(degrees);
        gc.drawImage(img, -w / 2.0, -h / 2.0, w, h);
        gc.restore();
    }

    private void handleCanvasClick(MouseEvent event) {
        if (shopSelection == ShopSelection.NONE) {
            return;
        }
        Point2D local = gameCanvas.sceneToLocal(event.getSceneX(), event.getSceneY());
        double worldX = (local.getX() - cameraX) / zoomLevel;
        double worldY = (local.getY() - cameraY) / zoomLevel;
        int gx = (int) Math.floor(worldX / TILE_SIZE);
        int gy = (int) Math.floor(worldY / TILE_SIZE);

        if (!logicGrid.isInside(gx, gy)) {
            return;
        }

        double cost = costForSelection(shopSelection);
        if (!bank.trySpend(cost)) {
            return;
        }

        Machine toPlace = createMachine(shopSelection);
        if (!logicGrid.placeMachine(gx, gy, toPlace)) {
            bank.deposit(cost);
            return;
        }

        shopSelection = ShopSelection.NONE;
        updateShopHint();
        updateMoneyLabel();
    }

    private double costForSelection(ShopSelection s) {
        return switch (s) {
            case DROPPER -> COST_DROPPER;
            case CONVEYOR -> COST_CONVEYOR;
            case UPGRADER -> COST_UPGRADER;
            case FURNACE -> COST_FURNACE;
            case NONE -> 0.0;
        };
    }

    private Machine createMachine(ShopSelection s) {
        Direction face = Direction.RIGHT;
        return switch (s) {
            case DROPPER -> new Dropper(COST_DROPPER, face, 10.0);
            case CONVEYOR -> new Conveyor(COST_CONVEYOR, face);
            case UPGRADER -> new Upgrader(COST_UPGRADER, face, 2.0);
            case FURNACE -> new Furnace(COST_FURNACE, face, bank);
            case NONE -> throw new IllegalStateException();
        };
    }

    @FXML
    void toggleShop() {
        double from = shopPanel.getTranslateX();
        double to = shopVisible ? SHOP_OFFSET_HIDDEN : SHOP_OFFSET_VISIBLE;
        shopVisible = !shopVisible;

        TranslateTransition tt = new TranslateTransition(Duration.millis(220), shopPanel);
        tt.setFromX(from);
        tt.setToX(to);
        tt.setInterpolator(Interpolator.EASE_BOTH);
        tt.play();
    }

    @FXML
    void buyDropper() {
        shopSelection = ShopSelection.DROPPER;
        updateShopHint();
    }

    @FXML
    void buyConveyor() {
        shopSelection = ShopSelection.CONVEYOR;
        updateShopHint();
    }

    @FXML
    void buyUpgrader() {
        shopSelection = ShopSelection.UPGRADER;
        updateShopHint();
    }

    @FXML
    void buyFurnace() {
        shopSelection = ShopSelection.FURNACE;
        updateShopHint();
    }

    private void updateMoneyLabel() {
        moneyLabel.setText(String.format("Balance: $%.0f", bank.getBalance()));
    }

    private void updateShopHint() {
        if (shopHintLabel == null) {
            return;
        }
        shopHintLabel.setText(
                shopSelection == ShopSelection.NONE
                        ? "Select a machine, then click the grid to place."
                        : "Selected: " + shopSelection + " — click the grid to place (facing right)."
        );
    }
}
