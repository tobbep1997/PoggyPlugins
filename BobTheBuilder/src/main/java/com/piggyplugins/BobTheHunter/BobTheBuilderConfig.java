package com.piggyplugins.BobTheHunter;

import net.runelite.client.config.*;

@ConfigGroup("BobTheBuilder")
public interface BobTheBuilderConfig extends Config {
    @ConfigSection(
            name = "Tick Delays",
            description = "Configuration for delays added to skilling activities",
            position = 3

    )
    String tickDelaySection = "Tick Delays";

    @ConfigItem(
            keyName = "toggle",
            name = "Toggle",
            description = "",
            position = -2
    )
    default Keybind toggle() {
        return Keybind.NOT_SET;
    }

    @ConfigItem(
            name = "ID",
            keyName = "id",
            description = "",
            position = 4
    )
    default int id() {
        return 30015493;
    }
    @ConfigItem(
            name = "Option",
            keyName = "option",
            description = "",
            position = 4
    )
    default int option() {
        return -1;
    }

    @ConfigItem(
            name = "UseOption",
            keyName = "useOption",
            description = "",
            position = 4
    )
    default boolean useOption() {
        return true;
    }
    @ConfigItem(
            name = "Items",
            keyName = "items",
            description = "",
            position = 4
    )
    default String items() {
        return "Oak plank";
    }

    @ConfigItem(
            name = "Build",
            keyName = "build",
            description = "",
            position = 4
    )
    default String build() {
        return "Larder";
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



}