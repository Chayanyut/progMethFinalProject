package game.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GridSystemTest {

    @Test
    void placeMachineRejectsOutOfBounds() {
        GridSystem grid = new GridSystem(3, 3);
        assertFalse(grid.placeMachine(-1, 0, new Conveyor(1, Direction.RIGHT)));
        assertFalse(grid.placeMachine(0, -1, new Conveyor(1, Direction.RIGHT)));
        assertFalse(grid.placeMachine(3, 0, new Conveyor(1, Direction.RIGHT)));
        assertFalse(grid.placeMachine(0, 3, new Conveyor(1, Direction.RIGHT)));
    }

    @Test
    void placeMachineRejectsOverlap() {
        GridSystem grid = new GridSystem(2, 2);
        assertTrue(grid.placeMachine(0, 0, new Conveyor(1, Direction.RIGHT)));
        assertFalse(grid.placeMachine(0, 0, new Conveyor(1, Direction.DOWN)));
    }

    @Test
    void conveyorLineMovesAtMostOneCellPerTick() {
        GridSystem grid = new GridSystem(5, 1);
        Dropper dropper = new Dropper(1, Direction.RIGHT, 10);
        Conveyor c1 = new Conveyor(1, Direction.RIGHT);
        Conveyor c2 = new Conveyor(1, Direction.RIGHT);
        assertTrue(grid.placeMachine(0, 0, dropper));
        assertTrue(grid.placeMachine(1, 0, c1));
        assertTrue(grid.placeMachine(2, 0, c2));

        grid.tick();
        assertNotNull(dropper.getCurrentItem());
        assertNull(c1.getCurrentItem());
        assertNull(c2.getCurrentItem());

        grid.tick();
        assertNull(dropper.getCurrentItem());
        assertNotNull(c1.getCurrentItem());
        assertNull(c2.getCurrentItem());

        grid.tick();
        assertNotNull(dropper.getCurrentItem());
        assertNull(c1.getCurrentItem());
        assertNotNull(c2.getCurrentItem());
    }

    @Test
    void furnaceCreditsBankAndClearsItem() {
        PlayerBank bank = new PlayerBank(0);
        GridSystem grid = new GridSystem(5, 1);
        Dropper dropper = new Dropper(1, Direction.RIGHT, 7);
        Conveyor conveyor = new Conveyor(1, Direction.RIGHT);
        Furnace furnace = new Furnace(1, Direction.LEFT, bank);

        assertTrue(grid.placeMachine(0, 0, dropper));
        assertTrue(grid.placeMachine(1, 0, conveyor));
        assertTrue(grid.placeMachine(2, 0, furnace));

        grid.tick();
        grid.tick();
        assertNull(furnace.getCurrentItem());
        assertEquals(0, bank.getBalance(), 1e-9);

        grid.tick();
        assertNull(furnace.getCurrentItem());
        assertEquals(7, bank.getBalance(), 1e-9);
    }
}
