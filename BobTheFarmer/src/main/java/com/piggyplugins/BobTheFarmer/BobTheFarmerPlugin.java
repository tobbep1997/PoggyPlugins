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


class FarmingState
{
    public String Name = "";
    public String[] Tools = {};
    public ProcessState HerbPatchState = ProcessState.NOT_STARTED;
    public Dictionary<String, WorldPoint[]> Paths = new Hashtable<>();
    public int PathIndex = 0;

    public FarmingState(String name,String[] tools)
    {
        this.Name = name;
        this.Tools = tools;
        this.PathIndex = 0;
    }
    public void SetPath(WorldPoint[] path, String key)
    {
        Paths.put(key, path);
    }
}

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
    private boolean hasStocked = false;
    private final String[] Tools =  {"Magic secateurs", "Spade", "Rake", "Seed dibber" };
    FarmingState FarmingStateDisplay = null;
    private FarmingState ArdougneFarmingState = null;
    private FarmingState CatherbyFarmingState = null;
    private FarmingState CivitasIllaFortisFarmingState = null;
    private FarmingState FaladorFarmingState = null;
    private FarmingState FarmingGuildFarmingState = null;
    private FarmingState HarmonyIslandFarmingState = null;
    private FarmingState HosidiusFarmingState = null;
    private FarmingState PortPhasmatysFarmingState = null;
    private FarmingState TrollStrongholdFarmingState = null;
    private FarmingState WeissFarmingState = null;




    @Override
    protected void startUp() throws Exception {
        keyManager.registerKeyListener(hotkeyListenerToggle);
        keyManager.registerKeyListener(hotkeyListenerHerbRun);
        keyManager.registerKeyListener(hotkeyListenerDebug);
        this.overlayManager.add(overlay);
    }

    @Override
    protected void shutDown() throws Exception {
        keyManager.unregisterKeyListener(hotkeyListenerToggle);
        keyManager.unregisterKeyListener(hotkeyListenerHerbRun);
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
                ArdougneFarmingState.PathIndex = 2;
            }
            if (message.getMessage().contains("Try again tomorrow whilst the ring"))
            {
                FaladorFarmingState.PathIndex = 10;
            }
        }
    }
    @Subscribe
    private void onGameTick(GameTick event) {
        if (!EthanApiPlugin.loggedIn() || (!started || !(herbRun || treeRun))) {
            // We do an early return if the user isn't logged in
            hasStocked = false;
            herbRun = false;
            ResetFarmingStates();
            return;
        }

        state = getNextState();
        handleState();
    }

    private void ResetFarmingStates()
    {
        ArdougneFarmingState = new FarmingState("Ardougne", new String[] {});
        ArdougneFarmingState.SetPath(Paths.ArdougneTeleportPath1, "Teleport");

        CatherbyFarmingState = new FarmingState("Catherby", new String[] {});
        CatherbyFarmingState.SetPath(Paths.CatherbyTeleportPath1, "Camelot");

        CivitasIllaFortisFarmingState = new FarmingState("Civitas", new String[] {});

        FaladorFarmingState = new FarmingState("Falador", new String[] {});
        FaladorFarmingState.SetPath(Paths.FaladorTeleportPath1, "Teleport1");
        FaladorFarmingState.SetPath(Paths.FaladorTeleportPath2, "Teleport2");

        FarmingGuildFarmingState = new FarmingState("Farming guild", new String[] {});

        HarmonyIslandFarmingState = new FarmingState("Harmony", new String[] {});

        HosidiusFarmingState = new FarmingState("Hosidius", new String[] {});

        PortPhasmatysFarmingState = new FarmingState("Port Phasmatys", new String[] {});

        TrollStrongholdFarmingState = new FarmingState("Troll Stronghold", new String[] {});

        WeissFarmingState = new FarmingState("Weiss", new String[] {});
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
            case RESTOCK:
                Restock();
                break;
            case TRAVEL_ARDOUGNE:
                TravelToArdougne(ArdougneFarmingState);
                break;
            case ARDOUGNE:
                FarmHerbs(ArdougneFarmingState);
                break;
            case TRAVEL_CATHERBY:
                TravelToCatherby(CatherbyFarmingState);
                break;
            case CATHERBY:
                FarmHerbs(CatherbyFarmingState);
                break;
            case TRAVEL_FALADOR:
                TravelToFalador(FaladorFarmingState);
                break;
            case FALADOR:
                FarmHerbs(FaladorFarmingState);
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

        if (!hasStocked && !config.debugDisableRestock())
            return State.RESTOCK;

        if (config.enableArdougne() && ArdougneFarmingState.HerbPatchState.Index < 2)
        {
            return State.TRAVEL_ARDOUGNE;
        }
        if (config.enableArdougne() && ArdougneFarmingState.HerbPatchState.Index >= 2 &&
                ArdougneFarmingState.HerbPatchState != ProcessState.DONE)
        {
            return State.ARDOUGNE;
        }

        if (config.enableCatherby() && CatherbyFarmingState.HerbPatchState.Index < 2)
        {
            return State.TRAVEL_FALADOR;
        }
        if (config.enableCatherby() && CatherbyFarmingState.HerbPatchState.Index >= 2 &&
                CatherbyFarmingState.HerbPatchState != ProcessState.DONE)
        {
            return State.FALADOR;
        }

        if (config.enableFalador() && FaladorFarmingState.HerbPatchState.Index < 2)
        {
            return State.TRAVEL_FALADOR;
        }
        if (config.enableFalador() && FaladorFarmingState.HerbPatchState.Index >= 2 &&
                FaladorFarmingState.HerbPatchState != ProcessState.DONE)
        {
            return State.FALADOR;
        }

        Stop("Done");
        return null;
    }


    private ProcessState FarmHerbsState(FarmingState currentState)
    {
        //Check if there is any weeds in the inventory and drop them if there is
        if (Inventory.search().withName("Weeds").first().isPresent())
            return ProcessState.EMPTY_INVENTORY;

        //Check if the patch is not fully grown then mark the patch as done
        if (TileObjects.search().withName("Herbs").first().isPresent() && currentState.HerbPatchState != ProcessState.PLANTING)
            if (!Arrays.asList(TileObjectQuery.getObjectComposition(TileObjects.search().withName("Herbs").first().get()).getActions()).contains("Pick"))
                return ProcessState.NOTE;

        //Harvest herbs if there is any
        if (TileObjects.search().withName("Herbs").withAction("Pick").first().isPresent())
            return ProcessState.HARVEST;

        //Clear dead herbs
        if (TileObjects.search().withName("Dead herbs").withAction("Clear").first().isPresent())
            return ProcessState.CLEAR;

        //Plant new herbs
        if (TileObjects.search().nameContains("Herb patch").withAction("Rake").first().isPresent()){
            return ProcessState.RAKE;
        }

        //Check if herb patch is ready for planting
        if (TileObjects.search().nameContains("Herb patch").withAction("Inspect").first().isPresent()) {
            if (Inventory.search().withName(config.seed()).first().isPresent())
                //Use seed on herb patch
                return ProcessState.PLANTING;
        }

        //Use compost on the herbs
        if (TileObjects.search().nameContains("Herbs").withAction("Inspect").first().isPresent()) {
            if (Inventory.search().withName(config.compost()).first().isPresent())
                return ProcessState.COMPOST;
        }

        return ProcessState.PROCESS_HERB_PATCH;
    }

    private void FarmHerbs(FarmingState state)
    {
        if (state.HerbPatchState.Index < 2 || state.HerbPatchState == ProcessState.DONE)
            return;

        state.HerbPatchState = FarmHerbsState(state);
        SetDisplayState(state);

        switch (state.HerbPatchState)
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
                    Inventory.search().withName(config.seed()).first().ifPresent(item -> {
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
                    state.HerbPatchState = ProcessState.COMPOST;
                    Inventory.search().withName(config.compost()).first().ifPresent(item -> {
                        //Use compost on herb patch
                        MousePackets.queueClickPacket();
                        MousePackets.queueClickPacket();
                        ObjectPackets.queueWidgetOnTileObject(item, tileObject);
                        state.HerbPatchState = ProcessState.NOTE;
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
                state.HerbPatchState = ProcessState.DONE;
                break;
        }
        setTimeout();
    }

    private void Restock()
    {
        if (Bank.isOpen()) {
            ArrayList<String> keepItems = new ArrayList<String>(Arrays.asList(Tools));

            if (config.enableArdougne())
                keepItems.addAll(Arrays.asList(ArdougneFarmingState.Tools));
            if (config.enableCatherby())
                keepItems.addAll(Arrays.asList(CatherbyFarmingState.Tools));
            if (config.enableCivitasIllaFortis())
                keepItems.addAll(Arrays.asList(CivitasIllaFortisFarmingState.Tools));
            if (config.enableFalador())
                keepItems.addAll(Arrays.asList(FaladorFarmingState.Tools));
            if (config.enableFarmingGuild())
                keepItems.addAll(Arrays.asList(FarmingGuildFarmingState.Tools));
            if (config.enableHarmonyIsland())
                keepItems.addAll(Arrays.asList(HarmonyIslandFarmingState.Tools));
            if (config.enableHosidius())
                keepItems.addAll(Arrays.asList(HosidiusFarmingState.Tools));
            if (config.enablePortPhasmatys())
                keepItems.addAll(Arrays.asList(PortPhasmatysFarmingState.Tools));
            if (config.enableTrollStronghold())
                keepItems.addAll(Arrays.asList(TrollStrongholdFarmingState.Tools));
            if (config.enableWeiss())
                keepItems.addAll(Arrays.asList(WeissFarmingState.Tools));

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
            if (!TakeOutItemFromBank(config.seed(), patches)) {
                Stop("Missing " + config.seed() + " in bank");
                return;
            }

            //Take out compost
            if (!TakeOutItemFromBank(config.compost(), patches)) {
                Stop("Missing " + config.seed() + " in bank");
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
                for (String tool : ArdougneFarmingState.Tools)
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

            //Take out tools that are needed for the Falador herb patch
            if (config.enableFalador())
            {
                for (String tool : FaladorFarmingState.Tools)
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
            hasStocked = true;
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

    private void TravelToArdougne(FarmingState state)
    {
        ArdougneFarmingState.HerbPatchState = ProcessState.TRAVEL;
        SetDisplayState(state);

        if (ArdougneFarmingState.PathIndex == 0)
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
            ArdougneFarmingState.PathIndex = 1;
            timeout = 4;
            return;
        }
        if (ArdougneFarmingState.PathIndex == 1)
        {
            ArdougneFarmingState.HerbPatchState = ProcessState.PROCESS_HERB_PATCH;
            return;
        }
        if (ArdougneFarmingState.PathIndex == 2)
        {
            if (CastTeleportSpell(WidgetInfoExtended.SPELL_ARDOUGNE_TELEPORT))
            {
                ResetPath();
                ArdougneFarmingState.PathIndex = 3;
                setTimeout();
            }
            else
            {
                Stop("Couldn't teleport to Ardougne");
            }
            return;
        }
        if (ArdougneFarmingState.PathIndex == 3)
        {
            if (TravelPath(ArdougneFarmingState.Paths.get("Teleport")))
            {
                ArdougneFarmingState.HerbPatchState = ProcessState.PROCESS_HERB_PATCH;
                setTimeout();
            }
        }
    }

    private void TravelToFalador(FarmingState state)
    {
        FaladorFarmingState.HerbPatchState = ProcessState.TRAVEL;
        SetDisplayState(state);

        if (FaladorFarmingState.PathIndex == 0)
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
            FaladorFarmingState.PathIndex = 1;
            timeout = 8;
            return;
        }
        if (FaladorFarmingState.PathIndex == 1)
        {
            FaladorFarmingState.HerbPatchState = ProcessState.PROCESS_HERB_PATCH;
            return;
        }
        if (FaladorFarmingState.PathIndex == 10)
        {
            if (CastTeleportSpell(WidgetInfoExtended.SPELL_FALADOR_TELEPORT))
            {
                ResetPath();
                FaladorFarmingState.PathIndex = 11;
                setTimeout();
            }
            else
            {
                Stop("Couldn't teleport to Falador");
            }
            return;
        }
        if (FaladorFarmingState.PathIndex == 11)
        {
            if (TravelPath(FaladorFarmingState.Paths.get("Teleport1")))
            {
                FaladorFarmingState.PathIndex = 12;
                setTimeout();
            }
            return;
        }
        if (FaladorFarmingState.PathIndex == 12)
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
            FaladorFarmingState.PathIndex = 13;
            return;
        }
        if (FaladorFarmingState.PathIndex == 13)
        {
            if (TravelPath(FaladorFarmingState.Paths.get("Teleport2")))
            {
                FaladorFarmingState.PathIndex = 14;
                setTimeout();
            }
            return;
        }
        if (FaladorFarmingState.PathIndex == 14)
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
            FaladorFarmingState.HerbPatchState = ProcessState.PROCESS_HERB_PATCH;
        }

    }

    private void TravelToCatherby(FarmingState state)
    {
        CatherbyFarmingState.HerbPatchState = ProcessState.TRAVEL;
        SetDisplayState(state);

        if (CatherbyFarmingState.PathIndex == 0)
        {
            if (CastTeleportSpell(WidgetInfoExtended.SPELL_CATHERBY_TELEPORT))
            {
                ResetPath();
                CatherbyFarmingState.PathIndex = 1;
                setTimeout();
            }
            else
            {
                Stop("Couldn't teleport to Catherby");
            }
        }
        if (CatherbyFarmingState.PathIndex == 1)
        {
            if (TravelPath(CatherbyFarmingState.Paths.get("Camelot")))
            {
                CatherbyFarmingState.PathIndex = 2;
                setTimeout();
            }
            return;
        }
        if (CatherbyFarmingState.PathIndex == 2)
        {
            CatherbyFarmingState.HerbPatchState = ProcessState.PROCESS_HERB_PATCH;
            setTimeout();
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

    private void SetDisplayState(FarmingState state)
    {
        FarmingStateDisplay = state;
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
