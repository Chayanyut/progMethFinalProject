package game.ui;

import game.logic.*;
import game.ui.sound.SoundEffect;
import game.ui.sound.SoundManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.input.KeyCode;

import java.net.URL;
import java.util.ResourceBundle;

@SuppressWarnings("CallToPrintStackTrace")
public class GameController implements Initializable {

    // ==========================================
    // Constants
    // ==========================================
    private static final double TILE_SIZE = GameConstants.TILE_SIZE;

    // ==========================================
    // FXML UI Components
    // ==========================================
    @FXML private Canvas  gameCanvas;
    @FXML private VBox    shopPopup;
    @FXML private HBox    inventoryBar;
    @FXML private TabPane inventoryTabPane;
    @FXML private Label   moneyLabel;
    @FXML private Label   shopHintLabel;
    @FXML private TabPane shopTabPane;
    @FXML private Button  buildModeButton;
    @FXML private Button  removeModeButton;

    // ==========================================
    // Core Systems
    // ==========================================
    private final GridSystem    logicGrid = new GridSystem(20, 20);
    private final PlayerBank    bank      = new PlayerBank(500.0);
    private final GameRenderer  renderer  = new GameRenderer();
    private final CameraManager camera    = new CameraManager();
    private final SoundManager sound     = new SoundManager();

    // ==========================================
    // Managers
    // ==========================================
    private ShopManager      shopManager;
    private InputHandler     inputHandler;
    private GameLoopManager  gameLoopManager;
    private PlacementManager placementManager;

    // ==========================================
    // Deposit sound — balance-diff approach
    // ==========================================
    private double previousBalance;

    // ==========================================
    // Initialization
    // ==========================================
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        previousBalance = bank.getBalance();
        initShop();
        initPlacement();
        initInput();
        initGameLoop();
        bindCanvas();
        bindSceneLifecycle();
        sound.playMusic();
    }

    // ==========================================
    // Wiring
    // ==========================================

    private void initShop() {
        shopManager = new ShopManager(
                bank,
                moneyLabel,
                renderer::imageForMachineType,
                () -> { if (placementManager != null) placementManager.updateHint(); }
        );
        ShopUIBuilder.build(shopTabPane, renderer::imageForMachineType, shopManager::attemptBuy);
        shopManager.initInventoryUI(inventoryTabPane);
        shopManager.refreshUI();
    }

    private void initPlacement() {
        placementManager = new PlacementManager(
                logicGrid,
                bank,
                () -> shopPopup != null && shopPopup.isVisible(),
                shopManager::getActiveSelection,
                shopManager::getInventoryCount,
                shopManager::consumeFromInventory,
                shopManager::returnToInventory,
                shopManager::refreshUI,
                text -> { if (shopHintLabel != null) shopHintLabel.setText(text); },
                () -> sound.playSfx(SoundEffect.PLACE),
                () -> sound.playSfx(SoundEffect.REMOVE)
        );
    }

    private void initInput() {
        inputHandler = new InputHandler(
                this::toggleShop,
                this::toggleInventory,
                this::setBuildMode,
                this::setRemoveMode,
                placementManager::cyclePlacementFacing,
                camera::applyZoom,
                () -> shopPopup != null && shopPopup.isVisible(),
                () -> inventoryBar != null && inventoryBar.isVisible()
        );
    }

    private void initGameLoop() {
        gameLoopManager = new GameLoopManager(
                this::onRenderFrame,
                this::onLogicTick
        );
    }

    private void bindCanvas() {
        gameCanvas.setOnMouseClicked(placementManager::handleCanvasClick);
        gameCanvas.setOnMouseMoved(placementManager::handleCanvasMouseMove);
    }

    private void bindSceneLifecycle() {
        gameCanvas.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null) {
                inputHandler.detach(oldScene);
                gameLoopManager.stop();
                sound.stopMusic();
            }
            if (newScene != null) {
                inputHandler.attach(newScene);
                gameLoopManager.start();
                sound.playMusic();
            }
        });
    }

    // ==========================================
    // Game Loop Callbacks
    // ==========================================

    private void onRenderFrame(double dtSec) {
        if (dtSec > 0) {
            double pan = GameLoopManager.PAN_SPEED_PX_PER_SEC * dtSec;
            double dx = 0, dy = 0;
            if (inputHandler.getActiveKeys().contains(KeyCode.W)) dy += pan;
            if (inputHandler.getActiveKeys().contains(KeyCode.S)) dy -= pan;
            if (inputHandler.getActiveKeys().contains(KeyCode.A)) dx += pan;
            if (inputHandler.getActiveKeys().contains(KeyCode.D)) dx -= pan;
            camera.pan(dx, dy);
        }

        double worldW = logicGrid.getWidth()  * TILE_SIZE;
        double worldH = logicGrid.getHeight() * TILE_SIZE;
        camera.applyTransformsAndClamp(gameCanvas, worldW, worldH);

        renderer.render(
                gameCanvas, logicGrid, shopManager,
                placementManager.getMouseWorldX(),
                placementManager.getMouseWorldY(),
                placementManager.getPlacementFacing(),
                shopPopup.isVisible(),
                inventoryBar.isVisible(),
                placementManager.getPlacementMode()
        );
    }

    private void onLogicTick() {
        logicGrid.tick();

        double newBalance = bank.getBalance();
        if (newBalance > previousBalance) {
            Platform.runLater(() -> sound.playSfx(SoundEffect.DEPOSIT));
        }
        previousBalance = newBalance;

        Platform.runLater(shopManager::refreshUI);
    }

    // ==========================================
    // Panel Toggles
    // ==========================================

    @FXML
    void toggleShop() {
        boolean opening = !shopPopup.isVisible();
        if (opening) setInventoryVisible(false);
        setShopVisible(opening);
        inputHandler.clearKeys();
    }

    @FXML
    void toggleInventory() {
        boolean opening = !inventoryBar.isVisible();
        if (opening) setShopVisible(false);
        setInventoryVisible(opening);
        inputHandler.clearKeys();
    }

    // ==========================================
    // Placement Mode Actions
    // ==========================================

    @FXML
    void setBuildMode() {
        placementManager.setMode(PlacementMode.BUILD);
        updateModeButtons(PlacementMode.BUILD);
    }

    @FXML
    void setRemoveMode() {
        placementManager.setMode(PlacementMode.REMOVE);
        updateModeButtons(PlacementMode.REMOVE);
    }

    private void updateModeButtons(PlacementMode mode) {
        if (buildModeButton  != null) buildModeButton.getStyleClass().removeAll("mode-btn-active");
        if (removeModeButton != null) removeModeButton.getStyleClass().removeAll("mode-btn-active");
        if (mode == PlacementMode.BUILD  && buildModeButton  != null) buildModeButton.getStyleClass().add("mode-btn-active");
        if (mode == PlacementMode.REMOVE && removeModeButton != null) removeModeButton.getStyleClass().add("mode-btn-active");
    }

    // ==========================================
    // Visibility helpers
    // ==========================================

    private void setShopVisible(boolean visible) {
        shopPopup.setVisible(visible);
        shopPopup.setManaged(visible);
    }

    private void setInventoryVisible(boolean visible) {
        inventoryBar.setVisible(visible);
        inventoryBar.setManaged(visible);
    }
}