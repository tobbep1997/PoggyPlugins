package com.piggyplugins.BobTheFunction;

import com.example.EthanApiPlugin.Collections.BankInventory;
import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.Collections.TileObjects;
import com.example.EthanApiPlugin.Collections.query.TileObjectQuery;
import com.example.InteractionApi.BankInteraction;
import com.example.InteractionApi.BankInventoryInteraction;
import com.example.InteractionApi.TileObjectInteraction;
import com.example.Packets.MousePackets;
import com.example.Packets.WidgetPackets;
import com.piggyplugins.PiggyUtils.API.BankUtil;
import net.runelite.api.Client;
import net.runelite.api.ObjectComposition;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class Bank {

    //------------------------------------- Public -------------------------------------

    //Opens the closes bank
    public static void OpenBank(Client client, int maxDistance) {
        TileObject bankBooth = BankCloseBy(client, maxDistance);
        if (bankBooth != null) {
            MousePackets.queueClickPacket();
            TileObjectInteraction.interact(bankBooth, GetBankInteraction(bankBooth));
        }
    }

    //Find close banks
    public static TileObject BankCloseBy(Client client, int maxDistance) {
        Optional<TileObject> bankBooth = null;
        bankBooth = TileObjects.search().withAction("Bank").withinDistance(maxDistance).nearestToPlayer();

        Optional<TileObject> bankChest = null;
        bankChest = TileObjects.search().withName("Bank chest").withinDistance(maxDistance).nearestToPlayer();

        if (bankBooth.isPresent() && bankChest.isPresent())
        {
            WorldPoint playerPos = client.getLocalPlayer().getWorldLocation();

            int bbDist = CalculateDistance(playerPos.getX(), playerPos.getY(), bankBooth.get().getX(), bankBooth.get().getY());
            int bcDist = CalculateDistance(playerPos.getX(), playerPos.getY(), bankChest.get().getX(), bankChest.get().getY());

            return bbDist < bcDist ? bankBooth.get() : bankChest.get();
        }
        if (bankBooth.isPresent())
            return bankBooth.get();
        if (bankChest.isPresent())
            return bankChest.get();
        return null;
    }



    //Set the bank to withdraw in either noted or unnoted form
    public static boolean setWithdrawNotes(boolean noted) {
        if (!com.example.EthanApiPlugin.Collections.Bank.isOpen()) return false;
        if (com.example.EthanApiPlugin.Collections.Bank.isOpen()) {
            if (noted)
            {
                MousePackets.queueClickPacket();
                WidgetPackets.queueWidgetActionPacket(1, 786456, -1, -1);
                return true;
            }
            else
            {
                MousePackets.queueClickPacket();
                WidgetPackets.queueWidgetActionPacket(1, 786454, -1, -1);
                return true;
            }
        }
        return false;
    }

    //Returns if the bank is currently set to noted or unnoted
    public static boolean getWithdrawNotes(Client client) {
        return client.getVarbitValue(3958) == 1;
    }

    public static boolean DepositItems(ArrayList<String> deposit)
    {
        List<Widget> bankInv = BankInventory.search().filter(widget -> !CompareItem(deposit, widget.getName())).result();

        if (bankInv.isEmpty())
            return false;

        MousePackets.queueClickPacket();
        BankInventoryInteraction.useItem(bankInv.get(0), "Deposit-All");

        return true;
    }


    //Take out a set amount of items from the bank
    //This double checks if you have any items in the invetory already so it dosen't withdraw too much
    public static boolean WithdrawItemFromBank(String item, int amount) {
        AtomicBoolean succeeded = new AtomicBoolean(false);
        BankUtil.nameContainsNoCase(item).first().ifPresentOrElse(widget ->
            {
                int localAmount = amount -
                    (
                        Inventory.search().withName(item).first().isPresent() ?
                            (
                                Inventory.search().isStackable(Inventory.search().withName(item).first().get()) ?
                                        Inventory.search().withName(item).first().get().getItemQuantity() :
                                        Inventory.search().withName(item).result().size()
                            ) : 0
                    );

                if (amount < 0)
                    localAmount = BankUtil.getItemAmount(widget.getItemId());

                if (localAmount > 0)
                {
                    MousePackets.queueClickPacket();
                    BankInteraction.withdrawX(widget, localAmount);
                }

                succeeded.set(true);
            },
            () -> {
                succeeded.set(false);
            });

        if (Inventory.search().withName(item).first().isPresent())
            succeeded.set(true);

        return succeeded.get();
    }


    //------------------------------------- Private -------------------------------------

    //Checks if an item is in the keepItems list
    private static boolean CompareItem(List<String> keepItems, String compItem) {
        for (String item : keepItems)
        {
            if (compItem.contains(item))
                return true;
        }
        return false;
    }

    private static int CalculateDistance(int x1, int y1, int x2, int y2)
    {
        return (int)Math.round(Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2)));
    }

    //Returns the interaction of a bank object
    private static String GetBankInteraction(TileObject bankObject) {
        ObjectComposition objectComposition = TileObjectQuery.getObjectComposition(bankObject);
        return Arrays.stream(objectComposition.getActions()).anyMatch(action -> action != null && action.toLowerCase().contains("bank")) ? "Bank" : "Use";
    }

}
