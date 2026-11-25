package com.runecrown;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.http.api.loottracker.LootRecordType;
import net.runelite.client.util.Text;
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

	@Inject
	private Client gameClient;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ConfigManager configManager;

	@Getter
	private volatile Feed feed;

	// "Your Vorkath kill count is: 512." / "Your completed Chambers of Xeric count is: 50."
	private static final Pattern KC = Pattern.compile("^Your (?:completed |subdued )?(.+?) (?:kill |chest |completion |success |harvest )?count is: ([0-9,]+)");
	// The last kill count seen per boss (the message comes just before the loot).
	private final Map<String, Integer> lastKc = new HashMap<>();

	private RunecrownPanel panel;
	private NavigationButton button;
	private ScheduledFuture<?> poll;

	@Override
	protected void startUp()
	{
		panel = new RunecrownPanel(this::refresh, this::answer, () -> config.dropRaces());
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
		if (RunecrownConfig.GROUP.equals(e.getGroup()) && ("code".equals(e.getKey()) || "dropRaces".equals(e.getKey())))
		{
			feed = null;
			executor.execute(this::refresh);
		}
	}

	/* ---------------- Drop races ----------------
	   Only when "Clan drop races" is on, and only drops that match a race running now are sent. */

	@Subscribe
	public void onChatMessage(ChatMessage e)
	{
		if (e.getType() != ChatMessageType.GAMEMESSAGE && e.getType() != ChatMessageType.SPAM)
		{
			return;
		}
		Matcher m = KC.matcher(Text.removeTags(e.getMessage()));
		if (!m.find())
		{
			return;
		}
		String boss = m.group(1);
		int kc;
		try
		{
			kc = Integer.parseInt(m.group(2).replace(",", ""));
		}
		catch (NumberFormatException x)
		{
			return;
		}
		lastKc.put(Feed.norm(boss), kc);
		// Count this kill for every race on this boss that is running now (kills while the plugin was off are not seen).
		Feed f = feed;
		if (f == null || !config.dropRaces())
		{
			return;
		}
		for (Feed.Race r : f.races)
		{
			if (r.running() && Feed.sameBoss(boss, r.boss))
			{
				String k = "raceKills." + r.id;
				Integer n = configManager.getConfiguration(RunecrownConfig.GROUP, k, Integer.class);
				configManager.setConfiguration(RunecrownConfig.GROUP, k, (n == null ? 0 : n) + 1);
			}
		}
	}

	@Subscribe
	public void onNpcLootReceived(NpcLootReceived e)
	{
		if (e.getNpc() != null)
		{
			loot(e.getNpc().getName(), e.getItems());
		}
	}

	// Raids and other chests (from RuneLite's loot tracker). NPC kills come through onNpcLootReceived.
	@Subscribe
	public void onLootReceived(LootReceived e)
	{
		if (e.getType() != LootRecordType.NPC)
		{
			loot(e.getName(), e.getItems());
		}
	}

	private void loot(String source, Collection<ItemStack> items)
	{
		Feed f = feed;
		String code = code();
		if (f == null || f.races.isEmpty() || !config.dropRaces() || !RunecrownClient.CODE.matcher(code).matches())
		{
			return;
		}
		for (Feed.Race r : f.races)
		{
			if (!r.running() || !Feed.sameBoss(source, r.boss))
			{
				continue;
			}
			for (ItemStack it : items)
			{
				String name = itemManager.getItemComposition(it.getId()).getName();
				for (String want : r.items)
				{
					if (Feed.norm(want).equals(Feed.norm(name)))
					{
						report(r, source, name);
						return;
					}
				}
			}
		}
	}

	private void report(Feed.Race r, String source, String item)
	{
		Player p = gameClient.getLocalPlayer();
		String rsn = p == null ? null : p.getName();
		Integer kc = lastKc.get(Feed.norm(source));
		if (kc == null)
		{
			kc = lastKc.get(Feed.norm(r.boss));
		}
		Integer kills = configManager.getConfiguration(RunecrownConfig.GROUP, "raceKills." + r.id, Integer.class);
		client.drop(config.server(), code(), r.id, source, item, kc, kills, rsn, res -> {
			if (feed != null)
			{
				feed.message = Boolean.TRUE.equals(res.won) ? "You won the drop race \"" + r.title + "\"! Your clan's Discord has been told."
					: res.winner != null ? res.winner.name + " got there first in \"" + r.title + "\"." : res.message;
			}
			refresh();
		});
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
