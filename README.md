# Runecrown for RuneLite

Shows your clan's next events from [Runecrown](https://runecrown.com) in a RuneLite side panel, with
Going / Maybe / Can't buttons, open groups and new forum posts. Optionally shows the next event and a
countdown on screen.

## Clan drop races

Your clan's event hosts can start a drop race in the hub: a boss and the item(s) that win. Switch on
**Clan drop races** in the plugin's settings to take part. When one of those items drops for you while the
race runs, the plugin tells Runecrown, the first one wins, and the winner is posted in your clan's Discord
with the number of kills it took. It is off until you switch it on.

## Clan feed and bingo

Five more switches, all off until you turn them on, each for one kind of news:

- **Share valuable drops**: drops worth more than your clan's minimum (Management picks it, 1m by default),
  and any item your clan's bingo board wants. Bingo tiles tick themselves.
- **Share pets**: the "funny feeling" message, with the boss you just killed.
- **Share collection log slots**: needs the game setting *Collection log - New addition notification* on.
- **Share 99s and max**: only level 99 and max total, never lower levels.
- **Share personal bests**: when the game says "(new personal best)", the time goes on your clan's records page.

Your clan sees them on the hub's **Drops and pets feed**, **Clan bingo**, **Clan collection log** and
**Hall of fame** pages, and the bot posts them in Discord. This replaces separate webhook plugins.

## Setup

1. In the Runecrown hub, open your clan, then **Settings › Overlay and add-ons › Make a code**.
2. In RuneLite, open the plugin's settings (wrench icon › Runecrown) and paste the code.
3. Click the gold star in the RuneLite side bar.

## What it sends

Only your own overlay code, to `guild-hub-bot.holmen-moe.workers.dev` (the Runecrown server), once a
minute while the panel or the on-screen box is shown, plus your answer when you press Going / Maybe / Can't.
With **Clan drop races** switched on (off by default), and only while your clan runs a drop race, it also sends
a drop that matches the race: the item's name, the boss, your kill count for that boss, how many kills the
plugin counted since the race started, and your RuneScape name.
Each clan feed switch sends only its own kind (see above) with your RuneScape name and, where it fits, the
boss and your kill count. Nothing else from the game is sent, and nothing at all for clans on other games.
The code can be removed at any time in the hub, which stops it working at once.
