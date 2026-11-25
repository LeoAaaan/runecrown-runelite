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
