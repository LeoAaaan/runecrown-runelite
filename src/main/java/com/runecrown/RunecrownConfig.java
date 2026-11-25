package com.runecrown;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(RunecrownConfig.GROUP)
public interface RunecrownConfig extends Config
{
	String GROUP = "runecrown";

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
		name = "Share valuable drops",
		description = "Post your valuable drops in your clan's feed and Discord (the clan picks how valuable), and count items for clan bingo",
		position = 4,
		warning = "This sends drops worth more than your clan's minimum, and items your clan's bingo board wants, to the Runecrown server (not run by Jagex or RuneLite): the item, its value, the boss, your kill count and your RuneScape name. Continue?"
	)
	default boolean shareDrops()
	{
		return false;
	}

	@ConfigItem(
		keyName = "sharePets",
		name = "Share pets",
		description = "Tell your clan when you get a pet",
		position = 5,
		warning = "When you get a pet, this sends the boss or activity, your kill count and your RuneScape name to the Runecrown server (not run by Jagex or RuneLite). Continue?"
	)
	default boolean sharePets()
	{
		return false;
	}

	@ConfigItem(
		keyName = "shareClog",
		name = "Share collection log slots",
		description = "Fill in your clan's collection log. Needs the game setting 'Collection log - New addition notification' set to chat message",
		position = 6,
		warning = "When you get a new collection log item, this sends the item name and your RuneScape name to the Runecrown server (not run by Jagex or RuneLite). Continue?"
	)
	default boolean shareClog()
	{
		return false;
	}

	@ConfigItem(
		keyName = "shareLevels",
		name = "Share 99s and max",
		description = "Tell your clan when you reach level 99 or max total",
		position = 7,
		warning = "When you reach 99 in a skill or max total level, this sends the skill and your RuneScape name to the Runecrown server (not run by Jagex or RuneLite). Lower levels are never sent. Continue?"
	)
	default boolean shareLevels()
	{
		return false;
	}

	@ConfigItem(
		keyName = "sharePbs",
		name = "Share personal bests",
		description = "Put your new personal best times on your clan's records page",
		position = 8,
		warning = "When the game says you set a new personal best, this sends the boss, the time, the team size, your kill count and your RuneScape name to the Runecrown server (not run by Jagex or RuneLite). Continue?"
	)
	default boolean sharePbs()
	{
		return false;
	}

	@ConfigItem(
		keyName = "server",
		name = "Server",
		description = "Leave as it is unless Runecrown tells you otherwise",
		position = 9
	)
	default String server()
	{
		return "https://guild-hub-bot.holmen-moe.workers.dev";
	}
}
