package game.logic;

/**
 * A single-tick intent to move an item from one grid cell to another.
 */
public record OutgoingTransfer(int fromX, int fromY, int toX, int toY, Item item) {
}
