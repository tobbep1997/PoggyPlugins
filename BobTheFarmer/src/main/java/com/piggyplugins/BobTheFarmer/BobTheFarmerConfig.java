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
    String itemSelection = "Item selection";

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
            keyName = "seed",
            name = "Seed",
            description = "",
            position = 1
    )
    default String seed() {
        return "";
    }

    @ConfigItem(
            name = "Ardougne herb patch",
            keyName = "enableArdougne",
            description = "Requiers Ardougne cloak 2",
            position = 0,
            section = tickDelaySection
    )
    default boolean enableArdougne() {
        return false;
    }




    @ConfigItem(
            name = "Tick Delay Min",
            keyName = "tickDelayMin",
            description = "Lower bound of tick delay, can set both to 0 to remove delay",
            position = 0,
            section = tickDelaySection
    )
    default int tickdelayMin() {
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
