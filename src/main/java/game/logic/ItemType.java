package game.logic;

public enum ItemType {
    COAL("Coal", 1.0, "Coal.png"),
    COPPER("Copper Ore",3.0,"Copper.png"),
    IRON("Iron Ore", 8.0, "Iron.png"),
    SILVER("Silver Ore",20.0,"Silver.png"),
    GOLD("Gold Ore", 50.0, "Gold.png"),
    PLATINUM("Platinum Ore",120.0,"Platinum.png"),
    RUBY("Ruby",300.0,"Ruby.png"),
    SAPPHIRE("Sapphire",800.0,"Sapphire.png"),
    EMERALD("Emerald",2000.0,"Emerald.png"),
    DIAMOND("Diamond",5000.0,"Diamond.png");


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