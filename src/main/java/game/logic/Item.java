package game.logic;

/**
 * A value-bearing object moved between machines.
 */
public class Item {
    private double value;
    private final ItemType type;

    public Item(ItemType type) {
        this.type = type;
        this.value = type.getValue();
    }

    public ItemType getType() {
        return type;
    }

    public double getValue() {
        return value;
    }

    public void multiplyValue(double factor) {
        this.value *= factor;
    }
}
