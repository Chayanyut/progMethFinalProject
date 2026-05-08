package game.ui;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;

public class InputHandler {

    // ==========================================
    // Input State
    // ==========================================
    private final Set<KeyCode> activeKeys = ConcurrentHashMap.newKeySet();

    // ==========================================
    // Callbacks
    // ==========================================
    private final Runnable       onToggleShop;
    private final Runnable       onToggleInventory;
    private final Runnable       onCycleFacing;
    private final DoubleConsumer onZoom;
    private final BooleanSupplier isShopVisible;
    private final BooleanSupplier isInventoryVisible;

    // ==========================================
    // Constructor
    // ==========================================
    public InputHandler(Runnable onToggleShop,
                        Runnable onToggleInventory,
                        Runnable onCycleFacing,
                        DoubleConsumer onZoom,
                        BooleanSupplier isShopVisible,
                        BooleanSupplier isInventoryVisible) {
        this.onToggleShop       = onToggleShop;
        this.onToggleInventory  = onToggleInventory;
        this.onCycleFacing      = onCycleFacing;
        this.onZoom             = onZoom;
        this.isShopVisible      = isShopVisible;
        this.isInventoryVisible = isInventoryVisible;
    }

    // ==========================================
    // Scene Binding
    // ==========================================
    public void attach(Scene scene) {
        scene.addEventHandler(KeyEvent.KEY_PRESSED,  this::handleKeyPressed);
        scene.addEventHandler(KeyEvent.KEY_RELEASED, this::handleKeyReleased);
        scene.addEventHandler(ScrollEvent.SCROLL,    this::handleScroll);
    }

    public void detach(Scene scene) {
        scene.removeEventHandler(KeyEvent.KEY_PRESSED,  this::handleKeyPressed);
        scene.removeEventHandler(KeyEvent.KEY_RELEASED, this::handleKeyReleased);
        scene.removeEventHandler(ScrollEvent.SCROLL,    this::handleScroll);
    }

    // ==========================================
    // Accessors
    // ==========================================
    public Set<KeyCode> getActiveKeys() { return activeKeys; }

    public void clearKeys() { activeKeys.clear(); }

    // ==========================================
    // Handlers
    // ==========================================
    private void handleKeyPressed(KeyEvent event) {
        KeyCode code = event.getCode();
        boolean anyPanelOpen = isShopVisible.getAsBoolean() || isInventoryVisible.getAsBoolean();

        // Toggle keys always fire regardless of panel state
        if (code == KeyCode.E) { onToggleShop.run();      return; }
        if (code == KeyCode.B) { onToggleInventory.run(); return; }

        // R only works when no panel is open
        if (code == KeyCode.R && !anyPanelOpen) { onCycleFacing.run(); return; }

        // Movement keys only tracked when no panel is blocking
        if (!anyPanelOpen) activeKeys.add(code);
    }

    private void handleKeyReleased(KeyEvent event) {
        activeKeys.remove(event.getCode());
    }

    private void handleScroll(ScrollEvent event) {
        if (isShopVisible.getAsBoolean() || isInventoryVisible.getAsBoolean()) return;
        double factor = event.getDeltaY() > 0 ? 1.08 : 1.0 / 1.08;
        onZoom.accept(factor);
        event.consume();
    }
}