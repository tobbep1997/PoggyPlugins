package com.piggyplugins.BobTheFarmer;

public enum State {
    OFF,
    ANIMATING,
    TIMEOUT,

    //Herb runs
    RESTOCK_HERB,
    HERB_TRAVEL_ARDOUGNE,
    HERB_ARDOUGNE,
    HERB_TRAVEL_FALADOR,
    HERB_FALADOR,
    HERB_TRAVEL_CATHERBY,
    HERB_CATHERBY,

    //Tree runs
    RESTOCK_TREE,
    TREE_TRAVEL_FALADOR,
    TREE_FALADOR,
    TREE_TRAVEL_LUMBRIDGE,
    TREE_LUMBRIDGE
}

