package com.piggyplugins.BobTheFarmer;

import com.google.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import java.awt.*;

public class BobTheFarmerOverlay extends OverlayPanel {

    private final BobTheFarmerPlugin plugin;

    @Inject
    private BobTheFarmerOverlay(BobTheFarmerPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
        setPosition(OverlayPosition.BOTTOM_LEFT);
        setPreferredSize(new Dimension(160, 160));
    }


    @Override
    public Dimension render(Graphics2D graphics) {
        panelComponent.setPreferredSize(new Dimension(200, 320));
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Bob The Farmer")
                .color(new Color(255, 157, 249))
                .build());
        panelComponent.getChildren().add(TitleComponent.builder()
                .text(plugin.started ? "Running" + (plugin.herbRun ? ", Herb run" : "") : "Paused")
                .color(plugin.started ? Color.GREEN : Color.RED)
                .build());
        panelComponent.getChildren().add(LineComponent.builder()
                .left("State: ")
                .leftColor(new Color(255, 157, 249))
                .right(plugin.state==null || !plugin.started ? "STOPPED" : plugin.state.name())
                .rightColor(Color.WHITE)
                .build());
        panelComponent.getChildren().add(LineComponent.builder()
                .left(plugin.state==null || plugin.FarmingStateDisplay==null || !plugin.started ? "" : plugin.FarmingStateDisplay.Name)
                .leftColor(new Color(255, 157, 249))
                .right(plugin.state==null || plugin.FarmingStateDisplay==null || !plugin.started ? "" : plugin.FarmingStateDisplay.HerbPatchState.name())
                .rightColor(Color.WHITE)
                .build());
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Output: ")
                .leftColor(new Color(255, 157, 249))
                .right(plugin.debug)
                .rightColor(Color.WHITE)
                .build());
        panelComponent.getChildren().add(LineComponent.builder()
                .left("POINT: ")
                .leftColor(new Color(255, 157, 249))
                .right(plugin.debugPoint.toString())
                .rightColor(Color.WHITE)
                .build());




        return super.render(graphics);
    }
}