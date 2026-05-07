package game.logic;

/**
 * Spawns a new item when empty at the start of a tick, then behaves like a conveyor
 * so items can leave toward {@link #getFacing()}.
 */
public class Dropper extends Conveyor { ;
    private final ItemType itemToDrop;

    public Dropper(MachineType type, double cost, Direction facing, ItemType itemToDrop) {
        super(type, cost, facing);
        this.itemToDrop = itemToDrop;
    }

    @Override
    public void onTick(boolean hadItemAtStart) {
        spawnIfStillEmpty(!hadItemAtStart);
    }

    @Override
    public void processItem(Item item) {
        // No transformation on intake (normally not fed by belts).
    }

    void spawnIfStillEmpty(boolean wasEmptyAtTickStart) {
        if (!wasEmptyAtTickStart) {
            return;
        }
        if (getCurrentItem() != null) {
            return;
        }
        acceptItem(new Item(itemToDrop));
    }
}
