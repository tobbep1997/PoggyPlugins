package com.piggyplugins.BobTheCombatBoy;

import net.runelite.client.config.*;

@ConfigGroup("BobTheCombatBoy")
public interface BobTheCombatBoyConfig extends Config {
    @ConfigSection(
            name = "Tick Delays",
            description = "Configuration for delays added to skilling activities",
            position = 4

    )
    String tickDelaySection = "Tick Delays";

    @ConfigSection(
            name = "Items selection",
            description = "Configuration for items",
            position = 2

    )
    String itemSelection = "Item selection";

    @ConfigSection(
            name = "Cannon",
            description = "Configuration for cannon",
            position = 3

    )
    String cannonSelection = "Cannon selection";

    @ConfigItem(
            keyName = "toggle",
            name = "Toggle",
            description = "",
            position = 1
    )
    default Keybind toggle() {
        return Keybind.NOT_SET;
    }

    @ConfigItem(
            keyName = "target",
            name = "Target",
            description = "",
            position = 1
    )
    default String target() {
        return "Rat";
    }
    @ConfigItem(
            keyName = "eat",
            name = "Eat HP",
            description = "",
            position = 2
    )
    default int eat() {
        return 50;
    }
    @ConfigItem(
            keyName = "bury",
            name = "Bury bones",
            description = "",
            position = 2
    )
    default boolean bury() {
        return true;
    }

    @ConfigItem(
            name = "Tick Delay Min",
            keyName = "tickDelayMin",
            description = "Lower bound of tick delay, can set both to 0 to remove delay",
            position = 4,
            section = tickDelaySection
    )
    default int tickdelayMin() {
        return 0;
    }

    @ConfigItem(
            name = "Tick Delay Max",
            keyName = "tickDelayMax",
            description = "Upper bound of tick delay, can set both to 0 to remove delay",
            position = 5,
            section = tickDelaySection
    )
    default int tickDelayMax() {
        return 3;
    }

    @ConfigItem(
            name = "Loot",
            keyName = "loot",
            description = "",
            position = 0,
            section = itemSelection
    )
    default boolean loot() {
        return true;
    }
    @ConfigItem(
            name = "Loot coins",
            keyName = "lootCoins",
            description = "",
            position = 0,
            section = itemSelection
    )
    default boolean lootCoins() {
        return true;
    }

    @ConfigItem(
            name = "Use GE or HA Value",
            keyName = "useHAValue",
            description = "Check for HA value and uncheck for GE value",
            position = 1,
            section = itemSelection
    )
    default boolean useHAValue() {
        return false;
    }

    @ConfigItem(
            name = "Min value",
            keyName = "minVal",
            description = "",
            position = 2,
            section = itemSelection
    )
    default int minVal() {
        return 5000;
    }

    @ConfigItem(
            name = "Min stackable value",
            keyName = "minStackVal",
            description = "",
            position = 3,
            section = itemSelection
    )
    default int minStackVal() {
        return 1000;
    }

    @ConfigItem(
            name = "Use cannon",
            keyName = "useCannon",
            description = "",
            position = 0,
            section = cannonSelection
    )
    default boolean useCannon() {
        return true;
    }

    @ConfigItem(
            name = "Cannon time",
            keyName = "cannonTime",
            description = "",
            position = 1,
            section = cannonSelection
    )
    default int cannonTime() {
        return 30;
    }
}
