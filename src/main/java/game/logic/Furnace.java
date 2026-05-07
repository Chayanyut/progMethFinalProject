package game.logic;

/**
 * Destroys held items and deposits their value into a {@link PlayerBank}.
 */
public class Furnace extends Machine {
    private final PlayerBank bank;

    public Furnace(MachineType type, double cost, Direction facing, PlayerBank bank) {
        super(type, cost, facing);
        this.bank = bank;
    }

    @Override
    public void onTick(boolean hadItemAtStart) {
        smeltHeldItem();
    }

    @Override
    public void processItem(Item item) {
        // Smelting happens on tick when an item is held; see GridSystem.tick().
    }

    void smeltHeldItem() {
        Item item = getCurrentItem();
        if (item == null) {
            return;
        }
        bank.deposit(item.getValue());
        clearCurrentItem();
    }
}
