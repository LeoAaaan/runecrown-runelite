package com.runecrown;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

// A small box on the game screen: the next event and how long until it starts. Drag it anywhere (Alt + drag).
class RunecrownOverlay extends OverlayPanel
{
	private static final Color GOLD = new Color(230, 185, 85);
	private final RunecrownPlugin plugin;
	private final RunecrownConfig config;

	@Inject
	RunecrownOverlay(RunecrownPlugin plugin, RunecrownConfig config)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		Feed f = plugin.getFeed();
		if (!config.showOnScreen() || f == null || f.guild == null || f.events.isEmpty())
		{
			return null;
		}
		Feed.Event e = f.events.get(0);
		panelComponent.getChildren().add(TitleComponent.builder().text("Runecrown").color(GOLD).build());
		panelComponent.getChildren().add(LineComponent.builder().left(e.title).build());
		String mine = e.me == null ? "not answered" : "signed".equals(e.me) || "late".equals(e.me) ? "going" : "tentative".equals(e.me) ? "maybe" : "can't";
		panelComponent.getChildren().add(LineComponent.builder().left(Feed.until(e.when())).right("you: " + mine).rightColor(e.me == null ? Color.LIGHT_GRAY : GOLD).build());
		return super.render(g);
	}
}
