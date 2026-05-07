package game.logic;

/**
 * Multiplies item value on intake, then behaves like a conveyor on each tick.
 */
public class Upgrader extends Conveyor {
    private final double upgradeFactor;

    public Upgrader(MachineType type, double cost, Direction facing, double upgradeFactor) {
        super(type, cost, facing);
        this.upgradeFactor = upgradeFactor;
    }

    public double getUpgradeFactor() {
        return upgradeFactor;
    }

    @Override
    public void processItem(Item item) {
        item.multiplyValue(upgradeFactor);
    }
}
