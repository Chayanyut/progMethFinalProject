package game.logic;

import java.util.Optional;

/**
 * Pushes a held item one cell along {@link #getFacing()} when the target is free at tick start.
 */
public class Conveyor extends Machine {

    public Conveyor(double cost, Direction facing) {
        super(cost, facing);
    }

    @Override
    public void processItem(Item item) {
        // No transformation on intake.
    }

    @Override
    Optional<OutgoingTransfer> prepareOutgoingTransfer(GridSystem grid, boolean[][] hadItemAtStart) {
        if (getCurrentItem() == null) {
            return Optional.empty();
        }
        int nx = getGridX() + getFacing().deltaX();
        int ny = getGridY() + getFacing().deltaY();
        if (!grid.isInside(nx, ny)) {
            return Optional.empty();
        }
        Machine target = grid.getMachine(nx, ny);
        if (target == null) {
            return Optional.empty();
        }
        if (hadItemAtStart[nx][ny]) {
            return Optional.empty();
        }
        Item outgoing = getCurrentItem();
        if (!target.wouldAccept(outgoing)) {
            return Optional.empty();
        }
        return Optional.of(new OutgoingTransfer(getGridX(), getGridY(), nx, ny, outgoing));
    }

    @Override
    public MachineType getType() { return MachineType.CONVEYOR; }
}
