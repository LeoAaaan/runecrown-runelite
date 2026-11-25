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
	description = "Your clan's events, drop races, bingo, drops and pets feed, collection log and personal bests on Runecrown",
	tags = {"clan", "events", "calendar", "signup", "guild", "bingo", "drops", "pets", "collection log", "personal best", "discord"}
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
	// The boss of the last kill count message, and when it came (for pets and personal bests).
	private String lastBoss;
	private long lastBossAt;
	// A personal best seen before its kill count message (raids say the time first).
	private Long pendingPbMs;
	private Integer pendingPbTeam;
	private long pendingPbAt;

	private static final Pattern PET = Pattern.compile("^(?:You have a funny feeling like you(?:'re being| would have been) followed|You feel something weird sneaking into your backpack)");
	private static final Pattern CLOG = Pattern.compile("^New item added to your collection log: (.+?)\\.?$");
	private static final Pattern LEVEL = Pattern.compile("^Congratulations, you've just advanced your (.+?) level\\. You are now level (\\d+)");
	private static final Pattern TOTAL = Pattern.compile("^Congratulations, you've reached a total level of (\\d+)");
	private static final Pattern PB = Pattern.compile("([0-9]+(?::[0-9]{2}){1,2}(?:\\.[0-9]{1,2})?) \\(new personal best\\)");
	private static final Pattern TEAM = Pattern.compile("Team size: (?:(\\d+) player|(Solo))", Pattern.CASE_INSENSITIVE);

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
		if (RunecrownConfig.GROUP.equals(e.getGroup()) && ("code".equals(e.getKey()) || "dropRaces".equals(e.getKey()) || e.getKey().startsWith("share")))
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
		String msg = Text.removeTags(e.getMessage());
		clanChat(msg);
		Matcher m = KC.matcher(msg);
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
		lastBoss = boss;
		lastBossAt = System.currentTimeMillis();
		if (pendingPbMs != null && lastBossAt - pendingPbAt < 15000)
		{
			sendPb(boss, pendingPbMs, pendingPbTeam);
		}
		pendingPbMs = null;
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

	/* ---------------- Clan feed ----------------
	   Each kind has its own switch (all off at first). Nothing is sent for clans on other games. */

	private Feed.Osrs osrsPart()
	{
		Feed f = feed;
		return f == null || !RunecrownClient.CODE.matcher(code()).matches() ? null : f.osrs;
	}

	private String recentBoss()
	{
		return lastBoss != null && System.currentTimeMillis() - lastBossAt < 60000 ? lastBoss : null;
	}

	private void clanChat(String msg)
	{
		Feed.Osrs o = osrsPart();
		if (o == null)
		{
			return;
		}
		Matcher m;
		if (config.sharePets() && o.pets && PET.matcher(msg).find())
		{
			String boss = recentBoss();
			Map<String, Object> b = body("pet");
			b.put("source", boss);
			b.put("kc", boss == null ? null : lastKc.get(Feed.norm(boss)));
			send(b);
			return;
		}
		if (config.shareClog() && o.clog && (m = CLOG.matcher(msg)).find())
		{
			Map<String, Object> b = body("clog");
			b.put("item", m.group(1).trim());
			send(b);
			return;
		}
		if (config.shareLevels() && o.levels)
		{
			if ((m = LEVEL.matcher(msg)).find() && "99".equals(m.group(2)))
			{
				Map<String, Object> b = body("level");
				b.put("skill", m.group(1));
				b.put("level", 99);
				send(b);
				return;
			}
			if ((m = TOTAL.matcher(msg)).find() && Integer.parseInt(m.group(1)) >= 2277)
			{
				Map<String, Object> b = body("level");
				b.put("skill", "Total");
				b.put("level", Integer.parseInt(m.group(1)));
				send(b);
				return;
			}
		}
		if (config.sharePbs() && o.pbs && (m = PB.matcher(msg)).find())
		{
			long ms = parseTime(m.group(1));
			Integer team = null;
			Matcher t = TEAM.matcher(msg);
			if (t.find())
			{
				team = t.group(2) != null ? 1 : Integer.valueOf(t.group(1));
			}
			// Bosses say the kill count first, raids say it after the time.
			String boss = lastBoss != null && System.currentTimeMillis() - lastBossAt < 3000 ? lastBoss : null;
			if (ms > 0 && boss != null)
			{
				sendPb(boss, ms, team);
			}
			else if (ms > 0)
			{
				pendingPbMs = ms;
				pendingPbTeam = team;
				pendingPbAt = System.currentTimeMillis();
			}
		}
	}

	// "1:19.80" -> 79800, "1:02:03" -> 3723000
	static long parseTime(String s)
	{
		try
		{
			String[] parts = s.split(":");
			double secs = 0;
			for (String p : parts)
			{
				secs = secs * 60 + Double.parseDouble(p);
			}
			return Math.round(secs * 1000);
		}
		catch (NumberFormatException e)
		{
			return 0;
		}
	}

	private void sendPb(String boss, long ms, Integer team)
	{
		Map<String, Object> b = body("pb");
		b.put("boss", boss);
		b.put("ms", ms);
		b.put("team", team);
		b.put("kc", lastKc.get(Feed.norm(boss)));
		send(b);
	}

	private Map<String, Object> body(String kind)
	{
		Map<String, Object> b = new HashMap<>();
		b.put("kind", kind);
		Player p = gameClient.getLocalPlayer();
		b.put("rsn", p == null ? null : p.getName());
		return b;
	}

	private void send(Map<String, Object> b)
	{
		client.osrs(config.server(), code(), b, res -> {
			if (res.error != null && feed != null)
			{
				feed.message = res.message;
				paint(feed, false);
			}
		});
	}

	// A drop worth sharing: above the clan's minimum, or an item one of your bingo boards wants.
	private void clanDrops(String source, Collection<ItemStack> items)
	{
		Feed.Osrs o = osrsPart();
		if (o == null || !config.shareDrops() || items == null)
		{
			return;
		}
		Map<String, int[]> seen = new HashMap<>();
		Map<String, Integer> ids = new HashMap<>();
		for (ItemStack it : items)
		{
			String name = itemManager.getItemComposition(it.getId()).getName();
			int[] q = seen.computeIfAbsent(name, k -> new int[1]);
			q[0] += it.getQuantity();
			ids.put(name, it.getId());
		}
		for (Map.Entry<String, int[]> en : seen.entrySet())
		{
			String name = en.getKey();
			int qty = en.getValue()[0];
			long value = (long) itemManager.getItemPrice(ids.get(name)) * qty;
			boolean bingo = false;
			for (Feed.Tile t : o.bingo)
			{
				if (t.boss != null && !t.boss.isEmpty() && !Feed.sameBoss(source, t.boss))
				{
					continue;
				}
				for (String want : t.items)
				{
					if (Feed.norm(want).equals(Feed.norm(name)))
					{
						bingo = true;
					}
				}
			}
			if (bingo || (o.minValue != null && value >= o.minValue && value > 0))
			{
				Map<String, Object> b = body("drop");
				b.put("item", name);
				b.put("qty", qty);
				b.put("value", value);
				b.put("source", source);
				b.put("kc", lastKc.get(Feed.norm(source)));
				send(b);
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
		clanDrops(source, items);
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
