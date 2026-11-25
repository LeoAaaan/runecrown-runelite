package com.runecrown;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

// What GET /overlay/feed answers (read with Gson).
class Feed
{
	Guild guild;
	Me me;
	String hub;
	List<Event> events = new ArrayList<>();
	List<Group> groups = new ArrayList<>();
	List<Post> forum = new ArrayList<>();
	List<Race> races = new ArrayList<>();
	// Only for Old School RuneScape clans: what the clan wants shared.
	Osrs osrs;
	String at;
	String error;
	String message;
	// Answer to a drop report.
	Boolean won;
	Winner winner;

	static class Guild
	{
		String id;
		String name;
		String tag;
		String game;
		String gameName;
	}

	static class Me
	{
		String name;
	}

	static class Event
	{
		String id;
		String title;
		String type;
		String date;
		Integer size;
		boolean started;
		int signed;
		int tentative;
		String me;
		String link;

		Instant when()
		{
			try
			{
				return Instant.parse(date);
			}
			catch (Exception e)
			{
				return null;
			}
		}
	}

	static class Group
	{
		String id;
		String title;
		String when;
		String by;
	}

	static class Osrs
	{
		// Drops worth at least this many coins go to the clan feed; null = the clan has the drop feed off.
		Long minValue;
		boolean pets;
		boolean clog;
		boolean levels;
		boolean pbs;
		// Items wanted by the clan bingo boards you are on (sent whatever they are worth).
		List<Tile> bingo = new ArrayList<>();
	}

	static class Tile
	{
		String boss;
		List<String> items = new ArrayList<>();
	}

	// A drop race: the first member to get one of the items from the boss wins.
	static class Race
	{
		String id;
		String title;
		String boss;
		List<String> items = new ArrayList<>();
		String start;
		String end;
		Winner winner;

		boolean running()
		{
			try
			{
				Instant now = Instant.now();
				return winner == null && !now.isBefore(Instant.parse(start)) && now.isBefore(Instant.parse(end));
			}
			catch (Exception e)
			{
				return false;
			}
		}

		Instant ends()
		{
			try
			{
				return Instant.parse(end);
			}
			catch (Exception e)
			{
				return null;
			}
		}
	}

	static class Winner
	{
		String name;
		String item;
		Integer kc;
		Integer raceKills;
		String at;
		boolean me;
	}

	// Lower case letters and digits only, so "Vorkath's head" matches "Vorkath’s head".
	static String norm(String s)
	{
		return s == null ? "" : s.replaceAll("<[^>]*>", "").toLowerCase().replaceAll("[^a-z0-9]+", "");
	}

	static boolean sameBoss(String a, String b)
	{
		String x = norm(a), y = norm(b);
		return !x.isEmpty() && !y.isEmpty() && (x.contains(y) || y.contains(x));
	}

	static class Post
	{
		String id;
		String title;
		String cat;
		int replies;
	}

	// "in 2h 15m", "in 3d 4h", "started"
	static String until(Instant when)
	{
		if (when == null)
		{
			return "";
		}
		long m = Duration.between(Instant.now(), when).toMinutes();
		if (m <= 0)
		{
			return "started";
		}
		long d = m / 1440, h = (m % 1440) / 60, mm = m % 60;
		return "in " + (d > 0 ? d + "d " + h + "h" : h > 0 ? h + "h " + mm + "m" : mm + "m");
	}
}
