package game.logic;

public enum MachineCategory {
    PRODUCTION("Production"),
    TRANSPORT("Transport"),
    UPGRADES("Upgrades"),
    PROCESSING("Processing");

    private final String displayName;

    MachineCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}