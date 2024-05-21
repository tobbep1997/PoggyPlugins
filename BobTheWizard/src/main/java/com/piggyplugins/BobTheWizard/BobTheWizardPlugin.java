package com.piggyplugins.BobTheWizard;

import com.example.EthanApiPlugin.Collections.*;
import com.example.EthanApiPlugin.Collections.query.TileObjectQuery;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.BankInteraction;
import com.example.InteractionApi.BankInventoryInteraction;
import com.example.InteractionApi.TileObjectInteraction;
import com.example.PacketUtils.WidgetInfoExtended;
import com.example.Packets.MousePackets;
import com.example.Packets.WidgetPackets;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;


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
    BankState bankState;


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
            bankState = BankState.DEPOSIT;
            return;
        }

        state = getNextState();
        handleState();
    }

    private void handleState() {
        if (state == State.MAKE_PLANK)
            bankState = BankState.DEPOSIT;

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
            case MAKE_PLANK:
                MakePlank();
                break;
            case BANK:
                Restock();
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

        if (config.trainingMethod() == TrainingMethod.TeleAlc)
        {
            if (alced || config.alc().isEmpty())
            {
                return State.CAST_TELEPORT;
            }

            return State.CAST_HIGH_ALC;
        }

        if (config.trainingMethod() == TrainingMethod.PlankMake)
        {
            if (Inventory.search().nameContains(config.plank()).first().isPresent())
                return State.MAKE_PLANK;
            else
                return State.BANK;
        }

        return State.NONE;
    }
    private void Restock() {

        //Check if bank is open
        if (Bank.isOpen()) {
            //Add all items that are needed to the keepItems List
            ArrayList<String> keepItems = new ArrayList<String>();
            keepItems.add("Rune pouch");
            keepItems.add("Coins");
            keepItems.add("Astral rune");
            keepItems.add("Nature rune");
            keepItems.add(config.plank());

            //Banking state machine
            switch (bankState){

                //Deposit items that are not needed
                case DEPOSIT:
                    List<Widget> bankInv = BankInventory.search().filter(widget -> !CompareItem(keepItems, widget.getName())).result();
                    for (Widget item : bankInv) {
                        MousePackets.queueClickPacket();
                        BankInventoryInteraction.useItem(item, "Deposit-All");
                    }
                    bankState = BankState.WITHDRAW;
                    break;
                //Withdraw items in non noted form
                case WITHDRAW:
                        //Take out coins
                        if (!WithdrawItemFromBank("Coins", -1)) {
                            started = false;
                            return;
                        }
                        if (!WithdrawItemFromBank(config.plank(), Inventory.getEmptySlots())) {
                            started = false;
                            return;
                        }
                    break;
            }
            setTimeout();
        }
        else {
            OpenBank();
        }
    }

    private int CalculateDistance(int x1, int y1, int x2, int y2)
    {
        return (int)Math.round(Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2)));
    }

    private TileObject BankCloseBy(int distance) {
        Optional<TileObject> bankBooth = null;
        bankBooth = TileObjects.search().withAction("Bank").withinDistance(distance).nearestToPlayer();

        Optional<TileObject> bankChest = null;
        bankChest = TileObjects.search().withName("Bank chest").withinDistance(distance).nearestToPlayer();

        if (bankBooth.isPresent() && bankChest.isPresent())
        {
            WorldPoint playerPos = client.getLocalPlayer().getWorldLocation();

            int bbDist = CalculateDistance(playerPos.getX(), playerPos.getY(), bankBooth.get().getX(), bankBooth.get().getY());
            int bcDist = CalculateDistance(playerPos.getX(), playerPos.getY(), bankChest.get().getX(), bankChest.get().getY());

            return bbDist < bcDist ? bankBooth.get() : bankChest.get();
        }
        if (bankBooth.isPresent())
            return bankBooth.get();
        if (bankChest.isPresent())
            return bankChest.get();
        return null;
    }

    //Returns the interaction of a bank object
    private String GetBankInteraction(TileObject bankObject) {
        ObjectComposition objectComposition = TileObjectQuery.getObjectComposition(bankObject);
        return Arrays.stream(objectComposition.getActions()).anyMatch(action -> action != null && action.toLowerCase().contains("bank")) ? "Bank" : "Use";
    }

    //Opens the closes bank
    private void OpenBank() {
        TileObject bankBooth = BankCloseBy(20);
        if (bankBooth != null) {
            MousePackets.queueClickPacket();
            TileObjectInteraction.interact(bankBooth, GetBankInteraction(bankBooth));
        }
    }

    //Take out a set amount of items from the bank
    //This double checks if you have any items in the invetory already so it dosen't withdraw too much
    private boolean WithdrawItemFromBank(String item, int amount) {
        AtomicBoolean succeeded = new AtomicBoolean(false);
        BankUtil.nameContainsNoCase(item).first().ifPresentOrElse(widget ->
                {
                    int localAmount = amount -
                            (
                                    Inventory.search().withName(item).first().isPresent() ?
                                            (
                                                    Inventory.search().isStackable(Inventory.search().withName(item).first().get()) ?
                                                            Inventory.search().withName(item).first().get().getItemQuantity() :
                                                            Inventory.search().withName(item).result().size()
                                            ) : 0
                            );

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

    private boolean CompareItem(List<String> keepItems, String compItem) {
        for (String item : keepItems)
        {
            if (compItem.contains(item))
                return true;
        }
        return false;
    }

    private void MakePlank()
    {
        Widget PlankMake = client.getWidget(WidgetInfoExtended.SPELL_PLANK_MAKE.getPackedId());
        if (PlankMake != null) {
            Inventory.search().nameContains(config.plank()).first().ifPresentOrElse(item -> {
                MousePackets.queueClickPacket();
                WidgetPackets.queueWidgetOnWidget(PlankMake, item);
            }, null);
            setTimeout();
        }
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
