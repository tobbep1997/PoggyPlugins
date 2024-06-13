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
import com.piggyplugins.PiggyUtils.API.PrayerUtil;
import com.piggyplugins.PiggyUtils.BreakHandler.ReflectBreakHandler;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.VarbitChanged;
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
import java.util.stream.Stream;

import static net.runelite.api.TileItem.OWNERSHIP_GROUP;
import static net.runelite.api.TileItem.OWNERSHIP_SELF;


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
    private boolean slayerTaskDone = false;
    private int slayerCountDown = 5;
    public Queue<TileItem> lootQueue = new ArrayDeque<>();
    WorldPoint safeSpot = null;
    NPC currentTarget = null;


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
        debug = "" + client.getLocalPlayer().getAnimation();

        if (!EthanApiPlugin.loggedIn() || !started || breakHandler.isBreakActive(this)) {
            // We do an early return if the user isn't logged in
            slayerTaskDone = false;
            slayerCountDown = 5;
            safeSpot = null;
            currentTarget = null;
            lootQueue.clear();
            return;
        }
        if (safeSpot == null)
            safeSpot = client.getLocalPlayer().getWorldLocation();

        state = getNextState();
        handleState();
    }

    @Subscribe
    public void onItemSpawned(ItemSpawned itemSpawned) {
        if (!started || !config.loot()) return;

        boolean add = false;
        TileItem item = itemSpawned.getItem();
        ItemComposition comp = itemManager.getItemComposition(item.getId());

        if (item.getOwnership() != OWNERSHIP_SELF &&
                item.getOwnership() != OWNERSHIP_GROUP)
            return;
        if (itemSpawned.getTile().getWorldLocation().distanceTo(client.getLocalPlayer().getWorldLocation()) >= 10)
            return;

        if (config.lootCoins() && Objects.equals(comp.getName(), "Coins"))
            add = true;
        if (config.bury() && (comp.getName().toLowerCase().contains("bone") || comp.getName().toLowerCase().contains("ashes")))
            add = true;

        if (new LootItem(config.useHAValue() ? comp.getHaPrice() : itemManager.getItemPrice(comp.getId()),
                config.minVal(),
                config.minStackVal(),
                comp.isStackable()
        ).Loot())
            add = true;

        if (add)
            lootQueue.add(item);
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event) {
        if (!started || !config.slayerStop()) return;
        int bid = event.getVarbitId();
        int pid = event.getVarpId();
        if (pid == VarPlayer.SLAYER_TASK_SIZE) {
            if (event.getValue() <= 0) {
                slayerTaskDone = true;
            }
        }
    }

    private void handleState() {
        switch (state) {
            case ANIMATING:
                combatTimeout = config.attackTimeout();
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
            case SLAYER_DONE:
                slayerCountDown--;
                if (slayerCountDown <= 0)
                    slayerDone();
                break;
            case GTFO:
                teleportOut();
                break;
            case MOVE_TO_SAFE_SPOT:
                moveToSafeSpot();
                break;
        }
    }

    private State getNextState() {
        //Count cannon ticks
        if (config.useCannon() && !TileObjects.search().nameContains("multicannon").withAction("Fire").result().isEmpty())
            cannonTime--;

        if (breakHandler.shouldBreak(this))
            return State.HANDLE_BREAK;

        if (client.getBoostedSkillLevel(Skill.HITPOINTS) < config.eat() &&
                Inventory.search().withAction("Eat").first().isPresent())
            return State.EAT;

        if (client.getBoostedSkillLevel(Skill.PRAYER) < config.prayer() &&
                Inventory.search().nameContains("Prayer potion").first().isPresent())
            return State.PRAYER_POTION;

        if (client.getBoostedSkillLevel(Skill.HITPOINTS) < config.lowHealth() ||
                client.getBoostedSkillLevel(Skill.PRAYER) < config.lowPrayer())
            return State.GTFO;

        if (config.useCannon() && cannonTime <= 0)
            return State.RELOAD_CANNON;

        if (EthanApiPlugin.isMoving())
            return State.MOVING;

        if (!lootQueue.isEmpty())
            return State.LOOT;

        if (config.safeSpot() && client.getLocalPlayer().getWorldLocation().distanceTo(safeSpot) > 0)
            return State.MOVE_TO_SAFE_SPOT;

        if (timeout > 0)
            return State.TIMEOUT;

        if (slayerTaskDone && lootQueue.isEmpty())
            return State.SLAYER_DONE;

        if (client.getLocalPlayer().getAnimation() != -1)
            return State.ANIMATING;

        if (Inventory.search().withAction("Bury").first().isPresent() ||
                Inventory.search().withAction("Scatter").first().isPresent())
            return State.BURY;

        return State.ATTACK;
    }

    private void eat() {
        Inventory.search().withAction("Eat").first().ifPresent(food -> {
            MousePackets.queueClickPacket();
            InventoryInteraction.useItem(food, "Eat");
        });
    }

    private void moveToSafeSpot() {
        //Move the player to the trap location
        MousePackets.queueClickPacket();
        MovementPackets.queueMovement(safeSpot);
    }

    private void bury() {
        Inventory.search().withAction("Bury").first().ifPresent(bones -> {
            MousePackets.queueClickPacket();
            InventoryInteraction.useItem(bones, "Bury");
        });
        Inventory.search().withAction("Scatter").first().ifPresent(bones -> {
            MousePackets.queueClickPacket();
            InventoryInteraction.useItem(bones, "Scatter");
        });
        setTimeout();
    }

    private void prayer() {
        Inventory.search().nameContains("Prayer potion").first().ifPresent(potion -> {
            MousePackets.queueClickPacket();
            InventoryInteraction.useItem(potion, "Drink");
        });
        setTimeout();
    }

    private void cannon() {
        if (Inventory.search().nameContains("Cannonball").first().isPresent()) {
            TileObjects.search().nameContains("multicannon").withAction("Fire").nearestToPlayer().ifPresent(can -> {
                MousePackets.queueClickPacket();
                TileObjectInteraction.interact(can, "Fire");
            });
        }
        cannonTime = randNext(config.cannonTime(), 5, 5);
        setTimeout();
    }

    private void attack() {
        if (currentTarget == null) {
            NPCs.search().nameContains(config.target()).nearestToPlayer().ifPresent(enemy -> {
                if (enemy.getWorldLocation().distanceTo(client.getLocalPlayer().getWorldLocation()) > 10) {
                    return;
                }
                currentTarget = enemy;
            });
        } else {
            try {
                if (!currentTarget.getComposition().isInteractible()) {
                    currentTarget = null;
                    return;
                }
            } catch (Exception e) {
                currentTarget = null;
                return;
            }


            if (config.safeSpot()) {
                if (currentTarget.getWorldLocation().distanceTo(safeSpot) > 10) {
                    currentTarget = null;
                    return;
                }
            }

            MousePackets.queueClickPacket();
            NPCInteraction.interact(currentTarget, "Attack");
        }

        setTimeout();
    }

    private void loot() {
        if (Inventory.full() && config.dropItems()) {
            Optional<Widget> item = Inventory.search().withName("Vial").first();

            if (item.isPresent()) {
                MousePackets.queueClickPacket();
                InventoryInteraction.useItem(item.get(), "Drop");
            }
        }

        boolean loot = false;
        TileItem tileItem = lootQueue.poll();
        ItemComposition composition = itemManager.getItemComposition(tileItem.getId());
        if (Inventory.full()) {
            if (composition.isStackable() &&
                    Inventory.search().withId(tileItem.getId()).first().isPresent())
                loot = true;
        } else
            loot = true;

        if (!loot)
            return;

        TileItems.search().withId(tileItem.getId()).withinDistance(10).nearestToPlayer().ifPresent(item -> {
            MousePackets.queueClickPacket();
            item.interact(false);
        });
        timeout = 1;
    }

    private void slayerDone() {
        if (config.slayerBreak()) {
            Inventory.search().withAction("Break").first().ifPresent(tab -> {
                MousePackets.queueClickPacket();
                InventoryInteraction.useItem(tab, "Break");
            });
        }
        if (config.slayerStop())
            started = false;
    }

    private void teleportOut() {
        Inventory.search().withAction("Break").first().ifPresent(tab -> {
            MousePackets.queueClickPacket();
            InventoryInteraction.useItem(tab, "Break");
        });
        started = false;

    }

    private void setTimeout() {
        timeout = RandomUtils.nextInt(config.tickdelayMin(), config.tickDelayMax());
    }

    private int randNext(int base, int down, int up) {
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
