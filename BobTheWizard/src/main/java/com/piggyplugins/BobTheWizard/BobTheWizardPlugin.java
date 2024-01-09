package com.piggyplugins.BobTheWizard;

import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.Collections.Widgets;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.PacketUtils.WidgetInfoExtended;
import com.example.Packets.MousePackets;
import com.example.Packets.WidgetPackets;
import com.google.inject.Provides;
import com.piggyplugins.PiggyUtils.BreakHandler.ReflectBreakHandler;
import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;

import com.google.inject.Inject;
import org.apache.commons.lang3.RandomUtils;

import java.util.Optional;


@PluginDescriptor(
        name = "<html><font color=\"#FF9DF9\">[PP]</font> Bob The Wizard</html>",
        description = "Magic goes brrr",
        tags = {"ethan", "piggy", "skilling"}
)
public class BobTheWizardPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    BobTheWizardConfig config;
    @Inject
    private KeyManager keyManager;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private BobTheWizardOverlay overlay;
    @Inject
    private ReflectBreakHandler breakHandler;
    State state;
    boolean started;
    private int timeout;
    private int teleportTimeout;
    private boolean alced;
    private final int TELEPORT_ANIMATION_ID = 714;
    private final int HIGH_ALC_ANIMATION_ID = 713;


    private final String Dodgy_Necklace = "Dodgy necklace";

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
    private BobTheWizardConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(BobTheWizardConfig.class);
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
            case HIGH_ALCING:
                teleportTimeout--;
                break;
            case TELEPORTING:
                alced = false;
                break;
            case CAST_HIGH_ALC:
                if (!alced)
                {
                    alc();
                    setTeleportTimeout();
                    alced = true;
                }
                break;
            case CAST_TELEPORT:
                teleport();
                setTimeout();
                break;
        }
    }

    private State getNextState() {
        if (EthanApiPlugin.isMoving()) {
            return State.ANIMATING;
        }

        if (client.getLocalPlayer().getAnimation() == TELEPORT_ANIMATION_ID){
            return State.TELEPORTING;
        }

        if (client.getLocalPlayer().getAnimation() == HIGH_ALC_ANIMATION_ID && teleportTimeout > 0){
            return State.HIGH_ALCING;
        }

        if (timeout > 0) {
            return State.TIMEOUT;
        }

        if (breakHandler.shouldBreak(this)) {
            return State.HANDLE_BREAK;
        }

        if (alced || config.alc().isEmpty())
        {
            return State.CAST_TELEPORT;
        }

        return State.CAST_HIGH_ALC;
    }

    private void alc()
    {
        String[] itemsToAlch = config.alc().replace(", ", ",").split(",");
        Widget highAlch = client.getWidget(WidgetInfoExtended.SPELL_HIGH_LEVEL_ALCHEMY.getPackedId());
        if (itemsToAlch.length > 0 && highAlch != null) {
            Inventory.search().onlyStackable().matchesWildCardNoCase(itemsToAlch[0]).first().ifPresentOrElse(item -> {
                MousePackets.queueClickPacket();
                WidgetPackets.queueWidgetOnWidget(highAlch, item);
            }, null);
        }
    }
    private void teleport()
    {
        WidgetInfoExtended teleportEnum = null;
        switch (config.teleport())
        {
            case VARROCK:
                teleportEnum = WidgetInfoExtended.SPELL_VARROCK_TELEPORT;
                break;
            case LUMBRIDGE:
                teleportEnum = WidgetInfoExtended.SPELL_LUMBRIDGE_TELEPORT;
                break;
            case FALADOR:
                teleportEnum = WidgetInfoExtended.SPELL_FALADOR_TELEPORT;
                break;
            case CAMELOT:
                teleportEnum = WidgetInfoExtended.SPELL_CAMELOT_TELEPORT;
                break;
            case ARDOUGNE:
                teleportEnum = WidgetInfoExtended.SPELL_ARDOUGNE_TELEPORT;
                break;
            case WATCHTOWER:
                teleportEnum = WidgetInfoExtended.SPELL_WATCHTOWER_TELEPORT;
                break;
            case TROLLHEIM:
                teleportEnum = WidgetInfoExtended.SPELL_TROLLHEIM_TELEPORT;
                break;
            case APE_ATOLL:
                teleportEnum = WidgetInfoExtended.SPELL_APE_ATOLL_TELEPORT;
                break;
            case KOUREND:
                teleportEnum = WidgetInfoExtended.SPELL_TELEPORT_TO_KOUREND;
                break;
        }

        if (teleportEnum == null)
            return;

        Optional<Widget> teleportSpellIcon = Widgets.search().withId(teleportEnum.getPackedId()).first();
        if (teleportSpellIcon.isPresent()) {
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetAction(teleportSpellIcon.get(), "Cast");
        }

    }

    private void setTimeout() {
        timeout = RandomUtils.nextInt(config.tickdelayMin(), config.tickDelayMax());
    }

    private void setTeleportTimeout() {
        teleportTimeout = RandomUtils.nextInt(config.teleportTickDelayMin(), config.teleportTickDelayMax());
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
