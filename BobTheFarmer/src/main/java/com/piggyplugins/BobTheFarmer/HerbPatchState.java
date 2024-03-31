package com.piggyplugins.BobTheFarmer;

//Possible herb patch states
public enum HerbPatchState
{
    NOT_STARTED(0),
    TRAVEL(1),
    PROCESS_HERB_PATCH(2),
    HARVEST(3),
    CLEAR(4),
    RAKE(5),
    PLANTING(6),
    COMPOST(7),
    EMPTY_INVENTORY(8),
    NOTE(9),
    DONE(10);

    public final int Index;
    private HerbPatchState(int index)
    {
        this.Index = index;
    }
}
