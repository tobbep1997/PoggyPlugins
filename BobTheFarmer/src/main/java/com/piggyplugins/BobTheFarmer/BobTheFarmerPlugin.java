package com.piggyplugins.BobTheFarmer;

import com.example.EthanApiPlugin.Collections.*;
import com.example.EthanApiPlugin.Collections.query.TileObjectQuery;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.BankInteraction;
import com.example.InteractionApi.BankInventoryInteraction;
import com.example.InteractionApi.TileObjectInteraction;
import com.example.PacketUtils.WidgetInfoExtended;
import com.example.Packets.*;
import com.google.inject.Provides;
import com.piggyplugins.PiggyUtils.API.BankUtil;
import com.piggyplugins.PiggyUtils.API.InventoryUtil;
import com.piggyplugins.PiggyUtils.BreakHandler.ReflectBreakHandler;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;


@PluginDescriptor(
        name = "<html><font color=\"#FF9DF9\">[PP]</font> Bob The Farmer</html>",
        description = "Bob goes Farming",
        tags = {"ethan", "piggy", "skilling"}
)
public class BobTheFarmerPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    BobTheFarmerConfig config;
    @Inject
    private KeyManager keyManager;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private BobTheFarmerOverlay overlay;
    @Inject
    private ReflectBreakHandler breakHandler;
    State state;
    boolean started;
    private int timeout;
    public String debug = "";
    private boolean hasStocked = false;


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
    private BobTheFarmerConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(BobTheFarmerConfig.class);
    }

    @Subscribe
    private void onGameTick(GameTick event) {
        if (!EthanApiPlugin.loggedIn() || !started || breakHandler.isBreakActive(this)) {
            // We do an early return if the user isn't logged in
            hasStocked = false;
            return;
        }

        state = getNextState();
        handleState();
    }

    private void handleState() {
        if (state == null)
            return;

        switch (state) {
            case ANIMATING:
                timeout = 5;
                break;
            case HANDLE_BREAK:
                breakHandler.startBreak(this);
                timeout = 10;
                break;
            case TIMEOUT:
                timeout--;
                break;
            case RESTOCK:
                Restock();
                break;
        }
    }

    private State getNextState() {
        if (EthanApiPlugin.isMoving() || client.getLocalPlayer().getAnimation() != -1) {
            return State.ANIMATING;
        }
        if (breakHandler.shouldBreak(this)) {
            return State.HANDLE_BREAK;
        }
        if (timeout > 0) {
            return State.TIMEOUT;
        }
        if (!hasStocked)
            return State.RESTOCK;

        if (config.enableArdougne())
            return State.TRAVLE_ARDOUGNE;


        return null;
    }



    private void Restock()
    {
        if (Bank.isOpen()) {
            List<Widget> bankInv = BankInventory.search().result();
            for (Widget item : bankInv) {
                MousePackets.queueClickPacket();
                BankInventoryInteraction.useItem(item, "Deposit-All");
            }

            BankUtil.nameContainsNoCase(config.items1()).first().ifPresentOrElse(widget ->
                        BankInteraction.withdrawX(widget, config.items1Amount()),
                    () -> {
                        started = false;
                        this.state = State.TIMEOUT;
                        breakHandler.stopPlugin(this);
            });

            BankUtil.nameContainsNoCase(config.items2()).first().ifPresentOrElse(widget ->
                            BankInteraction.withdrawX(widget, config.items2Amount()),
                    () -> {
                        started = false;
                        this.state = State.TIMEOUT;
                        breakHandler.stopPlugin(this);
                    });
            setTimeout();
        }
        else {
            Optional<TileObject> bankBooth = TileObjects.search().filter(tileObject -> {
                ObjectComposition objectComposition = TileObjectQuery.getObjectComposition(tileObject);
                return getName().toLowerCase().contains("bank") ||
                        Arrays.stream(objectComposition.getActions()).anyMatch(action -> action != null && action.toLowerCase().contains("bank"));
            }).nearestToPlayer();

            if (bankBooth.isPresent()) {

                MousePackets.queueClickPacket();
                TileObjectInteraction.interact(bankBooth.get(), "Bank");
            }
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
        }
    }
}
