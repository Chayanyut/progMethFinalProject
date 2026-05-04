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
    private static final double ZOOM_MIN = 0.5;
    private static final double ZOOM_MAX = 2.0;
    private static final double LOGIC_INTERVAL_SEC = 0.5;
    private static final double PAN_SPEED_PX_PER_SEC = 280.0;

    private static final double COST_DROPPER = 50.0;
    private static final double COST_CONVEYOR = 10.0;
    private static final double COST_UPGRADER = 100.0;
    private static final double COST_FURNACE = 200.0;

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

    private final Map<String, Image> imageCache = new HashMap<>();

    private double cameraX = 0.0;
    private double cameraY = 0.0;
    private double zoomLevel = 1.0;

    private final Set<KeyCode> activeKeys = ConcurrentHashMap.newKeySet();

    private Direction placementFacing = Direction.RIGHT;

    private enum ShopSelection {
        NONE, DROPPER, CONVEYOR, UPGRADER, FURNACE
    }

    private ShopSelection shopSelection = ShopSelection.NONE;

    private final Map<ShopSelection, Integer> inventory = new EnumMap<>(ShopSelection.class);

    private double mouseWorldX;
    private double mouseWorldY;

    private AnimationTimer gameLoop;
    private long lastFrameNanos;
    private long lastLogicTickNanos;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        preloadImages();
        gameCanvas.setOnMouseClicked(this::handleCanvasClick);
        gameCanvas.setOnMouseMoved(this::handleCanvasMouseMove);

        for (ShopSelection s : ShopSelection.values()) {
            if (s != ShopSelection.NONE) {
                inventory.put(s, 0);
            }
        }
        updateInventoryUI();

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
        if (shopPopup != null && shopPopup.isVisible()) {
            return;
        }
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

                // Calculate the actual visual size of the grid on the screen
                double worldW = logicGrid.getWidth() * TILE_SIZE * zoomLevel;
                double worldH = logicGrid.getHeight() * TILE_SIZE * zoomLevel;

                // Clamp camera so it can't be pushed entirely off-screen
                // 800 and 600 act as our max positive pan (pushing the grid right/down)
                // -worldW + 200 ensures at least 200px of the grid stays visible on the left/top
                cameraX = Math.max(-worldW + 200, Math.min(800, cameraX));
                cameraY = Math.max(-worldH + 200, Math.min(600, cameraY));

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

        double worldW = logicGrid.getWidth() * TILE_SIZE;
        double worldH = logicGrid.getHeight() * TILE_SIZE;
        gc.setFill(Color.web("#7f8c8d"));
        gc.fillRect(0, 0, worldW, worldH);

        gc.setStroke(Color.color(0.25, 0.28, 0.32));
        gc.setLineWidth(1);
        int maxX = logicGrid.getWidth();
        int maxY = logicGrid.getHeight();
        for (int gx = 0; gx <= maxX; gx++) {
            double x = gx * TILE_SIZE;
            gc.strokeLine(x, 0, x, worldH);
        }
        for (int gy = 0; gy <= maxY; gy++) {
            double y = gy * TILE_SIZE;
            gc.strokeLine(0, y, worldW, y);
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

        renderHologram(gc);
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
        if (shopPopup != null && shopPopup.isVisible()) {
            return;
        }
        if (shopSelection == ShopSelection.NONE) {
            return;
        }

        Integer count = inventory.get(shopSelection);
        if (count == null || count <= 0) {
            updateInventoryUI();
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

        Machine toPlace = createMachine(shopSelection);
        if (!logicGrid.placeMachine(gx, gy, toPlace)) {
            return;
        }

        inventory.put(shopSelection, count - 1);
        updateInventoryUI();
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
        Direction face = placementFacing;
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
        boolean next = !shopPopup.isVisible();
        shopPopup.setVisible(next);
        if (next) {
            activeKeys.clear();
        }
    }

    @FXML
    void buyDropper() {
        buyToInventory(ShopSelection.DROPPER, COST_DROPPER);
    }

    @FXML
    void buyConveyor() {
        buyToInventory(ShopSelection.CONVEYOR, COST_CONVEYOR);
    }

    @FXML
    void buyUpgrader() {
        buyToInventory(ShopSelection.UPGRADER, COST_UPGRADER);
    }

    @FXML
    void buyFurnace() {
        buyToInventory(ShopSelection.FURNACE, COST_FURNACE);
    }

    private void updateMoneyLabel() {
        moneyLabel.setText(String.format("Balance: $%.0f", bank.getBalance()));
    }

    private void updateShopHint() {
        if (shopHintLabel == null) {
            return;
        }
        int qty = shopSelection == ShopSelection.NONE ? 0 : inventory.getOrDefault(shopSelection, 0);
        shopHintLabel.setText(
                shopSelection == ShopSelection.NONE
                        ? "Pick a slot from inventory. Facing: " + placementFacing + " (R rotates)."
                        : "Selected: " + shopSelection + " (x" + qty + ") | Facing: " + placementFacing + " (R rotates)"
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
        if (shopPopup != null && shopPopup.isVisible()) {
            return;
        }
        Point2D local = gameCanvas.sceneToLocal(event.getSceneX(), event.getSceneY());
        mouseWorldX = (local.getX() - cameraX) / zoomLevel;
        mouseWorldY = (local.getY() - cameraY) / zoomLevel;
    }

    private void buyToInventory(ShopSelection type, double cost) {
        if (bank.trySpend(cost)) {
            inventory.put(type, inventory.getOrDefault(type, 0) + 1);
            updateMoneyLabel();
            updateInventoryUI();
            updateShopHint();
        }
    }

    private void updateInventoryUI() {
        if (inventoryBox == null) {
            return;
        }
        inventoryBox.getChildren().clear();

        for (ShopSelection type : ShopSelection.values()) {
            if (type == ShopSelection.NONE) {
                continue;
            }
            int qty = inventory.getOrDefault(type, 0);

            VBox slot = new VBox(4);
            slot.getStyleClass().add("inventory-slot");
            if (qty == 0) {
                slot.getStyleClass().add("inventory-slot-empty");
            }
            if (shopSelection == type) {
                slot.getStyleClass().add("inventory-slot-active");
            }

            ImageView icon = new ImageView(imageForSelection(type));
            icon.setFitWidth(40);
            icon.setFitHeight(40);
            icon.setPreserveRatio(true);

            Label qtyLabel = new Label("x" + qty);
            qtyLabel.setTextFill(Color.web("#ecf0f1"));
            qtyLabel.setFont(Font.font(12));

            slot.getChildren().addAll(icon, qtyLabel);
            slot.setOnMouseClicked(e -> {
                if (shopSelection == type) {
                    shopSelection = ShopSelection.NONE;
                } else {
                    shopSelection = type;
                }
                updateInventoryUI();
                updateShopHint();
            });

            VBox.setVgrow(icon, Priority.NEVER);
            inventoryBox.getChildren().add(slot);
        }
    }

    private Image imageForSelection(ShopSelection s) {
        return switch (s) {
            case DROPPER -> imageCache.get("dropper.png");
            case CONVEYOR -> imageCache.get("conveyor.png");
            case UPGRADER -> imageCache.get("upgrader.png");
            case FURNACE -> imageCache.get("furnace.png");
            case NONE -> imageCache.get("conveyor.png");
        };
    }

    private void renderHologram(GraphicsContext gc) {
        if (shopPopup != null && shopPopup.isVisible()) {
            return;
        }
        if (shopSelection == ShopSelection.NONE) {
            return;
        }

        int gx = (int) Math.floor(mouseWorldX / TILE_SIZE);
        int gy = (int) Math.floor(mouseWorldY / TILE_SIZE);

        boolean inside = logicGrid.isInside(gx, gy);
        boolean empty = inside && logicGrid.getMachine(gx, gy) == null;
        int qty = inventory.getOrDefault(shopSelection, 0);

        boolean valid = inside && empty && qty > 0;

        if (!inside) {
            return;
        }

        double px = gx * TILE_SIZE;
        double py = gy * TILE_SIZE;

        gc.save();

        // 1. Draw the ghost image first
        gc.setGlobalAlpha(0.4);
        Image img = imageForSelection(shopSelection);
        drawImageRotated(gc, img, px, py, TILE_SIZE, TILE_SIZE, facingAngle(placementFacing));

        // 2. Paint a colored tint box exactly over the tile to guarantee color
        if (valid) {
            gc.setFill(Color.web("#3498db", 0.4)); // Blue tint, 40% opacity
        } else {
            gc.setFill(Color.web("#e74c3c", 0.5)); // Red tint, 50% opacity
        }

        // Draw the tint box
        gc.fillRect(px, py, TILE_SIZE, TILE_SIZE);

        gc.restore();
    }
}
