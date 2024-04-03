package com.piggyplugins.BobTheFarmer;

public enum Allotment {
    NONE("","","",""),
    Snape_Grass("Snape grass seed", "Snape grass", "Jangerberries","White lily"),

    ;

    public final String SeedName;
    public final String PlantName;
    public final String ProtectionItem;
    Allotment(String itemName, String herbName, String protectionItem, String protectionFlower)
    {
        this.SeedName = itemName;
        this.PlantName = herbName;
        this.ProtectionItem = protectionItem;
    }
}
