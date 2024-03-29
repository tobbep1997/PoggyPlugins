package com.piggyplugins.BobTheFarmer;

import com.example.EthanApiPlugin.Collections.*;
import com.example.EthanApiPlugin.Collections.query.TileObjectQuery;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.BankInteraction;
import com.example.InteractionApi.BankInventoryInteraction;
import com.example.InteractionApi.InventoryInteraction;
import com.example.InteractionApi.TileObjectInteraction;
import com.example.PacketUtils.WidgetInfoExtended;
import com.example.Packets.*;
import com.google.inject.Provides;
import com.piggyplugins.PiggyUtils.API.BankUtil;
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

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;


class FarmingState
{
    public String[] Tools = {};
    public ProcessState HerbPatchState = ProcessState.NOT_STARTED;

    public Dictionary<String, WorldPoint[]> Paths = new Hashtable<>();

    public FarmingState(String[] tools)
    {
        this.Tools = tools;
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
    @Inject
    private ReflectBreakHandler breakHandler;
    State state;
    WorldPoint debugPoint = null;
    boolean started;
    boolean herbRun;
    boolean treeRun;
    private int timeout;
    public String debug = "";
    private boolean hasStocked = false;
    private final String[] Tools =  {"Magic secateurs", "Spade", "Rake", "Seed dibber" };
    public ProcessState FarmingStateDisplay = null;
    private FarmingState ArdougneFarmingState = null;
    private FarmingState FaladorFarmingState = null;
    private FarmingState PortPhasmatysFarmingState = null;
    private FarmingState CatherbyFarmingState = null;
    private FarmingState HosidiusFarmingState = null;
    private FarmingState TrollStrongholdFarmingState = null;
    private FarmingState HarmonyIslandFarmingState = null;
    private FarmingState WeissFarmingState = null;
    private FarmingState FarmingGuildFarmingState = null;
    private FarmingState CivitasIllaFortisFarmingState = null;




    @Override
    protected void startUp() throws Exception {
        keyManager.registerKeyListener(hotkeyListenerToggle);
        keyManager.registerKeyListener(hotkeyListenerHerbRun);
        keyManager.registerKeyListener(hotkeyListenerDebug);
        breakHandler.registerPlugin(this);
        this.overlayManager.add(overlay);
    }

    @Override
    protected void shutDown() throws Exception {
        keyManager.unregisterKeyListener(hotkeyListenerToggle);
        keyManager.unregisterKeyListener(hotkeyListenerHerbRun);
        keyManager.unregisterKeyListener(hotkeyListenerDebug);
        breakHandler.unregisterPlugin(this);
        this.overlayManager.remove(overlay);
    }

    @Provides
    private BobTheFarmerConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(BobTheFarmerConfig.class);
    }

    @Subscribe
    private void onGameTick(GameTick event) {
        if (!EthanApiPlugin.loggedIn() || (!started || !(herbRun || treeRun)) || breakHandler.isBreakActive(this)) {
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
        ArdougneFarmingState = new FarmingState(new String[] {});
        ArdougneFarmingState.SetPath(Paths.ArdougneTeleportPath, "Teleport");

        FaladorFarmingState = new FarmingState(new String[] {});
        PortPhasmatysFarmingState = new FarmingState(new String[] {});
        CatherbyFarmingState = new FarmingState(new String[] {});
        HosidiusFarmingState = new FarmingState(new String[] {});
        TrollStrongholdFarmingState = new FarmingState(new String[] {});
        HarmonyIslandFarmingState = new FarmingState(new String[] {});
        WeissFarmingState = new FarmingState(new String[] {});
        FarmingGuildFarmingState = new FarmingState(new String[] {});
        CivitasIllaFortisFarmingState = new FarmingState(new String[] {});
    }

    private void handleState() {
        if (state == null)
            return;

        switch (state) {
            case ANIMATING:
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
            case TRAVLE_ARDOUGNE:
                TravelArdougne();
                SetDisplayState(ArdougneFarmingState);
                setTimeout();
                break;
            case ARDOUGNE:
                FarmHerbs(ArdougneFarmingState);
                setTimeout();
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
        if (!herbRun && !treeRun)
            return null;

        if (!hasStocked && !config.debugDisableRestock())
            return State.RESTOCK;

        if (config.enableArdougne() && ArdougneFarmingState.HerbPatchState.Index < 2)
        {
            return State.TRAVLE_ARDOUGNE;
        }
        if (config.enableArdougne() && ArdougneFarmingState.HerbPatchState.Index >= 2 &&
                ArdougneFarmingState.HerbPatchState != ProcessState.DONE)
        {
            return State.ARDOUGNE;
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
                return ProcessState.DONE;

        //Harvest herbs if there is any
        if (TileObjects.search().withName("Herbs").withAction("Pick").first().isPresent())
            return ProcessState.HARVEST;

        //Harvest herbs if there is any
        if (TileObjects.search().withName("Herbs").withAction("Clear").first().isPresent())
            return ProcessState.HARVEST;

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
                        state.HerbPatchState = ProcessState.DONE;
                    });
                });
                break;
            case EMPTY_INVENTORY: //Check if there is any weeds in the inventory and drop them if there is
                Inventory.search().withName("Weeds").first().ifPresent(weeds -> {
                    MousePackets.queueClickPacket();
                    InventoryInteraction.useItem(weeds, "Drop");
                });
                break;
        }
    }

    private void Restock()
    {
        if (Bank.isOpen()) {
            ArrayList<String> keepItems = new ArrayList<String>(Arrays.asList(Tools));

            if (config.enableArdougne())
                keepItems.addAll(Arrays.asList(ArdougneFarmingState.Tools));

            if (config.enableFalador())
                keepItems.addAll(Arrays.asList(FaladorFarmingState.Tools));

            if (config.enablePortPhasmatys())
                keepItems.addAll(Arrays.asList(PortPhasmatysFarmingState.Tools));

            if (config.enableCatherby())
                keepItems.addAll(Arrays.asList(CatherbyFarmingState.Tools));

            if (config.enableHosidius())
                keepItems.addAll(Arrays.asList(HosidiusFarmingState.Tools));

            if (config.enableTrollStronghold())
                keepItems.addAll(Arrays.asList(TrollStrongholdFarmingState.Tools));

            if (config.enableHarmonyIsland())
                keepItems.addAll(Arrays.asList(HarmonyIslandFarmingState.Tools));

            if (config.enableWeiss())
                keepItems.addAll(Arrays.asList(WeissFarmingState.Tools));

            if (config.enableFarmingGuild())
                keepItems.addAll(Arrays.asList(FarmingGuildFarmingState.Tools));

            if (config.enableCivitasIllaFortis())
                keepItems.addAll(Arrays.asList(CivitasIllaFortisFarmingState.Tools));

            int patches = 0;
            patches += config.enableArdougne() ? 1 : 0;
            patches += config.enableFalador() ? 1 : 0;
            patches += config.enablePortPhasmatys() ? 1 : 0;
            patches += config.enableCatherby() ? 1 : 0;
            patches += config.enableHosidius() ? 1 : 0;
            patches += config.enableTrollStronghold() ? 1 : 0;
            patches += config.enableHarmonyIsland() ? 1 : 0;
            patches += config.enableWeiss() ? 1 : 0;
            patches += config.enableFarmingGuild() ? 1 : 0;
            patches += config.enableCivitasIllaFortis() ? 1 : 0;

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

                boolean gotCloak = false;
                for (int i = 4; i >= 2; i--) {
                    if (TakeOutItemFromBank("Ardougne cloak " + i, 1)) {
                        gotCloak = true;
                        break;
                    }
                }
                if (!gotCloak)
                {
                    Stop("Missing Ardougne cloak 2 or higher in bank");
                    return;
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

    private void TravelArdougne()
    {
        ArdougneFarmingState.HerbPatchState = ProcessState.TRAVEL;

        boolean teleported = false;
        for (int i = 4; i >= 2; i--) {
            if (Inventory.search().withName("Ardougne cloak " + i).first().isPresent())
            {
                teleported = true;
                MousePackets.queueClickPacket();
                InventoryInteraction.useItem(
                        Inventory.search().withName("Ardougne cloak " + i).first().get(),
                        "Farm Teleport");
                ArdougneFarmingState.HerbPatchState = ProcessState.PROCESS_HERB_PATCH;
            }

        }
        if (!teleported)
        {
            if (!CastTeleportSpell(WidgetInfoExtended.SPELL_ARDOUGNE_TELEPORT))
            {
                Stop("Couldn't teleport to Ardougne");
            }
            else
            {
                ArdougneFarmingState.HerbPatchState = ProcessState.PROCESS_HERB_PATCH;
            }
        }
        //TODO: REMOVE ME
        ArdougneFarmingState.HerbPatchState = ProcessState.PROCESS_HERB_PATCH;
    }

    private boolean CastTeleportSpell(WidgetInfoExtended spell)
    {
        Optional<Widget> teleportSpellIcon = Widgets.search().withId(spell.getPackedId()).first();
        if (teleportSpellIcon.isPresent()) {
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetAction(teleportSpellIcon.get(), "Cast");
        }

        return false;
    }

    private void SetDisplayState(FarmingState state)
    {
        FarmingStateDisplay = state.HerbPatchState;
    }

    private void setTimeout() {
        timeout = RandomUtils.nextInt(config.tickdelayMin(), config.tickDelayMax());
    }

    public void Stop(String reason)
    {
        started = false;
        debug = reason;
        this.state = State.TIMEOUT;
        breakHandler.stopPlugin(this);
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
            this.state = State.TIMEOUT;
            breakHandler.stopPlugin(this);
        } else {
            breakHandler.startPlugin(this);
        }
    }
}
