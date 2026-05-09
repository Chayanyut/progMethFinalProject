package game.ui;

import game.logic.*;
import javafx.scene.input.MouseEvent;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class PlacementManager {

    // ==========================================
    // Constants
    // ==========================================
    private static final double TILE_SIZE = GameConstants.TILE_SIZE;

    // ==========================================
    // Placement State
    // ==========================================
    private Direction     placementFacing = Direction.RIGHT;
    private PlacementMode placementMode   = PlacementMode.BUILD;
    private double mouseWorldX;
    private double mouseWorldY;

    // ==========================================
    // Hard Dependencies
    // ==========================================
    private final GridSystem logicGrid;
    private final PlayerBank bank;

    // ==========================================
    // Callbacks
    // ==========================================
    private final BooleanSupplier                isShopVisible;
    private final Supplier<MachineType>          getActiveSelection;
    private final Function<MachineType, Integer> getInventoryCount;
    private final Consumer<MachineType>          consumeFromInventory;
    private final Consumer<MachineType>          returnToInventory;
    private final Runnable                       refreshUI;
    private final Consumer<String>               onHintText;
    private final Runnable                       onPlaceSound;
    private final Runnable                       onRemoveSound;

    // ==========================================
    // Constructor
    // ==========================================
    public PlacementManager(GridSystem logicGrid,
                            PlayerBank bank,
                            BooleanSupplier isShopVisible,
                            Supplier<MachineType> getActiveSelection,
                            Function<MachineType, Integer> getInventoryCount,
                            Consumer<MachineType> consumeFromInventory,
                            Consumer<MachineType> returnToInventory,
                            Runnable refreshUI,
                            Consumer<String> onHintText,
                            Runnable onPlaceSound,
                            Runnable onRemoveSound) {
        this.logicGrid            = logicGrid;
        this.bank                 = bank;
        this.isShopVisible        = isShopVisible;
        this.getActiveSelection   = getActiveSelection;
        this.getInventoryCount    = getInventoryCount;
        this.consumeFromInventory = consumeFromInventory;
        this.returnToInventory    = returnToInventory;
        this.refreshUI            = refreshUI;
        this.onHintText           = onHintText;
        this.onPlaceSound         = onPlaceSound;
        this.onRemoveSound        = onRemoveSound;
    }

    // ==========================================
    // Accessors
    // ==========================================
    public Direction     getPlacementFacing() { return placementFacing; }
    public PlacementMode getPlacementMode()   { return placementMode;   }
    public double        getMouseWorldX()     { return mouseWorldX;     }
    public double        getMouseWorldY()     { return mouseWorldY;     }

    // ==========================================
    // Mode Switching
    // ==========================================
    public void setMode(PlacementMode mode) {
        this.placementMode = mode;
        updateHint();
    }

    // ==========================================
    // Facing
    // ==========================================
    public void cyclePlacementFacing() {
        placementFacing = switch (placementFacing) {
            case RIGHT -> Direction.DOWN;
            case DOWN  -> Direction.LEFT;
            case LEFT  -> Direction.UP;
            case UP    -> Direction.RIGHT;
        };
        updateHint();
    }

    // ==========================================
    // Hint Text
    // ==========================================
    public void updateHint() {
        if (onHintText == null) return;

        if (placementMode == PlacementMode.REMOVE) {
            onHintText.accept("REMOVE mode — click a machine to remove it.");
            return;
        }

        MachineType selection = getActiveSelection.get();
        int qty = getInventoryCount.apply(selection);
        onHintText.accept(
                selection == MachineType.NONE
                        ? "Pick a slot from inventory. Facing: " + placementFacing + " (R rotates)."
                        : "Selected: " + selection + " (x" + qty + ") | Facing: " + placementFacing + " (R rotates)"
        );
    }

    // ==========================================
    // Canvas Event Handlers
    // ==========================================
    public void handleCanvasClick(MouseEvent event) {
        if (isShopVisible.getAsBoolean()) return;

        int gx = (int) Math.floor(event.getX() / TILE_SIZE);
        int gy = (int) Math.floor(event.getY() / TILE_SIZE);
        if (!logicGrid.isInside(gx, gy)) return;

        if (placementMode == PlacementMode.REMOVE) {
            handleRemove(gx, gy);
        } else {
            handleBuild(gx, gy);
        }
    }

    public void handleCanvasMouseMove(MouseEvent event) {
        if (isShopVisible.getAsBoolean()) return;
        mouseWorldX = event.getX();
        mouseWorldY = event.getY();
    }

    // ==========================================
    // Private helpers
    // ==========================================
    private void handleBuild(int gx, int gy) {
        MachineType selection = getActiveSelection.get();
        if (selection == MachineType.NONE) return;
        if (getInventoryCount.apply(selection) <= 0) {
            refreshUI.run();
            return;
        }

        Machine toPlace = createMachine(selection);
        if (logicGrid.placeMachine(gx, gy, toPlace)) {
            consumeFromInventory.accept(selection);
            if (onPlaceSound != null) onPlaceSound.run();
        }
    }

    private void handleRemove(int gx, int gy) {
        Machine existing = logicGrid.getMachine(gx, gy);
        if (existing == null) return;

        MachineType type = existing.getType();
        if (logicGrid.removeMachine(gx, gy)) {
            if (type != MachineType.NONE) returnToInventory.accept(type);
            if (onRemoveSound != null) onRemoveSound.run();
            refreshUI.run();
        }
    }

    private Machine createMachine(MachineType type) {
        return type.create(placementFacing, bank);
    }
}