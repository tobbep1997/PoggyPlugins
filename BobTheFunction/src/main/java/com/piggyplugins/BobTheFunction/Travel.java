package com.piggyplugins.BobTheFunction;

import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.Packets.MousePackets;
import com.example.Packets.MovementPackets;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;

public class Travel {
    //Check is the player is on the targetTile
    public static boolean playerAtTarget(Client client, WorldPoint targetTile) {
        return client.getLocalPlayer().getWorldLocation().getX() == targetTile.getX() && client.getLocalPlayer().getWorldLocation().getY() == targetTile.getY();
    }

    //Iterates through a WorldPoint array and moves the player to next point
    public static boolean TravelPath(Client client, Path path) {
        if (EthanApiPlugin.isMoving())
            return false;

        if (playerAtTarget(client, path.GetWorldPoint()))
            path.NextWorldPoint();

        if (path.Done())
            return  true;

        //Move the player to the trap location
        MousePackets.queueClickPacket();
        MovementPackets.queueMovement(path.GetWorldPoint());

        return false;
    }

}
