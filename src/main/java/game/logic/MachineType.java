package game.logic;

public enum MachineType {
    /* List of all machines in the shop
    Simply add machine name under each section to include them in the shop */

    NONE(0),

    // Conveyor
    CONVEYOR(10),

    // Dropper
    DROPPER(50),

    // Upgrader
    UPGRADER(100),

    // Furnace
    FURNACE(200);

    private final double cost;
    MachineType(double cost) { this.cost = cost; }
    public double getCost() { return cost; }
}