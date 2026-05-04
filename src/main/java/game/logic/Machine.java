package game.logic;

import java.util.Optional;

/**
 * Base type for grid-placed machines that may hold at most one item.
 */
public abstract class Machine implements Tickable {
    private int gridX;
    private int gridY;
    private double cost;
    private Direction facing;
    protected Item currentItem;

    protected Machine(double cost, Direction facing) {
        this.cost = cost;
        this.facing = facing;
    }

    @Override
    public void onTick() {
        // Simulation steps are orchestrated by GridSystem.tick().
    }

    public int getGridX() {
        return gridX;
    }

    public int getGridY() {
        return gridY;
    }

    public void setGridPosition(int gridX, int gridY) {
        this.gridX = gridX;
        this.gridY = gridY;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public Direction getFacing() {
        return facing;
    }

    public void setFacing(Direction facing) {
        this.facing = facing;
    }

    public Item getCurrentItem() {
        return currentItem;
    }

    void setCurrentItem(Item currentItem) {
        this.currentItem = currentItem;
    }

    /**
     * Invoked after an item is accepted into {@link #currentItem}.
     */
    public abstract void processItem(Item item);

    public boolean wouldAccept(Item item) {
        if (item == null) {
            return false;
        }
        return currentItem == null;
    }

    public boolean acceptItem(Item item) {
        if (!wouldAccept(item)) {
            return false;
        }
        this.currentItem = item;
        processItem(item);
        return true;
    }

    void clearCurrentItem() {
        this.currentItem = null;
    }

    /**
     * Describes an outgoing item move for this tick, if any.
     */
    Optional<OutgoingTransfer> prepareOutgoingTransfer(GridSystem grid, boolean[][] hadItemAtStart) {
        return Optional.empty();
    }
}
