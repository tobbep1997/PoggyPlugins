package com.piggyplugins.BobTheFarmer;

public enum Herb {
    Guam_leaf("Guam seed"),
    Marrentill("Marrentill seed"),
    Tarromin("Tarromin seed"),
    Harralander("Harralander seed"),
    Ranarr_weed("Ranarr seed"),
    Toadflax("Toadflax seed"),
    Irit_leaf("Irit seed"),
    Avantoe("Avantoe seed"),
    Kwuarm("Kwuarm seed"),
    Snapdragon("Snapdragon seed"),
    Cadantine("Cadantine seed"),
    Lantadyme("Lantadyme seed"),
    Dwarf_weed("Dwarf weed seed"),
    Torstol("Torstol seed")
    ;

    public final String SeedName;
    Herb(String itemName)
    {
        this.SeedName = itemName;
    }
}
