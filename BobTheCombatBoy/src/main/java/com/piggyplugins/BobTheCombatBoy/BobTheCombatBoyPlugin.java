package com.piggyplugins.BobTheCombatBoy;

import com.example.EthanApiPlugin.Collections.*;
import com.example.EthanApiPlugin.Collections.query.TileObjectQuery;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.BankInventoryInteraction;
import com.example.InteractionApi.InventoryInteraction;
import com.example.InteractionApi.NPCInteraction;
import com.example.InteractionApi.TileObjectInteraction;
import com.example.Packets.*;
import com.google.inject.Provides;
import com.piggyplugins.PiggyUtils.BreakHandler.ReflectBreakHandler;
import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.game.LootManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;

import com.google.inject.Inject;
import org.apache.commons.lang3.RandomUtils;

import javax.swing.*;
import java.util.*;


@PluginDescriptor(
        name = "<html><font color=\"#FF9DF9\">[PP]</font> Bob The Combat</html>",
        description = "Bob goes Combating",
        tags = {"ethan", "piggy", "skilling"}
)
public class BobTheCombatBoyPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    BobTheCombatBoyConfig config;
    @Inject
    private KeyManager keyManager;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private BobTheCombatBoyOverlay overlay;
    @Inject
    private ReflectBreakHandler breakHandler;
    @Inject
    private ItemManager itemManager;



    State state;
    boolean started;
    private int timeout;
    private int combatTimeout;
    boolean takeBreak = false;
    public String debug = "";
    private int cannonTime = 0;


    public Queue<ItemStack> lootQueue = new ArrayDeque<>();

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
    private BobTheCombatBoyConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(BobTheCombatBoyConfig.class);
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

    @Subscribe
    public void onNpcLootReceived(NpcLootReceived event) {
        if (!started || !config.loot()) return;

        Collection<ItemStack> items = event.getItems();
        items.stream().filter(item -> {
            ItemComposition comp = itemManager.getItemComposition(item.getId());
            if (config.lootCoins() && Objects.equals(comp.getName(), "Coins"))
                return true;
            if (config.bury() && comp.getName().toLowerCase().contains("bone"))
                return true;

            return new LootItem(config.useHAValue() ? comp.getHaPrice() : comp.getPrice(),
                    config.minVal(),
                    config.minStackVal(),
                    comp.isStackable()
            ).Loot();
        }).forEach(it -> {
            lootQueue.add(it);
        });
    }

    private void handleState() {
        switch (state) {
            case ANIMATING:
                combatTimeout = 5;
                break;
            case HANDLE_BREAK:
                takeBreak = false;
                breakHandler.startBreak(this);
                timeout = 10;
                break;
            case TIMEOUT:
                timeout--;
                break;
            case EAT:
                eat();
                break;
            case BURY:
                bury();
                break;
            case PRAYER_POTION:
                prayer();
                break;
            case COMBAT_POTION:
                break;
            case LOOT:
                loot();
                break;
            case RELOAD_CANNON:
                cannon();
                break;
            case ATTACK:
                combatTimeout--;
                if (combatTimeout <= 0)
                    attack();
                break;
        }
    }

    private State getNextState() {
        //Count cannon ticks
        if (config.useCannon() && !TileObjects.search().nameContains("multicannon").withAction("Fire").result().isEmpty())
            cannonTime--;
        if (breakHandler.shouldBreak(this))
            return State.HANDLE_BREAK;
        if (client.getBoostedSkillLevel(Skill.HITPOINTS) < config.eat())
            return State.EAT;
        if (timeout > 0)
            return State.TIMEOUT;
        if (!lootQueue.isEmpty())
            return State.LOOT;
        if (config.useCannon() && cannonTime <= 0)
            return State.RELOAD_CANNON;
        if (Inventory.search().withAction("Bury").first().isPresent())
            return State.BURY;
        if (client.getBoostedSkillLevel(Skill.PRAYER) < 20)
            return State.PRAYER_POTION;
        if (EthanApiPlugin.isMoving() || client.getLocalPlayer().getAnimation() != -1)
            return State.ANIMATING;

        return State.ATTACK;
    }

    private void eat()
    {
        Inventory.search().withAction("Eat").first().ifPresent(food -> {
            MousePackets.queueClickPacket();
            InventoryInteraction.useItem(food, "Eat");
        });
    }

    private void bury()
    {
        Inventory.search().withAction("Bury").first().ifPresent(bones -> {
            MousePackets.queueClickPacket();
            InventoryInteraction.useItem(bones, "Bury");
        });
        setTimeout();
    }

    private void prayer()
    {
        Inventory.search().nameContains("Prayer potion").first().ifPresent(potion -> {
            MousePackets.queueClickPacket();
            InventoryInteraction.useItem(potion, "Drink");
        });
        setTimeout();
    }

    private void cannon()
    {
        if (Inventory.search().nameContains("Cannonball").first().isPresent())
        {
            TileObjects.search().nameContains("multicannon").withAction("Fire").nearestToPlayer().ifPresent(can -> {
                MousePackets.queueClickPacket();
                TileObjectInteraction.interact(can, "Fire");
            });
        }
        cannonTime = randNext(config.cannonTime(), 5, 5);
        setTimeout();
    }

    private void attack()
    {
        NPCs.search().nameContains(config.target()).nearestToPlayer().ifPresent(enemy -> {
            if (enemy.getWorldLocation().distanceTo(client.getLocalPlayer().getWorldLocation()) > 10)
                return;
            MousePackets.queueClickPacket();
            NPCInteraction.interact(enemy, "Attack");
        });
        setTimeout();
    }
    private void loot()
    {
        boolean loot = false;
        ItemStack itemStack = lootQueue.poll();
        ItemComposition composition = itemManager.getItemComposition(itemStack.getId());
        if (Inventory.full())
        {
            if (composition.isStackable() &&
                    Inventory.search().withId(itemStack.getId()).first().isPresent())
                loot = true;
        }
        else
            loot = true;

        if (!loot)
            return;

        TileItems.search().withId(itemStack.getId()).withinDistance(10).first().ifPresent(item -> {
            MousePackets.queueClickPacket();
            item.interact(false);
        });
        setTimeout();
    }

    private void setTimeout() {
        timeout = RandomUtils.nextInt(config.tickdelayMin(), config.tickDelayMax());
    }
    private int randNext(int base, int down, int up)
    {
        return RandomUtils.nextInt(base - down, base + up);
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
