package com.piggyplugins.BobTheFunction;

import net.runelite.api.coords.WorldPoint;

public class BobThePath {

    public int pathIndex;
    public WorldPoint[] worldPoints;

    public BobThePath(WorldPoint[] worldPoints)
    {
        this.pathIndex = 0;
        this.worldPoints = worldPoints;
    }

    public void NextWorldPoint()
    {
        this.pathIndex++;
    }

    public boolean Done()
    {
        return pathIndex >= worldPoints.length;
    }

    public WorldPoint GetWorldPoint()
    {
        if (pathIndex < worldPoints.length)
            return worldPoints[pathIndex];
        else
            return null;
    }

}
