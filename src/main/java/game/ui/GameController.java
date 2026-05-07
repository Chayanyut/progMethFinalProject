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
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

import java.io.InputStream;
import java.net.URL;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GameController implements Initializable {

    private static final double TILE_SIZE = 50.0;
    private static final double LOGIC_INTERVAL_SEC = 0.5;
    private static final double PAN_SPEED_PX_PER_SEC = 280.0;

    private ShopManager shopManager;

    @FXML
    private Canvas gameCanvas;
    @FXML
    private VBox shopPopup;
    @FXML
    private Label moneyLabel;
    @FXML
    private Label shopHintLabel;
    @FXML
    private HBox inventoryBox;

    private final GridSystem logicGrid = new GridSystem(20, 20);

    private final PlayerBank bank = new PlayerBank(500.0);

    private final GameRenderer renderer = new GameRenderer();

    private final CameraManager camera = new CameraManager();

    private final Set<KeyCode> activeKeys = ConcurrentHashMap.newKeySet();

    private Direction placementFacing = Direction.RIGHT;

    private double mouseWorldX;
    private double mouseWorldY;

    private AnimationTimer gameLoop;
    private long lastFrameNanos;
    private long lastLogicTickNanos;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        preloadImages();

        // Initialize the new Shop Manager
        shopManager = new ShopManager(
                bank,
                inventoryBox,
                moneyLabel,
                this::imageForMachineType, // We will rename imageForSelection to this
                this::updateShopHint       // Tells the hint label to update when inventory changes
        );
        shopManager.refreshUI();

        gameCanvas.setOnMouseClicked(this::handleCanvasClick);
        gameCanvas.setOnMouseMoved(this::handleCanvasMouseMove);

        // ... (Keep the sceneProperty listener exactly the same) ...

        updateShopHint();
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
        KeyCode code = event.getCode();

        if (shopPopup != null && shopPopup.isVisible()) {
            if (code == KeyCode.R) {
                cyclePlacementFacing();
                updateShopHint();
            } else if (code == KeyCode.B) {
                toggleShop();
            }
            return;
        }

        if (code == KeyCode.B) {
            toggleShop();
            return;
        }
        if (code == KeyCode.R) {
            cyclePlacementFacing();
            updateShopHint();
            return;
        }

        activeKeys.add(code);
    }

    private void handleKeyReleased(KeyEvent event) {
        if (shopPopup != null && shopPopup.isVisible()) {
            return;
        }
        activeKeys.remove(event.getCode());
    }

    private void handleScroll(ScrollEvent event) {
        if (shopPopup != null && shopPopup.isVisible()) return;
        double factor = event.getDeltaY() > 0 ? 1.08 : 1 / 1.08;
        camera.applyZoom(factor);
        event.consume();
    }

    private void startGameLoop() {
        lastFrameNanos = 0;
        lastLogicTickNanos = 0;

        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double dtSec = (lastFrameNanos == 0) ? 0 : (now - lastFrameNanos) / 1_000_000_000.0;
                lastFrameNanos = now;

                if (dtSec > 0) {
                    double pan = PAN_SPEED_PX_PER_SEC * dtSec;
                    double dx = 0, dy = 0;
                    if (activeKeys.contains(KeyCode.W)) dy += pan;
                    if (activeKeys.contains(KeyCode.S)) dy -= pan;
                    if (activeKeys.contains(KeyCode.A)) dx += pan;
                    if (activeKeys.contains(KeyCode.D)) dx -= pan;
                    camera.pan(dx, dy);
                }

                camera.applyTransformsAndClamp(gameCanvas, logicGrid.getWidth() * TILE_SIZE, logicGrid.getHeight() * TILE_SIZE);

                if (lastLogicTickNanos == 0) lastLogicTickNanos = now;
                if ((now - lastLogicTickNanos) / 1_000_000_000.0 >= LOGIC_INTERVAL_SEC) {
                    logicGrid.tick();
                    lastLogicTickNanos = now;
                    shopManager.refreshUI(); // <--- This replaces the old updateMoneyLabel()!
                }

                renderGraphics();
            }
        };
        gameLoop.start();
    }

    private void handleCanvasClick(MouseEvent event) {
        if (shopPopup != null && shopPopup.isVisible()) return;

        ShopManager.MachineType selection = shopManager.getActiveSelection();
        if (selection == ShopManager.MachineType.NONE) return;

        if (shopManager.getInventoryCount(selection) <= 0) {
            shopManager.refreshUI();
            return;
        }

        Point2D local = gameCanvas.sceneToLocal(event.getSceneX(), event.getSceneY());
        double worldX = camera.screenToWorldX(local.getX());
        double worldY = camera.screenToWorldY(local.getY());
        int gx = (int) Math.floor(worldX / TILE_SIZE);
        int gy = (int) Math.floor(worldY / TILE_SIZE);

        if (!logicGrid.isInside(gx, gy)) return;

        Machine toPlace = createMachine(selection);
        if (logicGrid.placeMachine(gx, gy, toPlace)) {
            shopManager.consumeFromInventory(selection);
        }
    }


    private Machine createMachine(ShopManager.MachineType s) {
        Direction face = placementFacing;
        return switch (s) {
            case DROPPER -> new Dropper(s.getCost(), face, 10.0);
            case CONVEYOR -> new Conveyor(s.getCost(), face);
            case UPGRADER -> new Upgrader(s.getCost(), face, 2.0);
            case FURNACE -> new Furnace(s.getCost(), face, bank);
            case NONE -> throw new IllegalStateException();
        };
    }

    @FXML
    void toggleShop() {
        boolean next = !shopPopup.isVisible();
        shopPopup.setVisible(next);
        if (next) {
            activeKeys.clear();
        }
    }

    @FXML void buyDropper() { shopManager.attemptBuy(ShopManager.MachineType.DROPPER); }
    @FXML void buyConveyor() { shopManager.attemptBuy(ShopManager.MachineType.CONVEYOR); }
    @FXML void buyUpgrader() { shopManager.attemptBuy(ShopManager.MachineType.UPGRADER); }
    @FXML void buyFurnace() { shopManager.attemptBuy(ShopManager.MachineType.FURNACE); }

    private void updateShopHint() {
        // Safety check to ensure the UI and ShopManager are ready
        if (shopHintLabel == null || shopManager == null) {
            return;
        }

        // Ask the ShopManager what is currently selected
        ShopManager.MachineType currentSelection = shopManager.getActiveSelection();
        int qty = shopManager.getInventoryCount(currentSelection);

        // Update the label text
        shopHintLabel.setText(
                currentSelection == ShopManager.MachineType.NONE
                        ? "Pick a slot from inventory. Facing: " + placementFacing + " (R rotates)."
                        : "Selected: " + currentSelection + " (x" + qty + ") | Facing: " + placementFacing + " (R rotates)"
        );
    }

    private void cyclePlacementFacing() {
        placementFacing = switch (placementFacing) {
            case RIGHT -> Direction.DOWN;
            case DOWN -> Direction.LEFT;
            case LEFT -> Direction.UP;
            case UP -> Direction.RIGHT;
        };
    }

    private void handleCanvasMouseMove(MouseEvent event) {
        if (shopPopup != null && shopPopup.isVisible()) return;
        Point2D local = gameCanvas.sceneToLocal(event.getSceneX(), event.getSceneY());
        mouseWorldX = camera.screenToWorldX(local.getX());
        mouseWorldY = camera.screenToWorldY(local.getY());
    }
}
