package com.piggyplugins.BobTheChef;

import com.example.EthanApiPlugin.Collections.TileObjects;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.PrayerInteraction;
import com.example.InteractionApi.TileObjectInteraction;
import com.example.Packets.MousePackets;
import com.example.Packets.TileItemPackets;
import com.example.Packets.WidgetPackets;
import com.google.inject.Provides;
import com.piggyplugins.PiggyUtils.API.InventoryUtil;
import com.piggyplugins.PiggyUtils.BreakHandler.ReflectBreakHandler;
import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.crowdsourcing.dialogue.PlayerDialogueData;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;

import com.google.inject.Inject;
import org.apache.commons.lang3.RandomUtils;

import java.awt.event.KeyEvent;


@PluginDescriptor(
        name = "<html><font color=\"#FF9DF9\">[PP]</font> Bob The Chef</html>",
        description = "Bob goes hunting",
        tags = {"ethan", "piggy", "skilling"}
)
public class BobTheChefPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    BobTheChefConfig config;
    @Inject
    private KeyManager keyManager;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private BobTheChefOverlay overlay;
    @Inject
    private ReflectBreakHandler breakHandler;
    State state;
    boolean started;
    private int timeout;

    TileObject cookingRangeObject = null;
    TileObject bankObject = null;

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
    private BobTheChefConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(BobTheChefConfig.class);
    }

    @Subscribe
    private void onGameTick(GameTick event) {
        if (!EthanApiPlugin.loggedIn() || !started || breakHandler.isBreakActive(this)) {
            // We do an early return if the user isn't logged in
            cookingRangeObject = null;
            bankObject = null;
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
            case COOK:

                break;
            case BANK:

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

        if (InventoryUtil.getItem(config.rawFood()).isPresent())
        {
            return State.COOK;
        }
        else {
            return State.BANK;
        }
    }

    private void cook()
    {
        if (cookingRangeObject == null)
        {
            cookingRangeObject = TileObjects.search().withAction("Cook").nearestToPlayer().get();
        }

        if (cookingRangeObject == null)
        {
            started = false;
            return;
        }

        InventoryUtil.getItem(config.rawFood()).get();
        
        TileObjectInteraction.interact(cookingRangeObject, "Cook");

        MousePackets.queueClickPacket();
        MousePackets.queueClickPacket();
        //TileItemPackets.queueWidgetOnTileItem(cookingRangeObject, InventoryUtil.getItem(config.rawFood()).get(), false);
        //WidgetPackets.queueWidgetOnWidget(cookingRangeObject, InventoryUtil.getItem(config.rawFood()).get());
    }
    private void bank()
    {

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
        }
    }
}
