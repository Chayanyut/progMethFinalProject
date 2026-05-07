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

    CONVEYOR(10, "conveyor.png", MachineCategory.TRANSPORT, "Conveyor") {
        @Override
        public Machine create(Direction face, PlayerBank bank) {
            return new Conveyor(this, getCost(), face);
        }
    },

    // Dropper
    DROPPER(50, "dropper.png", MachineCategory.PRODUCTION, "Dropper") {
        @Override
        public Machine create(Direction face, PlayerBank bank) {
            return new Dropper(this, getCost(), face, 10.0); // 10.0 drop rate
        }
    },

    // Upgrader
    UPGRADER(100, "upgrader.png", MachineCategory.UPGRADES, "Upgrades") {
        @Override
        public Machine create(Direction face, PlayerBank bank) {
            return new Upgrader(this, getCost(), face, 2.0); // 2.0 upgrade multiplier
        }
    },
    TEST(19, "IMG_3439.png", MachineCategory.UPGRADES, "TEST") {
        @Override
        public Machine create(Direction face, PlayerBank bank) {
            return new Upgrader(this, getCost(), face, 1231);
        }
    },

    // Furnace
    FURNACE(200, "furnace.png", MachineCategory.PROCESSING, "Furnace") {
        @Override
        public Machine create(Direction face, PlayerBank bank) {
            return new Furnace(this, getCost(), face, bank); // Furnace needs the bank
        }
    };

    private final double cost;
    private final String imageName;
    private final MachineCategory category;
    private final String fallBackText;

    MachineType(double cost, String imageName, MachineCategory category) {
        this.cost = cost;
        this.imageName = imageName;
        this.category = category;
        this.fallBackText = null;
    }

    // Add fallback text for placeholder image
    MachineType(double cost, String imageName, MachineCategory category, String fallBackText) {
        this.cost = cost;
        this.imageName = imageName;
        this.category = category;
        this.fallBackText = fallBackText;
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

    public String getFallBackText() {
        return fallBackText;
    }

    public abstract Machine create(Direction face, PlayerBank bank);
}