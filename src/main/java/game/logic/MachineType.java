package game.logic;

public enum MachineType {
    /* Dynamically add into machine list
    To add new machine just copy and change constructor and use correct image path
     */

    NONE(0, "conveyor.png", null) {
        @Override
        public Machine create(Direction face, PlayerBank bank) {
            throw new IllegalStateException("Cannot create a NONE machine.");
        }
    },

    CONVEYOR(10, "conveyor.png", MachineCategory.TRANSPORT) {
        @Override
        public Machine create(Direction face, PlayerBank bank) {
            return new Conveyor(getCost(), face);
        }
    },

    // Dropper
    DROPPER(50, "dropper.png", MachineCategory.PRODUCTION) {
        @Override
        public Machine create(Direction face, PlayerBank bank) {
            return new Dropper(getCost(), face, 10.0); // 10.0 drop rate
        }
    },

    // Upgrader
    UPGRADER(100, "upgrader.png", MachineCategory.UPGRADES) {
        @Override
        public Machine create(Direction face, PlayerBank bank) {
            return new Upgrader(getCost(), face, 2.0); // 2.0 upgrade multiplier
        }
    },
    TEST(19, "upgrader.png", MachineCategory.UPGRADES) {
        @Override
        public Machine create(Direction face, PlayerBank bank) {
            return new Upgrader(getCost(), face, 1231);
        }
    },

    // Furnace
    FURNACE(200, "furnace.png", MachineCategory.PROCESSING) {
        @Override
        public Machine create(Direction face, PlayerBank bank) {
            return new Furnace(getCost(), face, bank); // Furnace needs the bank
        }
    };

    private final double cost;
    private final String imageName;
    private final MachineCategory category;

    MachineType(double cost, String imageName, MachineCategory category) {
        this.cost = cost;
        this.imageName = imageName;
        this.category = category;
    }

    public double getCost() {
        return cost;
    }

    public String getImageName() {
        return imageName;
    }

    public MachineCategory getCategory() {
        return category;
    }

    public abstract Machine create(Direction face, PlayerBank bank);
}