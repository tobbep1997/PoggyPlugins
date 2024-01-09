package com.piggyplugins.BobTheWizard;

import net.runelite.client.config.*;

@ConfigGroup("BobTheWizard")
public interface BobTheWizardConfig extends Config {
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
            keyName = "teleport",
            name = "Teleport",
            description = "",
            position = -2
    )
    default Teleport teleport() {
        return Teleport.CAMELOT;
    }

    @ConfigItem(
            keyName = "alc",
            name = "High Alc",
            description = "",
            position = -2
    )
    default String alc() {
        return "";
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
            name = "Teleport Tick Delay Min",
            keyName = "teleportTickDelayMin",
            description = "Lower bound of tick delay, can set both to 0 to remove delay",
            position = 6,
            section = tickDelaySection
    )
    default int teleportTickDelayMin() {
        return 0;
    }


    @ConfigItem(
            name = "Teleport Tick Delay Max",
            keyName = "teleportTickDelayMax",
            description = "Upper bound of tick delay, can set both to 0 to remove delay",
            position = 7,
            section = tickDelaySection
    )
    default int teleportTickDelayMax() {
        return 1;
    }


}
