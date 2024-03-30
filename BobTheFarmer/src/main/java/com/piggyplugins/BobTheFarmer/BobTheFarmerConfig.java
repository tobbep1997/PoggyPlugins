package com.piggyplugins.BobTheFarmer;

import net.runelite.client.config.*;

@ConfigGroup("BobTheBlower")
public interface BobTheFarmerConfig extends Config {
    @ConfigSection(
            name = "Tick Delays",
            description = "Configuration for delays added to skilling activities",
            position = 3

    )
    String tickDelaySection = "Tick Delays";

    @ConfigSection(
            name = "Herb patches",
            description = "Configuration for herb patches",
            position = 2

    )
    String herbPatches = "Herb patches";

    @ConfigSection(
            name = "General",
            description = "General configuration",
            position = 2

    )
    String generalConfiguration = "General configuration";

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
            keyName = "doHerbRun",
            name = "Do Herb run",
            description = "",
            position = 2
    )
    default Keybind doHerbRun() {
        return Keybind.NOT_SET;
    }
    @ConfigItem(
            keyName = "debugKey",
            name = "Bob the debugger",
            description = "",
            position = 2
    )
    default Keybind debugKey() {
        return Keybind.NOT_SET;
    }



    @ConfigItem(
            keyName = "seed",
            name = "Seed",
            description = "",
            position = 1,
            section = generalConfiguration

    )
    default String seed() {
        return "Avantoe seed";
    }

    @ConfigItem(
            keyName = "compost",
            name = "Compost",
            description = "",
            position = 1,
            section = generalConfiguration
    )
    default String compost() {
        return "Ultracompost";
    }

    @ConfigItem(
            keyName = "additionalItems",
            name = "Additional items",
            description = "",
            position = 1,
            section = generalConfiguration
    )
    default String additionalItems() {
        return "Rune pouch";
    }
    @ConfigItem(
            keyName = "debugDisableRestock",
            name = "debugDisableRestock",
            description = "",
            position = 3,
            section = generalConfiguration
    )
    default boolean debugDisableRestock() {
        return false;
    }

    @ConfigItem(
            name = "Ardougne herb patch",
            keyName = "enableArdougne",
            description = "Requires Ardougne cloak 2 or higher",
            position = 0,
            section = herbPatches
    )
    default boolean enableArdougne() {
        return false;
    }
    @ConfigItem(
            name = "Catherby herb patch",
            keyName = "enableCatherby",
            description = "",
            position = 1,
            section = herbPatches
    )
    default boolean enableCatherby() {
        return false;
    }
    @ConfigItem(
            name = "Civitas illa Fortis herb patch",
            keyName = "enableCivitasIllaFortis",
            description = "",
            position = 2,
            section = herbPatches
    )
    default boolean enableCivitasIllaFortis() {
        return false;
    }
    @ConfigItem(
            name = "Falador herb patch",
            keyName = "enableFalador",
            description = "Requires Explorer's ring 2 or higher",
            position = 3,
            section = herbPatches
    )
    default boolean enableFalador() {
        return false;
    }
    @ConfigItem(
            name = "Farming guild herb patch",
            keyName = "enableFarmingGuild",
            description = "",
            position = 4,
            section = herbPatches
    )
    default boolean enableFarmingGuild() {
        return false;
    }
    @ConfigItem(
            name = "Harmony Island herb patch",
            keyName = "enableHarmonyIsland",
            description = "",
            position = 5,
            section = herbPatches
    )
    default boolean enableHarmonyIsland() {
        return false;
    }
    @ConfigItem(
            name = "Hosidius herb patch",
            keyName = "enableHosidius",
            description = "",
            position = 5,
            section = herbPatches
    )
    default boolean enableHosidius() {
        return false;
    }
    @ConfigItem(
            name = "Port Phasmatys herb patch",
            keyName = "enablePortPhasmatys",
            description = "",
            position = 6,
            section = herbPatches
    )
    default boolean enablePortPhasmatys() {
        return false;
    }


    @ConfigItem(
            name = "Troll Stronghold herb patch",
            keyName = "enableTrollStronghold",
            description = "",
            position = 7,
            section = herbPatches
    )
    default boolean enableTrollStronghold() {
        return false;
    }

    @ConfigItem(
            name = "Weiss herb patch",
            keyName = "enableWeiss",
            description = "",
            position = 8,
            section = herbPatches
    )
    default boolean enableWeiss() {
        return false;
    }



    @ConfigItem(
            name = "Tick Delay Min",
            keyName = "tickDelayMin",
            description = "Lower bound of tick delay, can set both to 0 to remove delay",
            position = 0,
            section = tickDelaySection
    )
    default int tickDelayMin() {
        return 0;
    }

    @ConfigItem(
            name = "Tick Delay Max",
            keyName = "tickDelayMax",
            description = "Upper bound of tick delay, can set both to 0 to remove delay",
            position = 1,
            section = tickDelaySection
    )
    default int tickDelayMax() {
        return 3;
    }



}
