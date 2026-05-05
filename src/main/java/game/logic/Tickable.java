package game.logic;

/**
 * Simulation participant that receives periodic ticks.
 */
public interface Tickable {
    void onTick(boolean hadItemAtStart);
}
