package com.piggyplugins.BobTheHunter;

import com.example.EthanApiPlugin.Collections.TileObjects;
import com.example.EthanApiPlugin.Collections.query.TileObjectQuery;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.Packets.MovementPackets;
import com.google.inject.Provides;
import com.piggyplugins.PiggyUtils.API.TileItemUtil;
import com.piggyplugins.PiggyUtils.BreakHandler.ReflectBreakHandler;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;

import com.google.inject.Inject;
import org.apache.commons.lang3.RandomUtils;

import java.util.List;


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
            // We do an early return if the user isn't logged in
            return;
        }

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
            case PLACE_TRAPS:
                break;
            case SUCCESSFUL_TRAPS:
                break;
            case FAILED_TRAPS:
                break;
            case WAIT:
                playerWait();
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

        if (breakHandler.shouldBreak(this)) {
            return State.HANDLE_BREAK;
        }

        if (!GetSuccessfulTraps().isEmpty())
        {
            return State.SUCCESSFUL_TRAPS;
        }
        if (!GetFailedTraps().isEmpty())
        {
            return State.FAILED_TRAPS;
        }

        if (GetActiveTraps().result().size() < config.trapCount())
        {
            return State.PLACE_TRAPS;
        }

        return State.WAIT;
    }

    private List<TileObject> GetSuccessfulTraps()
    {

        return GetActiveTraps().withAction("Check").result();
    }

    private List<TileObject> GetFailedTraps()
    {
        List<TileObject> allTraps = GetActiveTraps().result();
        List<TileObject> successfulTraps = GetSuccessfulTraps();

        allTraps.removeAll(successfulTraps);

        return allTraps;
    }

    private TileObjectQuery GetActiveTraps()
    {
        return TileObjects.search().nameContains(TrapToString());
    }

    private List<TileObject> GetTimeoutTraps()
    {
        return GetActiveTraps().withAction("Lay").result();
    }

    private void placeTraps()
    {

    }

    private void playerWait()
    {
        if (client.getLocalPlayer().getWorldLocation() != startTile)
        {
            MovementPackets.queueMovement(startTile);
        }
    }

    private String TrapToString()
    {
        switch (config.trapType()){
            case BIRD_SNARE:
                return "Bird Snare";
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
        } else {
            breakHandler.startPlugin(this);
            startTile = client.getLocalPlayer().getWorldLocation();
        }
    }
}
