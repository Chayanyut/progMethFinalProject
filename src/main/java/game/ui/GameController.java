package game.ui;

import game.logic.*;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.application.Platform;


import java.net.URL;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("CallToPrintStackTrace")
public class GameController implements Initializable {

    // ==========================================
    // Constants & Configuration
    // ==========================================
    private static final double TILE_SIZE = 50.0;
    private static final double LOGIC_INTERVAL_SEC = 0.5;
    private static final double PAN_SPEED_PX_PER_SEC = 280.0;

    // ==========================================
    // FXML UI Components
    // ==========================================
    @FXML private Canvas gameCanvas;
    @FXML private VBox shopPopup;
    @FXML private HBox inventoryBox;
    @FXML private Label moneyLabel;
    @FXML private Label shopHintLabel;
    @FXML private TilePane shopItemContainer;

    // ==========================================
    // Core Game Systems & Managers
    // ==========================================
    private final GridSystem logicGrid = new GridSystem(20, 20);
    private final PlayerBank bank = new PlayerBank(500.0);
    private final GameRenderer renderer = new GameRenderer();
    private final CameraManager camera = new CameraManager();
    private ShopManager shopManager; // Initialized in initialize()

    // ==========================================
    // User Input & Interaction State
    // ==========================================
    private final Set<KeyCode> activeKeys = ConcurrentHashMap.newKeySet();
    private Direction placementFacing = Direction.RIGHT;
    private double mouseWorldX;
    private double mouseWorldY;

    // ==========================================
    // Game Loop & Timing
    // ==========================================
    private AnimationTimer gameLoop;
    private ScheduledExecutorService logicThread;
    private long lastFrameNanos;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Initialize the new Shop Manager
        shopManager = new ShopManager(
                bank,
                inventoryBox,
                moneyLabel,
                renderer::imageForMachineType, // Tell ShopManager to ask the Renderer for images
                this::updateShopHint           // Callback to update hint text when inventory changes
        );
        generateShopButtons();

        shopManager.refreshUI();

        // 2. Attach Canvas Mouse Handlers (Required for placing and holograms)
        gameCanvas.setOnMouseClicked(this::handleCanvasClick);
        gameCanvas.setOnMouseMoved(this::handleCanvasMouseMove);

        // 3. Listen for scene changes to hook up keyboard/scroll events and start the loop
        gameCanvas.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null) {
                detachSceneHandlers(oldScene);
                if (gameLoop != null) {
                    gameLoop.stop();
                    gameLoop = null;
                }

                if (logicThread != null && !logicThread.isShutdown()) {
                    logicThread.shutdownNow();
                }
            }
            if (newScene != null) {
                setupControls(newScene);
                startGameLoop();
            }
        });
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
                renderer.render(gameCanvas, logicGrid, shopManager, mouseWorldX, mouseWorldY, placementFacing, shopPopup.isVisible());
            }
        };
        gameLoop.start();

        logicThread = Executors.newSingleThreadScheduledExecutor();
        long intervalMs = (long) (LOGIC_INTERVAL_SEC * 1000);

        logicThread.scheduleAtFixedRate(() -> {
            try {
                logicGrid.tick();
                Platform.runLater(() -> shopManager.refreshUI());
            } catch (Exception e) {
                System.err.println("Error in Logic Thread:");
                e.printStackTrace();
            }
        }, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
    }

    private Machine createMachine(MachineType s) {
        return s.create(placementFacing, bank);
    }

    @FXML
    void toggleShop() {
        boolean next = !shopPopup.isVisible();
        shopPopup.setVisible(next);
        if (next) {
            activeKeys.clear();
        }
    }

    private void generateShopButtons() {
        if (shopItemContainer == null) return;

        shopItemContainer.getChildren().clear();

        for (MachineType type : MachineType.values()) {
            if (type == MachineType.NONE) continue;

            Button buyBtn = new Button("Buy " + type.name() + " ($" + type.getCost() + ")");
            buyBtn.getStyleClass().add("shop-button"); // For your CSS!

            ImageView icon = new ImageView(renderer.imageForMachineType(type));
            icon.setFitWidth(32);
            icon.setFitHeight(32);
            buyBtn.setGraphic(icon);

            buyBtn.setOnAction(e -> {
                shopManager.attemptBuy(type);
            });
            
            shopItemContainer.getChildren().add(buyBtn);
        }
    }

    private void updateShopHint() {
        // Safety check to ensure the UI and ShopManager are ready
        if (shopHintLabel == null || shopManager == null) {
            return;
        }

        // Ask the ShopManager what is currently selected
        MachineType currentSelection = shopManager.getActiveSelection();
        int qty = shopManager.getInventoryCount(currentSelection);

        // Update the label text
        shopHintLabel.setText(
                currentSelection == MachineType.NONE
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

    private void handleCanvasClick(MouseEvent event) {
        if (shopPopup != null && shopPopup.isVisible()) return;
        MachineType selection = shopManager.getActiveSelection();
        if (selection == MachineType.NONE) return;
        if (shopManager.getInventoryCount(selection) <= 0) {
            shopManager.refreshUI();
            return;
        }

        int gx = (int) Math.floor(event.getX() / TILE_SIZE);
        int gy = (int) Math.floor(event.getY() / TILE_SIZE);

        if (!logicGrid.isInside(gx, gy)) return;
        Machine toPlace = createMachine(selection);
        if (logicGrid.placeMachine(gx, gy, toPlace)) {
            shopManager.consumeFromInventory(selection);
        }
    }

    private void handleCanvasMouseMove(MouseEvent event) {
        if (shopPopup != null && shopPopup.isVisible()) return;
        mouseWorldX = event.getX();
        mouseWorldY = event.getY();
    }
}
