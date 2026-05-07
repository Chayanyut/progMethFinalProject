package game.logic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fixed-size grid of {@link Machine} cells with placement rules and a deterministic
 * two-phase item transfer each {@link #tick()}.
 */
public class GridSystem {
    private final int width;
    private final int height;
    private final Machine[][] cells;

    public GridSystem(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width and height must be positive");
        }
        this.width = width;
        this.height = height;
        this.cells = new Machine[width][height];
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isInside(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public Machine getMachine(int x, int y) {
        if (!isInside(x, y)) {
            return null;
        }
        return cells[x][y];
    }

    /**
     * @return false if out of bounds or cell already occupied
     */
    public boolean placeMachine(int x, int y, Machine machine) {
        if (!isInside(x, y)) {
            return false;
        }
        if (cells[x][y] != null) {
            return false;
        }
        cells[x][y] = machine;
        machine.setGridPosition(x, y);
        return true;
    }

    public boolean removeMachine(int x, int y) {
        if (!isInside(x, y)) {
            return false;
        }
        if (cells[x][y] == null) {
            return false;
        }
        cells[x][y] = null;
        return true;
    }

    /**
     * One simulation step: collect outgoing transfers using start-of-tick occupancy,
     * resolve target conflicts (row-major winning source), apply moves, smelt furnaces,
     * then spawn from droppers that started empty.
     */

    public void tick() {
        // 1. Snapshot the starting state
        boolean[][] hadItemAtStart = new boolean[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Machine m = cells[x][y];
                hadItemAtStart[x][y] = (m != null && m.getCurrentItem() != null);
            }
        }

        // 2. Gather intentions
        List<OutgoingTransfer> pending = new ArrayList<>();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Machine m = cells[x][y];
                if (m != null) {
                    m.prepareOutgoingTransfer(this, hadItemAtStart).ifPresent(pending::add);
                }
            }
        }

        // 3 & 4. Group by target AND resolve conflicts instantly
        Map<Integer, OutgoingTransfer> winnersMap = new HashMap<>();
        for (OutgoingTransfer transfer : pending) {
            // Create a fast 1D integer key
            int targetKey = transfer.toY() * width + transfer.toX();

            winnersMap.compute(targetKey, (key, currentWinner) -> {
                if (currentWinner == null) return transfer; // First one here wins by default

                // If collision: keep the one with the lowest X (or lowest Y if X is tied)
                if (transfer.fromX() < currentWinner.fromX() ||
                        (transfer.fromX() == currentWinner.fromX() && transfer.fromY() < currentWinner.fromY())) {
                    return transfer;
                }
                return currentWinner;
            });
        }

        // Optional: Sort the final execution order if strict chronological determinism is required
        List<OutgoingTransfer> winners = new ArrayList<>(winnersMap.values());
        winners.sort(Comparator.comparingInt(OutgoingTransfer::fromX).thenComparingInt(OutgoingTransfer::fromY));

        // 5. Execute the winning moves
        for (OutgoingTransfer transfer : winners) {
            Machine source = getMachine(transfer.fromX(), transfer.fromY());
            Machine destination = getMachine(transfer.toX(), transfer.toY());

            if (source == null || destination == null) continue;
            if (source.getCurrentItem() != transfer.item()) continue;
            if (!destination.wouldAccept(transfer.item())) continue;

            source.clearCurrentItem();
            if (!destination.acceptItem(transfer.item())) {
                source.setCurrentItem(transfer.item()); // Rollback if rejected
            }
        }

        // 6. Post-Tick Processing (Smelting, Spawning)
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Machine machine = cells[x][y];
                if (machine != null) {
                    machine.onTick(hadItemAtStart[x][y]);
                }
            }
        }
    }
}
