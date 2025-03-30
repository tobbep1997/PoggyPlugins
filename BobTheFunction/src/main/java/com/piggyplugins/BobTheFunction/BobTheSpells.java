package com.piggyplugins.BobTheFunction;

import com.example.EthanApiPlugin.Collections.Widgets;
import com.example.PacketUtils.WidgetInfoExtended;
import com.example.Packets.MousePackets;
import com.example.Packets.WidgetPackets;
import net.runelite.api.widgets.Widget;

import java.util.Optional;

public class BobTheSpells {
    //Casts a spell and returns true if the spell has been casted
    public static boolean CastTeleportSpell(WidgetInfoExtended spell, String Cast) {
        Optional<Widget> teleportSpellIcon = Widgets.search().withId(spell.getPackedId()).first();
        if (teleportSpellIcon.isPresent()) {
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetAction(teleportSpellIcon.get(), Cast);
            return true;
        }
        return false;
    }

    //Casts a spell and returns true if the spell has been casted
    public static boolean CastTeleportSpell(WidgetInfoExtended spell) {
        return CastTeleportSpell(spell, "Cast");
    }

}
