package game.ui;

import game.logic.MachineType;
import game.logic.PlayerBank;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class ShopManager {

    // ==========================================
    // Core state
    // ==========================================
    private final PlayerBank bank;
    private final Map<MachineType, Integer> inventory = new EnumMap<>(MachineType.class);

    /** Types bought at least once — controls which tiles appear in the inventory bar. */
    private final Set<MachineType> everBought = EnumSet.noneOf(MachineType.class);

    private MachineType activeSelection = MachineType.NONE;

    // ==========================================
    // UI references
    // ==========================================
    private final Label moneyLabel;
    private final Function<MachineType, Image> imageLookup;
    private final Runnable onStateChange;

    private InventoryUIBuilder inventoryUI; // wired after FXML is ready

    // ==========================================
    // Constructor
    // ==========================================
    public ShopManager(PlayerBank bank,
                       Label moneyLabel,
                       Function<MachineType, Image> imageLookup,
                       Runnable onStateChange) {
        this.bank          = bank;
        this.moneyLabel    = moneyLabel;
        this.imageLookup   = imageLookup;
        this.onStateChange = onStateChange;

        // Initialise all counts to 0 but don't build any UI tiles yet
        for (MachineType type : MachineType.values()) {
            if (type != MachineType.NONE) inventory.put(type, 0);
        }
    }

    // ==========================================
    // Inventory bar wiring (called from GameController after FXML load)
    // ==========================================
    public void initInventoryUI(TabPane inventoryTabPane) {
        inventoryUI = new InventoryUIBuilder(
                inventoryTabPane,
                imageLookup,
                this::handleTileClick
        );
    }

    // ==========================================
    // Shop actions
    // ==========================================
    public void attemptBuy(MachineType type) {
        if (type == MachineType.NONE) return;

        if (bank.trySpend(type.getCost())) {
            inventory.put(type, inventory.get(type) + 1);

            // First-ever purchase — reveal the tile in the inventory bar
            if (!everBought.contains(type)) {
                everBought.add(type);
                if (inventoryUI != null) inventoryUI.ensureVisible(type);
            }

            refreshUI();
        }
    }

    public void consumeFromInventory(MachineType type) {
        int current = inventory.getOrDefault(type, 0);
        if (current > 0) {
            inventory.put(type, current - 1);
            refreshUI();
        }
    }

    // ==========================================
    // Selection
    // ==========================================
    public MachineType getActiveSelection() { return activeSelection; }

    public int getInventoryCount(MachineType type) {
        return inventory.getOrDefault(type, 0);
    }

    private void handleTileClick(MachineType type) {
        activeSelection = (activeSelection == type) ? MachineType.NONE : type;
        refreshUI();
    }

    // ==========================================
    // UI refresh
    // ==========================================
    public void refreshUI() {
        if (moneyLabel != null) {
            moneyLabel.setText(String.format("Balance: $%.0f", bank.getBalance()));
        }

        if (inventoryUI != null) {
            inventoryUI.refreshSlots(inventory, activeSelection);
        }

        if (onStateChange != null) {
            onStateChange.run();
        }
    }
}