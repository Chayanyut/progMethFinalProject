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
    COALDROPPER(50, "DpCoal.png", MachineCategory.PRODUCTION, "Coal") {
        @Override
        public Machine create(Direction face, PlayerBank bank) {
            return new Dropper(this, getCost(), face, ItemType.COAL);
        }
    },
    COPPERDROPPER(250, "DpCopper.png", MachineCategory.PRODUCTION, "Copper") {
        @Override
        public Machine create(Direction face, PlayerBank bank) {
            return new Dropper(this, getCost(), face, ItemType.COPPER);
        }
    },
    IRONDROPPER(1000, "DpIron.png", MachineCategory.PRODUCTION, "Iron") {
        @Override
        public Machine create(Direction face, PlayerBank bank) {
            return new Dropper(this, getCost(), face, ItemType.IRON);
        }
    },
    SILVERDROPPER(4000, "DpSilver.png", MachineCategory.PRODUCTION, "Silver") {
        @Override
        public Machine create(Direction face, PlayerBank bank) {
            return new Dropper(this, getCost(), face, ItemType.SILVER);
        }
    },
    GOLDDROPPER(15000, "DpGold.png", MachineCategory.PRODUCTION, "Gold") {
        @Override
        public Machine create(Direction face, PlayerBank bank) {
            return new Dropper(this, getCost(), face, ItemType.GOLD);
        }
    },
    PLATINUMDROPPER(50000, "DpPlatinum.png", MachineCategory.PRODUCTION, "Platinum") {
        @Override
        public Machine create(Direction face, PlayerBank bank) {
            return new Dropper(this, getCost(), face, ItemType.PLATINUM);
        }
    },
    RUBYDROPPER(150000, "DpRuby.png", MachineCategory.PRODUCTION, "Ruby") {
        @Override
        public Machine create(Direction face, PlayerBank bank) {
            return new Dropper(this, getCost(), face, ItemType.RUBY);
        }
    },
    SAPPHIREDROPPER(500000, "DpSapphire.png", MachineCategory.PRODUCTION, "Sapphire") {
        @Override
        public Machine create(Direction face, PlayerBank bank) {
            return new Dropper(this, getCost(), face, ItemType.SAPPHIRE);
        }
    },
    EMERALDDROPPER(1500000, "DpEmerald.png", MachineCategory.PRODUCTION, "Emerald") {
        @Override
        public Machine create(Direction face, PlayerBank bank) {
            return new Dropper(this, getCost(), face, ItemType.EMERALD);
        }
    },
    DIAMONDDROPPER(5000000, "DpDiamond.png", MachineCategory.PRODUCTION, "Diamond") {
        @Override
        public Machine create(Direction face, PlayerBank bank) {
            return new Dropper(this, getCost(), face, ItemType.DIAMOND);
        }
    },

    // Upgrader
    UP1(150, "UP1.jpg", MachineCategory.UPGRADES, "Upgrades1") {
        @Override
        public Machine create(Direction face, PlayerBank bank) {
            return new Upgrader(this, getCost(), face, 2.0);
        }
    },
    UP2(2500, "UP2.jpg", MachineCategory.UPGRADES, "Upgrades2") {
        @Override
        public Machine create(Direction face, PlayerBank bank) {
            return new Upgrader(this, getCost(), face, 5.0);
        }
    },
    UP3(80000, "UP3.jpg", MachineCategory.UPGRADES, "Upgrades3") {
        @Override
        public Machine create(Direction face, PlayerBank bank) {
            return new Upgrader(this, getCost(), face, 15.0);
        }
    },
    UP4(3500000, "UP4.jpg", MachineCategory.UPGRADES, "Upgrades4") {
        @Override
        public Machine create(Direction face, PlayerBank bank) {
            return new Upgrader(this, getCost(), face, 60.0);
        }
    },
    UP5(250000000, "UP5.jpg", MachineCategory.UPGRADES, "Upgrades5") {
        @Override
        public Machine create(Direction face, PlayerBank bank) {
            return new Upgrader(this, getCost(), face, 250.0);
        }
    },

    // Furnace
    FURNACE(200, "Furnace.png", MachineCategory.PROCESSING, "Furnace") {
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