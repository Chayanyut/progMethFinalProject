package game.ui;

/**
 * Shared constants used across the UI layer.
 * Single source of truth — no more duplicated magic numbers.
 */
public final class GameConstants {

    private GameConstants() {}

    public static final double TILE_SIZE = 50.0;
    public static final double WORLD_WIDTH  = 2000.0;
    public static final double WORLD_HEIGHT = 1600.0;
}