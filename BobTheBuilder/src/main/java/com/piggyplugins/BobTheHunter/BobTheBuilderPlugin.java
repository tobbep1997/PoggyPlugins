package com.piggyplugins.BobTheHunter;

import com.example.EthanApiPlugin.Collections.*;
import com.example.EthanApiPlugin.Collections.query.ItemQuery;
import com.example.EthanApiPlugin.Collections.query.PlayerQuery;
import com.example.EthanApiPlugin.Collections.query.TileItemQuery;
import com.example.EthanApiPlugin.Collections.query.TileObjectQuery;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.InventoryInteraction;
import com.example.InteractionApi.TileObjectInteraction;
import com.example.Packets.*;
import com.google.inject.Provides;
import com.piggyplugins.PiggyUtils.API.InventoryUtil;
import com.piggyplugins.PiggyUtils.API.PlayerUtil;
import com.piggyplugins.PiggyUtils.API.TileItemUtil;
import com.piggyplugins.PiggyUtils.BreakHandler.ReflectBreakHandler;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.menus.WidgetMenuOption;
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
        name = "<html><font color=\"#FF9DF9\">[PP]</font> Bob The Builder</html>",
        description = "Bob goes building",
        tags = {"ethan", "piggy", "skilling"}
)
public class BobTheBuilderPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    BobTheBuilderConfig config;
    @Inject
    private KeyManager keyManager;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private BobTheBuilderOverlay overlay;
    @Inject
    private ReflectBreakHandler breakHandler;
    State state;
    boolean started;
    private int timeout;
    WorldPoint startTile;
    WorldPoint targetTile;
    boolean takeBreak = false;
    public String debug = "";


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
    private BobTheBuilderConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(BobTheBuilderConfig.class);
    }

    @Subscribe
    private void onGameTick(GameTick event) {
        if (!EthanApiPlugin.loggedIn() || !started || breakHandler.isBreakActive(this)) {
            startTile = null;
            resetStartTile = true;
            // We do an early return if the user isn't logged in
            return;
        }

        state = getNextState();
        handleState();
    }

    private void handleState() {
        switch (state) {
            case HANDLE_BREAK:
                takeBreak = false;
                breakHandler.startBreak(this);
                timeout = 10;
                break;
            case TIMEOUT:
                timeout--;
                break;
            case TELEPORT_TO_HOUSE:
                TeleportToHouse();
                setTimeout();
                break;
            case BUILD:
                Build();
                break;
            case REMOVE:
                Remove();
                break;
        }
    }

    private State getNextState() {
        if (EthanApiPlugin.isMoving()) {
            return State.ANIMATING;
        }
        if (breakHandler.shouldBreak(this)) {
            takeBreak = true;
        }
        if (timeout > 0) {
            return State.TIMEOUT;
        }
        if (hasItems())
        {
            TileObjectQuery tileObjectQuery = TileObjects.search().nameContains(config.build());
            if (tileObjectQuery.empty())
                return State.TELEPORT_TO_HOUSE;
            else
            {
                TileObject tileObject = tileObjectQuery.nearestToPlayer().get();
                ObjectComposition objectComposition = TileObjectQuery.getObjectComposition(tileObject);
                List<String> actions = Arrays.asList(objectComposition.getActions());

                if (actions.contains("Build"))
                {
                    return State.BUILD;
                }
                if (actions.contains("Remove"))
                {
                    return State.REMOVE;
                }
            }
        }

        return State.TIMEOUT;
    }

    private void TeleportToHouse()
    {

    }
    private void TeleportToVarrock()
    {

    }

    private void Build()
    {
        debug = "build";
        Widgets.search().withTextContains("Furniture Creation Menu").hiddenState(false).first().ifPresentOrElse(menu -> {
            debug = "Found menu";
            Widgets.search().withTextContains("2").hiddenState(false).first().ifPresent(option ->{
                debug =  Integer.toString(option.getId());
                MousePackets.queueClickPacket();
                WidgetPackets.queueResumePause(option.getId(), -1);
                setTimeout();
            });
        }, () -> {
            TileObjects.search().nameContains(config.build()).nearestToPlayer().ifPresent(tileObject -> {
                ObjectComposition objectComposition = TileObjectQuery.getObjectComposition(tileObject);
                List<String> actions = Arrays.asList(objectComposition.getActions());
                if (actions.contains("Build"))
                {
                    debug = "Open menu";
                    MousePackets.queueClickPacket();
                    TileObjectInteraction.interact(tileObject, "Build");
                }
            });
        });
    }

    private void Remove()
    {
        debug = "remove";
        Widgets.search().withTextContains("Really remove it").hiddenState(false).first().ifPresentOrElse(menu -> {
            debug = "Found menu";
            Widgets.search().withTextContains("Yes").hiddenState(false).first().ifPresent(option ->{
                debug =  "Interact";
                MousePackets.queueClickPacket();
                WidgetPackets.queueResumePause(option.getId(), 1);
                setTimeout();
            });
        }, () -> {
            TileObjects.search().nameContains(config.build()).nearestToPlayer().ifPresent(tileObject -> {
                ObjectComposition objectComposition = TileObjectQuery.getObjectComposition(tileObject);
                List<String> actions = Arrays.asList(objectComposition.getActions());
                if (actions.contains("Remove"))
                {
                    debug = "Open menu";
                    MousePackets.queueClickPacket();
                    TileObjectInteraction.interact(tileObject, "Remove");
                }
            });
        });
    }

    private boolean playerAtTarget()
    {
        return client.getLocalPlayer().getWorldLocation().getX() == targetTile.getX() && client.getLocalPlayer().getWorldLocation().getY() == targetTile.getY();
    }

    private boolean hasItems()
    {
        String[] items = config.items().split(",");
        boolean hasItems = true;
        for (int i = 0; i < items.length; i++) {
            if (!InventoryUtil.hasItem(items[i]))
                hasItems = false;
        }
        return hasItems;
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
