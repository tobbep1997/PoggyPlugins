package com.piggyplugins.BobTheFarmer;

import net.runelite.api.coords.WorldPoint;

import java.util.Dictionary;
import java.util.Hashtable;

public class HerbPatch
{
    public String Name = "";
    public String[] Tools = {};
    public HerbPatchState State = HerbPatchState.NOT_STARTED;
    public Dictionary<String, WorldPoint[]> Paths = new Hashtable<>();
    public int PathIndex = 0;

    public HerbPatch(String name, String[] tools)
    {
        this.Name = name;
        this.Tools = tools;
        this.PathIndex = 0;
    }
    public void SetPath(WorldPoint[] path, String key)
    {
        Paths.put(key, path);
    }
}
