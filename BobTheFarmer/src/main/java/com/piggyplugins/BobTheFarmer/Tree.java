package com.piggyplugins.BobTheFarmer;

public enum Tree {
    Oak("Oak sapling", "Tomatoes(5)", 1),
    Willow("Willow sapling","Apples(5)", 1),
    Maple("Maple sapling","Oranges(5)", 1),
    Yew("Yew sapling","Cactus spine", 10),
    Magic("Magic sapling","Coconut", 25);

    public final String Seed;
    public final String ProtectionItem;
    public final int AmountOfProtectionItem;
    Tree(String seed, String protectionItem, int amountOfProtectionItem)
    {
        this.Seed = seed;
        this.ProtectionItem = protectionItem;
        this.AmountOfProtectionItem = amountOfProtectionItem;
    }
}
