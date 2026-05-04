package game.logic;

/**
 * A value-bearing object moved between machines.
 */
public class Item {
    private double value;

    public Item(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    /**
     * Multiplies this item's value by the given factor (in-place).
     */
    public void multiplyValue(double factor) {
        this.value *= factor;
    }
}
