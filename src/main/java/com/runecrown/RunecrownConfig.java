package com.runecrown;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(RunecrownConfig.GROUP)
public interface RunecrownConfig extends Config
{
	String GROUP = "runecrown";

	@ConfigSection(
		name = "Share with your clan",
		description = "Each switch sends one kind of news to your clan's feed and Discord. All off until you turn them on.",
		position = 10
	)
	String shareSection = "share";

	@ConfigSection(
		name = "Clan list, events and chat",
		description = "Ranks, attendance and the clan chat bridge. All off until you turn them on.",
		position = 30
	)
	String clanSection = "clan";

	@ConfigItem(
		keyName = "code",
		name = "Overlay code",
		description = "Your own code from the Runecrown hub: Settings > Overlay and add-ons > Make a code. It starts with ro_",
		secret = true,
		position = 1,
		warning = "This plugin sends your overlay code to the Runecrown server (not run by Jagex or RuneLite) to read your clan's events. Nothing from the game is sent. Continue?"
	)
	default String code()
	{
		return "";
	}

	@ConfigItem(
		keyName = "showOnScreen",
		name = "Next event on screen",
		description = "Show the next event and a countdown in a small box on the game screen",
		position = 2
	)
	default boolean showOnScreen()
	{
		return true;
	}

	@ConfigItem(
		keyName = "dropRaces",
		name = "Clan drop races",
		description = "Take part in your clan's drop races: when a drop matches a race that is running, the plugin tells Runecrown so the winner is announced in your clan's Discord",
		position = 3,
		warning = "When your clan runs a drop race, this sends the matching drop's item name, the boss, your kill count and your RuneScape name to the Runecrown server (not run by Jagex or RuneLite). No other drops are sent. Continue?"
	)
	default boolean dropRaces()
	{
		return false;
	}

	@ConfigItem(
		keyName = "shareDrops",
		name = "Valuable drops",
		description = "Post your valuable drops in your clan's feed and Discord (the clan picks how valuable), and count items for clan bingo",
		position = 11,
		section = shareSection,
		warning = "This sends drops worth more than your clan's minimum, and items your clan's bingo board wants, to the Runecrown server (not run by Jagex or RuneLite): the item, its value, the boss, your kill count and your RuneScape name. Continue?"
	)
	default boolean shareDrops()
	{
		return false;
	}

	@ConfigItem(
		keyName = "sharePets",
		name = "Pets",
		description = "Tell your clan when you get a pet",
		position = 12,
		section = shareSection,
		warning = "When you get a pet, this sends the boss or activity, your kill count and your RuneScape name to the Runecrown server (not run by Jagex or RuneLite). Continue?"
	)
	default boolean sharePets()
	{
		return false;
	}

	@ConfigItem(
		keyName = "shareClog",
		name = "Collection log slots",
		description = "Fill in your clan's collection log. Needs the game setting 'Collection log - New addition notification' set to chat message",
		position = 13,
		section = shareSection,
		warning = "When you get a new collection log item, this sends the item name and your RuneScape name to the Runecrown server (not run by Jagex or RuneLite). Continue?"
	)
	default boolean shareClog()
	{
		return false;
	}

	@ConfigItem(
		keyName = "shareLevels",
		name = "99s and max",
		description = "Tell your clan when you reach level 99 or max total",
		position = 14,
		section = shareSection,
		warning = "When you reach 99 in a skill or max total level, this sends the skill and your RuneScape name to the Runecrown server (not run by Jagex or RuneLite). Lower levels are never sent. Continue?"
	)
	default boolean shareLevels()
	{
		return false;
	}

	@ConfigItem(
		keyName = "sharePbs",
		name = "Personal bests",
		description = "Put your new personal best times on your clan's records page",
		position = 15,
		section = shareSection,
		warning = "When the game says you set a new personal best, this sends the boss, the time, the team size, your kill count and your RuneScape name to the Runecrown server (not run by Jagex or RuneLite). Continue?"
	)
	default boolean sharePbs()
	{
		return false;
	}

	@ConfigItem(
		keyName = "shareRaids",
		name = "Raid completions",
		description = "Chambers of Xeric, Theatre of Blood and Tombs of Amascut: the raid, your kill count, team size and time",
		position = 16,
		section = shareSection,
		warning = "When you finish a raid, this sends the raid, your kill count, the team size, the time and your RuneScape name to the Runecrown server (not run by Jagex or RuneLite). Continue?"
	)
	default boolean shareRaids()
	{
		return false;
	}

	@ConfigItem(
		keyName = "shareClues",
		name = "Clue scrolls",
		description = "Each finished clue: the tier, how many you have done and what the casket was worth (big ones are posted in Discord)",
		position = 17,
		section = shareSection,
		warning = "When you finish a clue scroll, this sends the tier, your clue count, the casket's value and your RuneScape name to the Runecrown server (not run by Jagex or RuneLite). Continue?"
	)
	default boolean shareClues()
	{
		return false;
	}

	@ConfigItem(
		keyName = "shareTasks",
		name = "Combat tasks and diaries",
		description = "Combat achievement tasks and finished achievement diary tiers. Turn off the game setting 'Combat Achievement Tasks - Repeat completion' to avoid repeats",
		position = 18,
		section = shareSection,
		warning = "When you finish a combat task or an achievement diary tier, this sends its name, tier and your RuneScape name to the Runecrown server (not run by Jagex or RuneLite). Continue?"
	)
	default boolean shareTasks()
	{
		return false;
	}

	@ConfigItem(
		keyName = "shareQuests",
		name = "Quests",
		description = "Each quest you finish (your clan posts the big ones in Discord)",
		position = 19,
		section = shareSection,
		warning = "When you finish a quest, this sends the quest's name and your RuneScape name to the Runecrown server (not run by Jagex or RuneLite). Continue?"
	)
	default boolean shareQuests()
	{
		return false;
	}

	@ConfigItem(
		keyName = "shareDeaths",
		name = "Deaths",
		description = "Counts in the clan's feed. Only a hardcore ironman losing their status is posted in Discord",
		position = 20,
		section = shareSection,
		warning = "When you die, this sends whether you are a hardcore ironman, what killed you (if the game says) and your RuneScape name to the Runecrown server (not run by Jagex or RuneLite). Continue?"
	)
	default boolean shareDeaths()
	{
		return false;
	}

	@ConfigItem(
		keyName = "shareClan",
		name = "Share the clan list",
		description = "Keeps your clan's ranks and who is online in the hub (rank-ups, who left, last seen) and ticks attendance for clan events. Officers usually turn this on",
		position = 31,
		section = clanSection,
		warning = "Every 6 hours, and every 5 minutes while a clan event is on, this sends your clan's member list as the game shows it (names, rank titles, and the world of members online) to the Runecrown server (not run by Jagex or RuneLite). Continue?"
	)
	default boolean shareClan()
	{
		return false;
	}

	@ConfigItem(
		keyName = "checkIn",
		name = "Check me in at events",
		description = "While one of your clan's events is on, tick your own attendance (and your world, for events on one world)",
		position = 32,
		section = clanSection,
		warning = "While a clan event is on, this sends your world and your RuneScape name to the Runecrown server (not run by Jagex or RuneLite) every 5 minutes. Continue?"
	)
	default boolean checkIn()
	{
		return false;
	}

	@ConfigItem(
		keyName = "bridgeChat",
		name = "Clan chat to Discord",
		description = "Send your clan chat to the clan's Discord channel (Management picks it in the hub). One member's plugin sends at a time",
		position = 33,
		section = clanSection,
		warning = "This sends every message in your clan chat (who wrote it and what) to the Runecrown server (not run by Jagex or RuneLite), which posts it in your clan's Discord. Only turn it on if your clan agreed to it. Continue?"
	)
	default boolean bridgeChat()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showDiscord",
		name = "Discord messages in game",
		description = "Show messages from the clan's Discord channel in your chat box (only you see them, in the Clan tab)",
		position = 34,
		section = clanSection
	)
	default boolean showDiscord()
	{
		return false;
	}

	@ConfigItem(
		keyName = "server",
		name = "Server",
		description = "Leave as it is unless Runecrown tells you otherwise",
		position = 50
	)
	default String server()
	{
		return "https://guild-hub-bot.holmen-moe.workers.dev";
	}
}
