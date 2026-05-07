package game.ui;

import game.logic.PlayerBank;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

public class ShopManager {

    // You can expand this enum to 50 machines later, and the UI will adapt automatically!
    public enum MachineType {
        NONE(0), DROPPER(50), CONVEYOR(10), UPGRADER(100), FURNACE(200);

        private final double cost;
        MachineType(double cost) { this.cost = cost; }
        public double getCost() { return cost; }
    }

    private final PlayerBank bank;
    private final Map<MachineType, Integer> inventory = new EnumMap<>(MachineType.class);
    private MachineType activeSelection = MachineType.NONE;

    // UI References passed from GameController
    private final HBox inventoryBox;
    private final Label moneyLabel;
    private final Function<MachineType, Image> imageLookup;
    private final Runnable onStateChange;

    public ShopManager(PlayerBank bank, HBox inventoryBox, Label moneyLabel,
                       Function<MachineType, Image> imageLookup, Runnable onStateChange) {
        this.bank = bank;
        this.inventoryBox = inventoryBox;
        this.moneyLabel = moneyLabel;
        this.imageLookup = imageLookup;
        this.onStateChange = onStateChange;

        // Initialize inventory counts to 0
        for (MachineType type : MachineType.values()) {
            if (type != MachineType.NONE) inventory.put(type, 0);
        }
    }

    public PlayerBank getBank() {
        return bank;
    }

    public MachineType getActiveSelection() {
        return activeSelection;
    }

    public int getInventoryCount(MachineType type) {
        return inventory.getOrDefault(type, 0);
    }

    // This handles EVERY machine purchase dynamically
    public void attemptBuy(MachineType type) {
        if (type == MachineType.NONE) return;

        if (bank.trySpend(type.getCost())) {
            inventory.put(type, inventory.get(type) + 1);
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

    public void refreshUI() {
        // Update Bank Text
        if (moneyLabel != null) {
            moneyLabel.setText(String.format("Balance: $%.0f", bank.getBalance()));
        }

        // Update Bottom Hotbar
        if (inventoryBox != null) {
            inventoryBox.getChildren().clear();
            for (MachineType type : MachineType.values()) {
                if (type == MachineType.NONE) continue;
                inventoryBox.getChildren().add(createInventorySlot(type));
            }
        }

        // Tell GameController to update hints/holograms
        if (onStateChange != null) {
            onStateChange.run();
        }
    }

    private VBox createInventorySlot(MachineType type) {
        int qty = inventory.getOrDefault(type, 0);

        VBox slot = new VBox(4);
        slot.getStyleClass().add("inventory-slot");
        if (qty == 0) slot.getStyleClass().add("inventory-slot-empty");
        if (activeSelection == type) slot.getStyleClass().add("inventory-slot-active");

        ImageView icon = new ImageView(imageLookup.apply(type));
        icon.setFitWidth(40);
        icon.setFitHeight(40);
        icon.setPreserveRatio(true);

        Label qtyLabel = new Label("x" + qty);
        qtyLabel.setTextFill(Color.web("#ecf0f1"));
        qtyLabel.setFont(Font.font(12));

        slot.getChildren().addAll(icon, qtyLabel);

        // Deselection logic
        slot.setOnMouseClicked(e -> {
            activeSelection = (activeSelection == type) ? MachineType.NONE : type;
            refreshUI();
        });

        VBox.setVgrow(icon, Priority.NEVER);
        return slot;
    }
}