package com.piggyplugins.BobTheFarmer;

public enum ProcessState
{
    NOT_STARTED(0),
    TRAVEL(1),
    PROCESS_HERB_PATCH(2),
    HARVEST(3),
    CLEARING(4),
    PLANTING(5),
    COMPOST(6),
    EMPTY_INVENTORY(7),
    DONE(8);

    public final int Index;
    private ProcessState(int index)
    {
        this.Index = index;
    }
}
