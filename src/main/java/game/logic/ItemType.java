package game.logic;

public enum ItemType {
    COAL("Coal", 5.0, "IMG_3462.png"),
    IRON("Iron Ore", 15.0, "iron_item.png"),
    GOLD("Gold Ore", 50.0, "gold_item.png");

    private final String displayName;
    private final double value;
    private final String imageName;

    ItemType(String displayName, double value, String imageName) {
        this.displayName = displayName;
        this.value = value;
        this.imageName = imageName;
    }

    public String getDisplayName() { return displayName; }
    public double getValue() { return value; }
    public String getImageName() { return imageName; }
}