package game.logic;

/**
 * Destroys held items and deposits their value into a {@link PlayerBank}.
 */
public class Furnace extends Machine {
    private final PlayerBank bank;

    public Furnace(double cost, Direction facing, PlayerBank bank) {
        super(cost, facing);
        this.bank = bank;
    }

    public PlayerBank getBank() {
        return bank;
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
