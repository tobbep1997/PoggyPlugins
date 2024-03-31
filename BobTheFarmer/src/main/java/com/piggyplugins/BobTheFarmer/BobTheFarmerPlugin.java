package com.piggyplugins.BobTheFarmer;

import com.example.EthanApiPlugin.Collections.*;
import com.example.EthanApiPlugin.Collections.query.TileObjectQuery;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.*;
import com.example.PacketUtils.WidgetInfoExtended;
import com.example.Packets.*;
import com.google.inject.Provides;
import com.piggyplugins.PiggyUtils.API.BankUtil;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
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

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;




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
    State state;
    WorldPoint debugPoint = new WorldPoint(0,0,0);
    boolean started;
    boolean herbRun;
    boolean treeRun;
    private int timeout;
    public String debug = "";
    private boolean hasStockedHerb = false;
    private boolean hasStockedTree = false;
    private final String[] Tools =  {"Magic secateurs", "Spade", "Rake", "Seed dibber" };

    //Herb patches
    HerbPatch HerbPatchStateDisplay = null;
    private HerbPatch ArdougneHerbPatch = null;
    private HerbPatch CatherbyHerbPatch = null;
    private HerbPatch CivitasIllaFortisHerbPatch = null;
    private HerbPatch FaladorHerbPatch = null;
    private HerbPatch FarmingGuildHerbPatch = null;
    private HerbPatch HarmonyIslandHerbPatch = null;
    private HerbPatch HosidiusHerbPatch = null;
    private HerbPatch PortPhasmatysHerbPatch = null;
    private HerbPatch TrollStrongholdHerbPatch = null;
    private HerbPatch WeissHerbPatch = null;

    //Tree patches
    TreePatch TreePatchStateDisplay = null;
    private TreePatch FaladorTreePatch = null;



    @Override
    protected void startUp() throws Exception {
        keyManager.registerKeyListener(hotkeyListenerToggle);
        keyManager.registerKeyListener(hotkeyListenerHerbRun);
        keyManager.registerKeyListener(hotkeyListenerTreeRun);
        keyManager.registerKeyListener(hotkeyListenerDebug);
        this.overlayManager.add(overlay);
    }

    @Override
    protected void shutDown() throws Exception {
        keyManager.unregisterKeyListener(hotkeyListenerToggle);
        keyManager.unregisterKeyListener(hotkeyListenerHerbRun);
        keyManager.unregisterKeyListener(hotkeyListenerTreeRun);
        keyManager.unregisterKeyListener(hotkeyListenerDebug);
        this.overlayManager.remove(overlay);
    }

    @Provides
    private BobTheFarmerConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(BobTheFarmerConfig.class);
    }

    @Subscribe
    private void onChatMessage(ChatMessage message)
    {
        if (message.getType() == ChatMessageType.GAMEMESSAGE)
        {
            if (message.getMessage().contains("Try again tomorrow when the cape"))
            {
                ArdougneHerbPatch.PathIndex = 2;
            }
            if (message.getMessage().contains("Try again tomorrow whilst the ring"))
            {
                FaladorHerbPatch.PathIndex = 10;
            }
        }
    }
    @Subscribe
    private void onGameTick(GameTick event) {
        if (!EthanApiPlugin.loggedIn() || !started) {
            // We do an early return if the user isn't logged in
            hasStockedHerb = false;
            herbRun = false;
            treeRun = false;
            return;
        }

        if (!herbRun)
            ResetHerbPatchStates();

        if (!treeRun)
            ResetTreePatchStates();

        state = getNextState();
        handleState();
    }

    private void ResetHerbPatchStates()
    {
        ArdougneHerbPatch = new HerbPatch("Ardougne", new String[] {});
        ArdougneHerbPatch.SetPath(Paths.ArdougneHerbTeleportPath1, "Teleport");

        CatherbyHerbPatch = new HerbPatch("Catherby", new String[] {});
        CatherbyHerbPatch.SetPath(Paths.CatherbyHerbTeleportPath1, "Camelot");

        CivitasIllaFortisHerbPatch = new HerbPatch("Civitas", new String[] {});

        FaladorHerbPatch = new HerbPatch("Falador", new String[] {});
        FaladorHerbPatch.SetPath(Paths.FaladorHerbTeleportPath1, "Teleport1");
        FaladorHerbPatch.SetPath(Paths.FaladorHerbTeleportPath2, "Teleport2");

        FarmingGuildHerbPatch = new HerbPatch("Farming guild", new String[] {});

        HarmonyIslandHerbPatch = new HerbPatch("Harmony", new String[] {});

        HosidiusHerbPatch = new HerbPatch("Hosidius", new String[] {});

        PortPhasmatysHerbPatch = new HerbPatch("Port Phasmatys", new String[] {});

        TrollStrongholdHerbPatch = new HerbPatch("Troll Stronghold", new String[] {});

        WeissHerbPatch = new HerbPatch("Weiss", new String[] {});
    }

    private void ResetTreePatchStates()
    {
        FaladorTreePatch = new TreePatch("Falador", "Heskel", new String[] {});

    }

    private void handleState() {
        if (state == null)
            return;

        switch (state) {
            case ANIMATING:
                break;
            case TIMEOUT:
                timeout--;
                break;

            //HERBS
            case RESTOCK_HERB:
                Restock();
                break;
            case HERB_TRAVEL_ARDOUGNE:
                TravelToArdougne(ArdougneHerbPatch);
                break;
            case HERB_ARDOUGNE:
                FarmHerbs(ArdougneHerbPatch);
                break;
            case HERB_TRAVEL_CATHERBY:
                TravelToCatherby(CatherbyHerbPatch);
                break;
            case HERB_CATHERBY:
                FarmHerbs(CatherbyHerbPatch);
                break;
            case HERB_TRAVEL_FALADOR:
                TravelToFalador(FaladorHerbPatch);
                break;
            case HERB_FALADOR:
                FarmHerbs(FaladorHerbPatch);
                break;

            //TREE
            case RESTOCK_TREE:
                break;
            case TREE_TRAVEL_FALADOR:
                break;
            case TREE_FALADOR:
                break;
        }
    }

    private State getNextState() {
        if (EthanApiPlugin.isMoving() || client.getLocalPlayer().getAnimation() != -1) {
            return State.ANIMATING;
        }
        if (timeout > 0) {
            return State.TIMEOUT;
        }
        if (!herbRun && !treeRun)
            return null;

        if (herbRun)
        {
            if (!hasStockedHerb && !config.debugDisableRestock())
                return State.RESTOCK_HERB;

            if (config.enableArdougne() && ArdougneHerbPatch.State.Index < 2)
                return State.HERB_TRAVEL_ARDOUGNE;
            if (config.enableArdougne() && ArdougneHerbPatch.State.Index >= 2 &&
                    ArdougneHerbPatch.State != HerbPatchState.DONE)
                return State.HERB_ARDOUGNE;

            if (config.enableCatherby() && CatherbyHerbPatch.State.Index < 2)
                return State.HERB_TRAVEL_CATHERBY;
            if (config.enableCatherby() && CatherbyHerbPatch.State.Index >= 2 &&
                    CatherbyHerbPatch.State != HerbPatchState.DONE)
                return State.HERB_CATHERBY;

            if (config.enableFalador() && FaladorHerbPatch.State.Index < 2)
                return State.HERB_TRAVEL_FALADOR;
            if (config.enableFalador() && FaladorHerbPatch.State.Index >= 2 &&
                    FaladorHerbPatch.State != HerbPatchState.DONE)
                return State.HERB_FALADOR;
        }

        if (treeRun)
        {
            if (!hasStockedTree && !config.debugDisableRestock())
                return State.RESTOCK_TREE;
            if (config.enableTreeFalador() && FaladorTreePatch.State.Index < 2)
                return State.TREE_TRAVEL_FALADOR;
            if (config.enableTreeFalador() && FaladorTreePatch.State.Index >= 2 &&
                    FaladorTreePatch.State != TreePatchState.DONE)
                return State.TREE_FALADOR;

        }


        Stop("Done");
        return null;
    }


    private HerbPatchState FarmHerbsState(HerbPatch currentState)
    {
        //Check if there is any weeds in the inventory and drop them if there is
        if (Inventory.search().withName("Weeds").first().isPresent())
            return HerbPatchState.EMPTY_INVENTORY;

        //Check if the patch is not fully grown then mark the patch as done
        if (TileObjects.search().withName("Herbs").first().isPresent() && currentState.State != HerbPatchState.PLANTING)
            if (!Arrays.asList(TileObjectQuery.getObjectComposition(TileObjects.search().withName("Herbs").first().get()).getActions()).contains("Pick"))
                return HerbPatchState.NOTE;

        //Harvest herbs if there is any
        if (TileObjects.search().withName("Herbs").withAction("Pick").first().isPresent())
            return HerbPatchState.HARVEST;

        //Clear dead herbs
        if (TileObjects.search().withName("Dead herbs").withAction("Clear").first().isPresent())
            return HerbPatchState.CLEAR;

        //Plant new herbs
        if (TileObjects.search().nameContains("Herb patch").withAction("Rake").first().isPresent()){
            return HerbPatchState.RAKE;
        }

        //Check if herb patch is ready for planting
        if (TileObjects.search().nameContains("Herb patch").withAction("Inspect").first().isPresent()) {
            if (Inventory.search().withName(config.herb().SeedName).first().isPresent())
                //Use seed on herb patch
                return HerbPatchState.PLANTING;
        }

        //Use compost on the herbs
        if (TileObjects.search().nameContains("Herbs").withAction("Inspect").first().isPresent()) {
            if (Inventory.search().withName(config.compost()).first().isPresent())
                return HerbPatchState.COMPOST;
        }

        return HerbPatchState.PROCESS_HERB_PATCH;
    }

    private void FarmHerbs(HerbPatch state)
    {
        if (state.State.Index < 2 || state.State == HerbPatchState.DONE)
            return;

        state.State = FarmHerbsState(state);
        SetDisplayState(state);

        switch (state.State)
        {
            case HARVEST: //Harvest herbs if there is any
                TileObjects.search().withName("Herbs").withAction("Pick").first().ifPresent(herb -> {
                    MousePackets.queueClickPacket();
                    TileObjectInteraction.interact(herb, "Pick");
                });
                break;
            case CLEAR:
                //Plant new herbs
                TileObjects.search().nameContains("Dead herbs").withAction("Clear").first().ifPresent(tileObject -> {
                    MousePackets.queueClickPacket();
                    TileObjectInteraction.interact(tileObject, "Clear");
                });
                break;
            case RAKE:
                //Plant new herbs
                TileObjects.search().nameContains("Herb patch").withAction("Rake").first().ifPresent(tileObject -> {
                    MousePackets.queueClickPacket();
                    TileObjectInteraction.interact(tileObject, "Rake");
                });
                break;
            case PLANTING:
                TileObjects.search().nameContains("Herb patch").withAction("Inspect").first().ifPresent(tileObject -> {
                    Inventory.search().withName(config.herb().SeedName).first().ifPresent(item -> {
                        //Use seed on herb patch
                        MousePackets.queueClickPacket();
                        MousePackets.queueClickPacket();
                        ObjectPackets.queueWidgetOnTileObject(item, tileObject);
                    });
                });
                break;
            case COMPOST:
                //Use compost on the herbs
                TileObjects.search().nameContains("Herbs").withAction("Inspect").first().ifPresent(tileObject -> {
                    state.State = HerbPatchState.COMPOST;
                    Inventory.search().withName(config.compost()).first().ifPresent(item -> {
                        //Use compost on herb patch
                        MousePackets.queueClickPacket();
                        MousePackets.queueClickPacket();
                        ObjectPackets.queueWidgetOnTileObject(item, tileObject);
                        state.State = HerbPatchState.NOTE;
                    });
                });
                break;
            case EMPTY_INVENTORY: //Check if there is any weeds in the inventory and drop them if there is
                Inventory.search().withName("Weeds").first().ifPresent(weeds -> {
                    MousePackets.queueClickPacket();
                    InventoryInteraction.useItem(weeds, "Drop");
                });
                break;
            case NOTE:
                Inventory.search().nameContains("Grimy").withAction("Clean").onlyUnnoted().first().ifPresent(herbs -> {
                    debug = "found herb";

                    NPCs.search().nameContains("Tool").nearestToPlayer().ifPresent(leprechaun -> {
                        debug = "found leprechaun";
                        MousePackets.queueClickPacket();
                        NPCPackets.queueWidgetOnNPC(leprechaun, herbs);
                    });
                });
                state.State = HerbPatchState.DONE;
                break;
        }
        if (state.State != HerbPatchState.PROCESS_HERB_PATCH && state.State != HerbPatchState.EMPTY_INVENTORY)
            setTimeout();
    }

    private void Restock()
    {
        if (Bank.isOpen()) {
            ArrayList<String> keepItems = new ArrayList<String>(Arrays.asList(Tools));

            if (config.enableArdougne())
                keepItems.addAll(Arrays.asList(ArdougneHerbPatch.Tools));
            if (config.enableCatherby())
                keepItems.addAll(Arrays.asList(CatherbyHerbPatch.Tools));
            if (config.enableCivitasIllaFortis())
                keepItems.addAll(Arrays.asList(CivitasIllaFortisHerbPatch.Tools));
            if (config.enableFalador())
                keepItems.addAll(Arrays.asList(FaladorHerbPatch.Tools));
            if (config.enableFarmingGuild())
                keepItems.addAll(Arrays.asList(FarmingGuildHerbPatch.Tools));
            if (config.enableHarmonyIsland())
                keepItems.addAll(Arrays.asList(HarmonyIslandHerbPatch.Tools));
            if (config.enableHosidius())
                keepItems.addAll(Arrays.asList(HosidiusHerbPatch.Tools));
            if (config.enablePortPhasmatys())
                keepItems.addAll(Arrays.asList(PortPhasmatysHerbPatch.Tools));
            if (config.enableTrollStronghold())
                keepItems.addAll(Arrays.asList(TrollStrongholdHerbPatch.Tools));
            if (config.enableWeiss())
                keepItems.addAll(Arrays.asList(WeissHerbPatch.Tools));

            int patches = 0;
            patches += config.enableArdougne() ? 1 : 0;
            patches += config.enableCatherby() ? 1 : 0;
            patches += config.enableCivitasIllaFortis() ? 1 : 0;
            patches += config.enableFalador() ? 1 : 0;
            patches += config.enableFarmingGuild() ? 1 : 0;
            patches += config.enableHarmonyIsland() ? 1 : 0;
            patches += config.enableHosidius() ? 1 : 0;
            patches += config.enablePortPhasmatys() ? 1 : 0;
            patches += config.enableTrollStronghold() ? 1 : 0;
            patches += config.enableWeiss() ? 1 : 0;

            //Deposit items that are not needed
            List<Widget> bankInv = BankInventory.search().filter(widget -> keepItems.contains(widget.getName())).result();
            for (Widget item : bankInv) {
                MousePackets.queueClickPacket();
                BankInventoryInteraction.useItem(item, "Deposit-All");
            }

            //Take out basic tools
            for (String tool : Tools) {
                if (!TakeOutItemFromBank(tool, 1)) {
                    Stop("Missing " + tool + " in bank");
                    return;
                }
            }

            //Take out seeds
            if (!TakeOutItemFromBank(config.herb().SeedName, patches)) {
                Stop("Missing " + config.herb() + " in bank");
                return;
            }

            //Take out compost
            if (!TakeOutItemFromBank(config.compost(), patches)) {
                Stop("Missing " + config.herb() + " in bank");
                return;
            }

            //Take out additional items
            String[] additionalItems = config.additionalItems().split(",");
            for (String tool : additionalItems) {
                if (!TakeOutItemFromBank(tool, 1)) {
                    Stop("Missing " + tool + " in bank");
                    return;
                }
            }

            //Take out tools that are needed for the Ardougne herb patch
            if (config.enableArdougne())
            {
                for (String tool : ArdougneHerbPatch.Tools)
                {
                    if (!TakeOutItemFromBank(tool, 1)) {
                        Stop("Missing " + tool + " in bank");
                        return;
                    }
                }

                for (int i = 4; i >= 2; i--) {
                    if (TakeOutItemFromBank("Ardougne cloak " + i, 1)) {
                        break;
                    }
                }
            }

            //Take out tools that are needed for the Catherby herb patch
            if (config.enableCatherby())
            {
                for (String tool : CatherbyHerbPatch.Tools)
                {
                    if (!TakeOutItemFromBank(tool, 1)) {
                        Stop("Missing " + tool + " in bank");
                        return;
                    }
                }
            }

            //Take out tools that are needed for the Falador herb patch
            if (config.enableFalador())
            {
                for (String tool : FaladorHerbPatch.Tools)
                {
                    if (!TakeOutItemFromBank(tool, 1)) {
                        Stop("Missing " + tool + " in bank");
                        return;
                    }
                }

                for (int i = 4; i >= 2; i--) {
                    if (TakeOutItemFromBank("Explorer's ring " + i, 1)) {
                        break;
                    }
                }
            }

            //Make sure we have all items and mark it as clear
            hasStockedHerb = true;
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

            TileObjects.search().withName("Bank chest").nearestToPlayer().ifPresent(tileObject -> {
                MousePackets.queueClickPacket();
                TileObjectInteraction.interact(tileObject, "Use");
            });
        }
    }

    private boolean TakeOutItemFromBank(String item, int amount)
    {
        AtomicBoolean succeeded = new AtomicBoolean(false);
        BankUtil.nameContainsNoCase(item).first().ifPresentOrElse(widget ->
            {
                int localAmount = amount - Inventory.search().withName(item).result().size();

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

    private void TravelToArdougne(HerbPatch state)
    {
        ArdougneHerbPatch.State = HerbPatchState.TRAVEL;
        SetDisplayState(state);

        if (ArdougneHerbPatch.PathIndex == 0)
        {
            for (int i = 4; i >= 2; i--) {
                if (Inventory.search().withName("Ardougne cloak " + i).first().isPresent())
                {
                    MousePackets.queueClickPacket();
                    InventoryInteraction.useItem(
                            Inventory.search().withName("Ardougne cloak " + i).first().get(),
                            "Farm Teleport");
                }
            }
            ArdougneHerbPatch.PathIndex = 1;
            timeout = 4;
            return;
        }
        if (ArdougneHerbPatch.PathIndex == 1)
        {
            ArdougneHerbPatch.State = HerbPatchState.PROCESS_HERB_PATCH;
            return;
        }
        if (ArdougneHerbPatch.PathIndex == 2)
        {
            if (CastTeleportSpell(WidgetInfoExtended.SPELL_ARDOUGNE_TELEPORT))
            {
                ResetPath();
                ArdougneHerbPatch.PathIndex = 3;
                setTimeout();
            }
            else
            {
                Stop("Couldn't teleport to Ardougne");
            }
            return;
        }
        if (ArdougneHerbPatch.PathIndex == 3)
        {
            if (TravelPath(ArdougneHerbPatch.Paths.get("Teleport")))
            {
                ArdougneHerbPatch.State = HerbPatchState.PROCESS_HERB_PATCH;
            }
        }
    }

    private void TravelToFalador(HerbPatch state)
    {
        FaladorHerbPatch.State = HerbPatchState.TRAVEL;
        SetDisplayState(state);

        if (FaladorHerbPatch.PathIndex == 0)
        {
            for (int i = 4; i >= 2; i--) {
                if (Inventory.search().withName("Explorer's ring " + i).first().isPresent())
                {
                    MousePackets.queueClickPacket();
                    InventoryInteraction.useItem(
                            Inventory.search().withName("Explorer's ring " + i).first().get(),
                            "Teleport");
                }
            }
            FaladorHerbPatch.PathIndex = 1;
            timeout = 8;
            return;
        }
        if (FaladorHerbPatch.PathIndex == 1)
        {
            FaladorHerbPatch.State = HerbPatchState.PROCESS_HERB_PATCH;
            return;
        }
        if (FaladorHerbPatch.PathIndex == 10)
        {
            if (CastTeleportSpell(WidgetInfoExtended.SPELL_FALADOR_TELEPORT))
            {
                ResetPath();
                FaladorHerbPatch.PathIndex = 11;
                setTimeout();
            }
            else
            {
                Stop("Couldn't teleport to Falador");
            }
            return;
        }
        if (FaladorHerbPatch.PathIndex == 11)
        {
            if (TravelPath(FaladorHerbPatch.Paths.get("Teleport1")))
            {
                FaladorHerbPatch.PathIndex = 12;
                setTimeout();
            }
            return;
        }
        if (FaladorHerbPatch.PathIndex == 12)
        {
            TileObjects.search()
                    .nameContains("Gate")
                    .withAction("Open")
                    .withinDistance(6)
                    .nearestToPlayer()
                    .ifPresent(gate -> {

                        MousePackets.queueClickPacket();
                        TileObjectInteraction.interact(gate, "Open");
            });
            ResetPath();
            FaladorHerbPatch.PathIndex = 13;
            return;
        }
        if (FaladorHerbPatch.PathIndex == 13)
        {
            if (TravelPath(FaladorHerbPatch.Paths.get("Teleport2")))
            {
                FaladorHerbPatch.PathIndex = 14;
                setTimeout();
            }
            return;
        }
        if (FaladorHerbPatch.PathIndex == 14)
        {
            TileObjects.search()
                    .nameContains("Stile")
                    .withAction("Climb-over")
                    .withinDistance(6)
                    .nearestToPlayer()
                    .ifPresent(gate -> {

                        MousePackets.queueClickPacket();
                        TileObjectInteraction.interact(gate, "Climb-over");
                    });
            timeout = 4;
            FaladorHerbPatch.State = HerbPatchState.PROCESS_HERB_PATCH;
        }

    }

    private void TravelToCatherby(HerbPatch state)
    {
        CatherbyHerbPatch.State = HerbPatchState.TRAVEL;
        SetDisplayState(state);

        if (CatherbyHerbPatch.PathIndex == 0)
        {
            if (CastTeleportSpell(WidgetInfoExtended.SPELL_CAMELOT_TELEPORT))
            {
                ResetPath();
                CatherbyHerbPatch.PathIndex = 1;
                setTimeout();
            }
            else
            {
                Stop("Couldn't teleport to Camelot");
            }
        }
        if (CatherbyHerbPatch.PathIndex == 1)
        {
            if (TravelPath(CatherbyHerbPatch.Paths.get("Camelot")))
            {
                CatherbyHerbPatch.PathIndex = 2;
                setTimeout();
            }
            return;
        }
        if (CatherbyHerbPatch.PathIndex == 2)
        {
            CatherbyHerbPatch.State = HerbPatchState.PROCESS_HERB_PATCH;
        }
    }

    private int pathIndex = 0;
    private void ResetPath()
    {
        pathIndex = 0;
    }
    private boolean TravelPath(WorldPoint[] path)
    {
        if (EthanApiPlugin.isMoving())
            return false;

        if (playerAtTarget(path[pathIndex]))
            pathIndex++;

        if (pathIndex < path.length)
        {
            //Move the player to the trap location
            MousePackets.queueClickPacket();
            MovementPackets.queueMovement(path[pathIndex]);
        }
        else
            return true;
        return false;
    }

    private boolean playerAtTarget(WorldPoint targetTile)
    {
        return client.getLocalPlayer().getWorldLocation().getX() == targetTile.getX() && client.getLocalPlayer().getWorldLocation().getY() == targetTile.getY();
    }

    private boolean CastTeleportSpell(WidgetInfoExtended spell)
    {
        Optional<Widget> teleportSpellIcon = Widgets.search().withId(spell.getPackedId()).first();
        if (teleportSpellIcon.isPresent()) {
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetAction(teleportSpellIcon.get(), "Cast");
            return true;
        }
        return false;
    }

    private void SetDisplayState(HerbPatch state)
    {
        HerbPatchStateDisplay = state;
    }

    private void setTimeout() {
        timeout = RandomUtils.nextInt(config.tickDelayMin(), config.tickDelayMax());
    }

    public void Stop(String reason)
    {
        started = false;
        debug = reason;
        this.state = State.TIMEOUT;
        herbRun = false;
    }

    private final HotkeyListener hotkeyListenerToggle = new HotkeyListener(() -> config.toggle()) {
        @Override
        public void hotkeyPressed() {
            toggle();
        }
    };

    private final HotkeyListener hotkeyListenerHerbRun = new HotkeyListener(() -> config.doHerbRun()) {
        @Override
        public void hotkeyPressed() {
            herbRun();
        }
    };

    private final HotkeyListener hotkeyListenerTreeRun = new HotkeyListener(() -> config.doTreeRun()) {
        @Override
        public void hotkeyPressed() {
            treeRun();
        }
    };
    private final HotkeyListener hotkeyListenerDebug = new HotkeyListener(() -> config.debugKey()) {
        @Override
        public void hotkeyPressed() {
            Debug();
        }
    };

    public void Debug()
    {
        debugPoint = client.getLocalPlayer().getWorldLocation();
    }

    public void herbRun()
    {
        if (client.getGameState() != GameState.LOGGED_IN) {
            return;
        }
        herbRun = true;
    }

    public void treeRun()
    {
        if (client.getGameState() != GameState.LOGGED_IN) {
            return;
        }
        treeRun = true;
    }

    public void toggle() {
        if (client.getGameState() != GameState.LOGGED_IN) {
            return;
        }
        started = !started;

        debug = "";

        if (!started) {
            this.state = State.OFF;
        }
    }
}
