package game.logic;

/**
 * Multiplies item value on intake, then behaves like a conveyor on each tick.
 */
public class Upgrader extends Conveyor {
    private final double upgradeFactor;

    public Upgrader(double cost, Direction facing, double upgradeFactor) {
        super(cost, facing);
        this.upgradeFactor = upgradeFactor;
    }

    public double getUpgradeFactor() {
        return upgradeFactor;
    }

    @Override
    public MachineType getType() { return MachineType.UPGRADER; }

    @Override
    public void processItem(Item item) {
        item.multiplyValue(upgradeFactor);
    }
}
