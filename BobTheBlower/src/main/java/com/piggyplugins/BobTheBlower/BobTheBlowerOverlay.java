package com.piggyplugins.BobTheBlower;

import com.google.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import java.awt.*;

public class BobTheBlowerOverlay extends OverlayPanel {

    private final BobTheBlowerPlugin plugin;

    @Inject
    private BobTheBlowerOverlay(BobTheBlowerPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
        setPosition(OverlayPosition.BOTTOM_LEFT);
        setPreferredSize(new Dimension(160, 160));
    }


    @Override
    public Dimension render(Graphics2D graphics) {
        panelComponent.setPreferredSize(new Dimension(200, 320));
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Bob The Blower")
                .color(new Color(255, 157, 249))
                .build());
        panelComponent.getChildren().add(TitleComponent.builder()
                .text(plugin.started ? "Running" : "Paused")
                .color(plugin.started ? Color.GREEN : Color.RED)
                .build());
        panelComponent.getChildren().add(LineComponent.builder()
                .left("State: ")
                .leftColor(new Color(255, 157, 249))
                .right(plugin.state==null || !plugin.started ? "STOPPED" : plugin.state.name())
                .rightColor(Color.WHITE)
                .build());
        panelComponent.getChildren().add(LineComponent.builder()
                .left("DEBUG: ")
                .leftColor(new Color(255, 157, 249))
                .right(plugin.debug)
                .rightColor(Color.WHITE)
                .build());
        panelComponent.getChildren().add(LineComponent.builder()
                .left("BLOWING: ")
                .leftColor(new Color(255, 157, 249))
                .right(plugin.blowing ? "BLOWING" : "Single")
                .rightColor(Color.WHITE)
                .build());



        return super.render(graphics);
    }
}
