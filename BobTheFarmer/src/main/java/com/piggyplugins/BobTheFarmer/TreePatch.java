package com.piggyplugins.BobTheFarmer;

import net.runelite.api.coords.WorldPoint;

import java.util.Dictionary;
import java.util.Hashtable;

//Tree patch data structure
//This is to allow the bot the know where it is by knowing where it isn't
public class TreePatch {
    public String Name = "";
    public String Gardener = "";
    public String[] Tools = {};
    public TreePatchState State = TreePatchState.NOT_STARTED;
    public Dictionary<String, WorldPoint[]> Paths = new Hashtable<>();
    public int PathIndex = 0;

    public TreePatch(String name, String gardener, String[] tools)
    {
        this.Name = name;
        this.Gardener = gardener;
        this.Tools = tools;
        this.PathIndex = 0;
    }
    public void SetPath(WorldPoint[] path, String key)
    {
        Paths.put(key, path);
    }
}
