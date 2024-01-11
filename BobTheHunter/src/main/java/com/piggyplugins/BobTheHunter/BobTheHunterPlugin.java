package com.piggyplugins.BobTheHunter;

import com.example.EthanApiPlugin.Collections.ETileItem;
import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.Collections.TileItems;
import com.example.EthanApiPlugin.Collections.TileObjects;
import com.example.EthanApiPlugin.Collections.query.ItemQuery;
import com.example.EthanApiPlugin.Collections.query.PlayerQuery;
import com.example.EthanApiPlugin.Collections.query.TileObjectQuery;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.InventoryInteraction;
import com.example.InteractionApi.TileObjectInteraction;
import com.example.Packets.MousePackets;
import com.example.Packets.MovementPackets;
import com.example.Packets.PlayerPackets;
import com.example.Packets.TileItemPackets;
import com.google.inject.Provides;
import com.piggyplugins.PiggyUtils.API.InventoryUtil;
import com.piggyplugins.PiggyUtils.API.PlayerUtil;
import com.piggyplugins.PiggyUtils.API.TileItemUtil;
import com.piggyplugins.PiggyUtils.BreakHandler.ReflectBreakHandler;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;

import com.google.inject.Inject;
import net.runelite.client.util.Text;
import org.apache.commons.lang3.RandomUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;


@PluginDescriptor(
        name = "<html><font color=\"#FF9DF9\">[PP]</font> Bob The Hunter</html>",
        description = "Bob goes hunting",
        tags = {"ethan", "piggy", "skilling"}
)
public class BobTheHunterPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    BobTheHunterConfig config;
    @Inject
    private KeyManager keyManager;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private BobTheHunterOverlay overlay;
    @Inject
    private ReflectBreakHandler breakHandler;
    State state;
    boolean started;
    private int timeout;
    WorldPoint startTile;
    WorldPoint targetTile;
    WorldPoint playerTile;
    String debugInfo = "";

    boolean resetStartTile = true;

    @Override
    protected void startUp() throws Exception {
        keyManager.registerKeyListener(toggle);
        breakHandler.registerPlugin(this);
        this.overlayManager.add(overlay);
    }

    @Override
    protected void shutDown() throws Exception {
        keyManager.unregisterKeyListener(toggle);
        breakHandler.unregisterPlugin(this);
        this.overlayManager.remove(overlay);
    }

    @Provides
    private BobTheHunterConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(BobTheHunterConfig.class);
    }

    @Subscribe
    private void onGameTick(GameTick event) {
        if (!EthanApiPlugin.loggedIn() || !started || breakHandler.isBreakActive(this)) {
            startTile = null;
            resetStartTile = true;
            // We do an early return if the user isn't logged in
            return;
        }

        getStartTile();

        playerTile = client.getLocalPlayer().getWorldLocation();

        debugInfo = Integer.toString(GetActiveTraps().result().size());

        state = getNextState();
        handleState();
    }

    private void handleState() {
        switch (state) {
            case HANDLE_BREAK:
                breakHandler.startBreak(this);
                timeout = 10;
                break;
            case TIMEOUT:
                timeout--;
                break;
            case DROP_ITEMS:
                DropItems();
                break;
            case TIMEOUT_TRAPS:
                PickUpTimeoutTraps();
                targetTile = null;
                setTimeout();
                break;
            case MOVE_TO_TILE:
                moveToTargetTile();
                targetTile = null;
                break;
            case PLACE_TRAPS:
                placeTraps();
                break;
            case SUCCESSFUL_TRAPS:
                ClearSuccessfulTrap();
                targetTile = null;
                setTimeout();
                break;
            case FAILED_TRAPS:
                ClearFailedTraps();
                targetTile = null;
                setTimeout();
                break;
            case WAIT:
                playerWait();
                targetTile = null;
                break;
        }
    }

    private State getNextState() {
        if (EthanApiPlugin.isMoving()) {
            return State.ANIMATING;
        }
        if (timeout > 0) {
            return State.TIMEOUT;
        }
        if (!GetTimeoutTraps().isEmpty())
        {
            return State.TIMEOUT_TRAPS;
        }
        if (targetTile != null && !playerAtTarget()) {
            return State.MOVE_TO_TILE;
        }
        if (GetActiveTraps().result().size() + (config.trapType() == TrapType.BOX_TRAP ? GetSuccessfulTraps().size() : 0) < config.trapCount())
        {
            return State.PLACE_TRAPS;
        }
        if (breakHandler.shouldBreak(this)) {
            return State.HANDLE_BREAK;
        }
        if ((InventoryUtil.getItems().size() > 24 || state == State.DROP_ITEMS) && !isInventoryReset())
        {
            return State.DROP_ITEMS;
        }
        if (!GetSuccessfulTraps().isEmpty())
        {
            return State.SUCCESSFUL_TRAPS;
        }
        if (!GetFailedTraps().isEmpty())
        {
            return State.FAILED_TRAPS;
        }
        return State.WAIT;
    }

    private void getStartTile()
    {
        if (startTile == null || resetStartTile)
        {
            startTile = client.getLocalPlayer().getWorldLocation();
            resetStartTile = false;
        }
    }

    private List<TileObject> GetSuccessfulTraps()
    {
        return TileObjects.search().nameContains(TrapToString(true)).withinDistance(5).withAction("Check").result();
    }

    private List<TileObject> GetFailedTraps()
    {
        List<TileObject> traps = TileObjects.search().nameContains(TrapToString(false)).withinDistance(5).withAction("Dismantle").result();
        traps.removeAll(TileObjects.search().nameContains(TrapToString(false)).withinDistance(5).withAction("Investigate").result());
        return traps;
    }

    private TileObjectQuery GetActiveTraps()
    {
        return TileObjects.search().nameContains(TrapToString(false)).withinDistance(5);
    }

    private List<ETileItem> GetTimeoutTraps()
    {
        return TileItems.search().nameContains(TrapToString(false)).result();
    }

    private WorldPoint GetTrapPositions(WorldPoint startTile, int index, boolean fiveTraps)
    {
        WorldPoint retPoint = new WorldPoint(startTile.getX(), startTile.getY(), startTile.getPlane());
        if (fiveTraps)
        {

        }
        else
        {
            switch (index)
            {
                case 0:
                    retPoint = retPoint.dx(1);
                    break;
                case 1:
                    retPoint = retPoint.dy(1);
                    break;
                case 2:
                    retPoint = retPoint.dx(-1);
                    break;
                case 3:
                    retPoint = retPoint.dy(-1);
                    break;
            }
        }
        return retPoint;
    }

    private boolean playerAtTarget()
    {
        return client.getLocalPlayer().getWorldLocation().getX() == targetTile.getX() && client.getLocalPlayer().getWorldLocation().getY() == targetTile.getY();
    }

    private void moveToTargetTile()
    {
        //Move the player to the trap location
        MousePackets.queueClickPacket();
        MovementPackets.queueMovement(targetTile);

        if (playerAtTarget())
            targetTile = null;
    }

    private void placeTraps()
    {
        Optional<Widget> trap = InventoryUtil.nameContainsNoCase(TrapToString(false).toLowerCase()).first();
        if (trap.isEmpty())
            return;

        for (int i = 0; i < config.trapCount(); i++)
        {
            WorldPoint trapTarget = GetTrapPositions(startTile, i, config.trapCount() >= 5);
            TileObjectQuery tileQuery = GetActiveTraps();

            if (!tileQuery.atLocation(trapTarget).result().isEmpty())
                continue;

            tileQuery = TileObjects.search().nameContains(TrapToString(true)).withinDistance(5).withAction("Check");

            if (!tileQuery.atLocation(trapTarget).result().isEmpty())
                continue;

            targetTile = trapTarget;
            if (!playerAtTarget())
                break;
            targetTile = null;

            MousePackets.queueClickPacket();
            InventoryInteraction.useItem(trap.get(), "Lay");
            timeout = 3;
        }
    }

    private void ClearSuccessfulTrap()
    {
        for (TileObject getSuccessfulTrap : GetSuccessfulTraps()) {
            MousePackets.queueClickPacket();
            TileObjectInteraction.interact(getSuccessfulTrap, "Check");
        }
    }

    private void ClearFailedTraps()
    {
        for (TileObject getSuccessfulTrap : GetFailedTraps()) {
            MousePackets.queueClickPacket();
            TileObjectInteraction.interact(getSuccessfulTrap, "Dismantle");
        }
    }

    private void PickUpTimeoutTraps()
    {
        if (TileItems.search().nameContains(TrapToString(false)).withinDistance(5).nearestToPlayer().isPresent())
        {
            MousePackets.queueClickPacket();
            TileItemPackets.queueTileItemAction(TileItems.search().nameContains(TrapToString(false)).withinDistance(5).nearestToPlayer().get(), false);
        }
    }

    private void DropItems()
    {
        //This is now mine
        List<Widget> itemsToDrop = Inventory.search()
                .filter(item -> !shouldKeep(item.getName())).result();

        for (int i = 0; i < Math.min(itemsToDrop.size(), RandomUtils.nextInt(1, 3)); i++) {
            MousePackets.queueClickPacket();
            if (itemsToDrop.get(i).getName().contains("Ferret"))
            {
                InventoryInteraction.useItem(itemsToDrop.get(i), "Release");
            }
            else
            {
                InventoryInteraction.useItem(itemsToDrop.get(i), "Drop");
            }
        }
    }

    private boolean shouldKeep(String name) {
        List<String> itemsToKeep = new ArrayList<>(List.of(config.itemsToKeep().split(",")));
        itemsToKeep.add(TrapToString(false));
        return itemsToKeep.stream()
                .anyMatch(i -> Text.removeTags(name.toLowerCase()).contains(i.toLowerCase()));
    }

    private boolean isInventoryReset() {
        List<Widget> inventory = Inventory.search().result();
        for (Widget item : inventory) {
            if (!shouldKeep(Text.removeTags(item.getName()))) { 
                return false;
            }
        }
        return true;
    }

    private void playerWait()
    {
        if (client.getLocalPlayer().getWorldLocation().getX() != startTile.getX() || client.getLocalPlayer().getWorldLocation().getY() != startTile.getY())
        {
            MousePackets.queueClickPacket();
            MovementPackets.queueMovement(startTile);
        }
        else
        {
            DropItems();
        }
    }

    private String TrapToString(boolean catched)
    {
        switch (config.trapType()){
            case BIRD_SNARE:
                return "Bird snare";
            case BOX_TRAP:
                return catched ? "Shaking box" : "Box trap";
            default:
                return "";
        }
    }

    private void setTimeout() {
        timeout = RandomUtils.nextInt(config.tickdelayMin(), config.tickDelayMax());
    }

    private final HotkeyListener toggle = new HotkeyListener(() -> config.toggle()) {
        @Override
        public void hotkeyPressed() {
            toggle();
        }
    };

    public void toggle() {
        if (client.getGameState() != GameState.LOGGED_IN) {
            return;
        }
        started = !started;

        if (!started) {
            this.state = State.TIMEOUT;
            breakHandler.stopPlugin(this);
            resetStartTile = true;
            startTile = null;
        } else {
            breakHandler.startPlugin(this);
            startTile = client.getLocalPlayer().getWorldLocation();
        }
    }
}
