package com.runecrown;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "Runecrown",
	description = "Your clan's next events from Runecrown, with Going / Maybe / Can't buttons",
	tags = {"clan", "events", "calendar", "signup", "guild"}
)
public class RunecrownPlugin extends Plugin
{
	@Inject
	private RunecrownConfig config;

	@Inject
	private RunecrownClient client;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private RunecrownOverlay overlay;

	@Inject
	private ScheduledExecutorService executor;

	@Getter
	private volatile Feed feed;

	private RunecrownPanel panel;
	private NavigationButton button;
	private ScheduledFuture<?> poll;

	@Override
	protected void startUp()
	{
		panel = new RunecrownPanel(this::refresh, this::answer);
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
		button = NavigationButton.builder().tooltip("Runecrown").icon(icon).priority(8).panel(panel).build();
		clientToolbar.addNavigation(button);
		overlayManager.add(overlay);
		// Once a minute is plenty: events are planned hours or days ahead.
		poll = executor.scheduleWithFixedDelay(this::refresh, 2, 60, TimeUnit.SECONDS);
	}

	@Override
	protected void shutDown()
	{
		if (poll != null)
		{
			poll.cancel(true);
			poll = null;
		}
		overlayManager.remove(overlay);
		clientToolbar.removeNavigation(button);
		feed = null;
		panel = null;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged e)
	{
		if (RunecrownConfig.GROUP.equals(e.getGroup()) && "code".equals(e.getKey()))
		{
			feed = null;
			executor.execute(this::refresh);
		}
	}

	private String code()
	{
		return config.code() == null ? "" : config.code().trim();
	}

	void refresh()
	{
		String code = code();
		if (!RunecrownClient.CODE.matcher(code).matches())
		{
			feed = null;
			paint(null, true);
			return;
		}
		client.feed(config.server(), code, f -> {
			// Keep the last good list when the network blips; only a bad code clears it.
			if (f.guild != null || feed == null || "bad_code".equals(f.error) || "no_code".equals(f.error))
			{
				feed = f;
			}
			else
			{
				feed.message = f.message;
			}
			paint(feed, false);
		});
	}

	void answer(String eventId, String status)
	{
		String code = code();
		if (!RunecrownClient.CODE.matcher(code).matches())
		{
			return;
		}
		client.answer(config.server(), code, eventId, status, r -> {
			if (r.error != null && feed != null)
			{
				feed.message = r.message;
				paint(feed, false);
				return;
			}
			refresh();
		});
	}

	private void paint(Feed f, boolean noCode)
	{
		RunecrownPanel p = panel;
		if (p != null)
		{
			SwingUtilities.invokeLater(() -> p.show(f, noCode));
		}
	}

	@Provides
	RunecrownConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RunecrownConfig.class);
	}
}
