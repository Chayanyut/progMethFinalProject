package game.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DropperTest {

    @Test
    void freshDropperHasNoItem() {
        Dropper dropper = new Dropper(MachineType.COALDROPPER, 1, Direction.RIGHT, ItemType.COAL);
        assertNull(dropper.getCurrentItem());
    }

    @Test
    void spawnIfStillEmptyProducesItemWhenEmpty() {
        Dropper dropper = new Dropper(MachineType.COALDROPPER, 1, Direction.RIGHT, ItemType.COAL);
        dropper.spawnIfStillEmpty(true);
        assertNotNull(dropper.getCurrentItem());
    }

    @Test
    void spawnedItemMatchesConfiguredType() {
        Dropper dropper = new Dropper(MachineType.GOLDDROPPER, 1, Direction.RIGHT, ItemType.GOLD);
        dropper.spawnIfStillEmpty(true);
        assertEquals(ItemType.GOLD, dropper.getCurrentItem().getType());
    }

    @Test
    void spawnIfStillEmptyDoesNotReplaceExistingItem() {
        Dropper dropper = new Dropper(MachineType.COALDROPPER, 1, Direction.RIGHT, ItemType.COAL);
        dropper.spawnIfStillEmpty(true);
        Item first = dropper.getCurrentItem();
        dropper.spawnIfStillEmpty(true);
        assertSame(first, dropper.getCurrentItem());
    }

    @Test
    void spawnIfStillEmptyWithFalseDoesNotSpawn() {
        Dropper dropper = new Dropper(MachineType.COALDROPPER, 1, Direction.RIGHT, ItemType.IRON);
        dropper.spawnIfStillEmpty(false);
        assertNull(dropper.getCurrentItem());
    }
}
