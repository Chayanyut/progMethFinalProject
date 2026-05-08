package game.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UpgraderTest {

    @Test
    void getUpgradeFactorReturnsConstructedValue() {
        Upgrader upgrader = new Upgrader(MachineType.UP1, 1, Direction.RIGHT, 2.5);
        assertEquals(2.5, upgrader.getUpgradeFactor(), 1e-9);
    }

    @Test
    void processItemMultipliesValueByFactor() {
        Upgrader upgrader = new Upgrader(MachineType.UP1, 1, Direction.RIGHT, 3.0);
        Item item = new Item(ItemType.COAL);
        double expected = item.getValue() * 3.0;
        upgrader.processItem(item);
        assertEquals(expected, item.getValue(), 1e-9);
    }

    @Test
    void processItemWithFactorOneDoesNotChangeValue() {
        Upgrader upgrader = new Upgrader(MachineType.UP1, 1, Direction.RIGHT, 1.0);
        Item item = new Item(ItemType.IRON);
        double original = item.getValue();
        upgrader.processItem(item);
        assertEquals(original, item.getValue(), 1e-9);
    }
}
