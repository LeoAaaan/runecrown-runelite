package com.runecrown;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.function.BiConsumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;

// The side panel: guild name, next events with Going / Maybe / Can't, open groups, new forum posts, Open the hub.
class RunecrownPanel extends PluginPanel
{
	private static final Color GOLD = new Color(230, 185, 85);
	private static final Color GREEN = new Color(79, 191, 122);
	private static final Color AMBER = new Color(224, 164, 58);
	private static final Color RED = new Color(224, 99, 99);
	private static final DateTimeFormatter WHEN = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault());

	private final JPanel body = new JPanel();
	private final Runnable refresh;
	private final BiConsumer<String, String> answer;

	RunecrownPanel(Runnable refresh, BiConsumer<String, String> answer)
	{
		super(false);
		this.refresh = refresh;
		this.answer = answer;
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(8, 8, 8, 8));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setBackground(ColorScheme.DARK_GRAY_COLOR);
		add(body, BorderLayout.NORTH);
		show(null, true);
	}

	void show(Feed f, boolean noCode)
	{
		body.removeAll();
		if (noCode)
		{
			title("Runecrown", null);
			text("Paste your own overlay code in this plugin's settings (wrench icon › Runecrown).", ColorScheme.LIGHT_GRAY_COLOR);
			text("In the hub: Settings › Overlay and add-ons › Make a code.", ColorScheme.LIGHT_GRAY_COLOR);
			link("Open the hub", "https://app.runecrown.com");
		}
		else if (f == null)
		{
			title("Runecrown", null);
			text("Loading…", ColorScheme.LIGHT_GRAY_COLOR);
		}
		else if (f.guild == null)
		{
			title("Runecrown", null);
			text(f.message != null ? f.message : "Something went wrong.", RED);
			button("Try again", refresh);
		}
		else
		{
			title(f.guild.name + (f.guild.tag != null && !f.guild.tag.isEmpty() ? " [" + f.guild.tag + "]" : ""), f.guild.gameName + (f.me != null ? " · " + f.me.name : ""));
			if (f.message != null)
			{
				text(f.message, RED);
			}
			heading("Next events");
			if (f.events.isEmpty())
			{
				text("Nothing planned yet.", ColorScheme.LIGHT_GRAY_COLOR);
			}
			for (Feed.Event e : f.events)
			{
				event(e);
			}
			heading("Open groups");
			if (f.groups.isEmpty())
			{
				text("No open groups.", ColorScheme.LIGHT_GRAY_COLOR);
			}
			for (Feed.Group g : f.groups)
			{
				text(g.title + (g.by != null && !g.by.isEmpty() ? " · " + g.by : ""), Color.WHITE);
			}
			heading("New on the forum");
			if (f.forum.isEmpty())
			{
				text("No posts yet.", ColorScheme.LIGHT_GRAY_COLOR);
			}
			for (Feed.Post p : f.forum)
			{
				text(p.title + "  (" + p.replies + ")", Color.WHITE);
			}
			body.add(Box.createVerticalStrut(8));
			link("Open the hub", f.hub != null && !f.hub.isEmpty() ? f.hub : "https://app.runecrown.com");
			button("Refresh", refresh);
		}
		body.revalidate();
		body.repaint();
	}

	private void event(Feed.Event e)
	{
		JPanel card = new JPanel();
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
		card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		card.setBorder(new EmptyBorder(6, 6, 6, 6));
		card.setAlignmentX(LEFT_ALIGNMENT);
		JLabel t = new JLabel("<html>" + esc(e.title) + "</html>");
		t.setFont(FontManager.getRunescapeBoldFont());
		t.setForeground(Color.WHITE);
		card.add(t);
		JLabel when = new JLabel(Feed.until(e.when()) + (e.when() != null ? " · " + WHEN.format(e.when()) : ""));
		when.setForeground(GOLD);
		when.setFont(FontManager.getRunescapeSmallFont());
		card.add(when);
		JLabel counts = new JLabel(e.type + " · " + e.signed + (e.size != null ? "/" + e.size : "") + " going" + (e.tentative > 0 ? ", " + e.tentative + " maybe" : ""));
		counts.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		counts.setFont(FontManager.getRunescapeSmallFont());
		card.add(counts);
		String mine = e.me == null ? "You have not answered yet" : "You: " + ("signed".equals(e.me) || "late".equals(e.me) ? "Going" : "tentative".equals(e.me) ? "Maybe" : "Can't");
		JLabel me = new JLabel(mine);
		me.setForeground(e.me == null ? ColorScheme.LIGHT_GRAY_COLOR : "absent".equals(e.me) ? RED : "tentative".equals(e.me) ? AMBER : GREEN);
		me.setFont(FontManager.getRunescapeSmallFont());
		card.add(me);
		if (!e.started)
		{
			JPanel row = new JPanel(new GridLayout(1, 3, 4, 0));
			row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			row.setAlignmentX(LEFT_ALIGNMENT);
			row.add(answerButton(e, "signed", "Going", GREEN));
			row.add(answerButton(e, "tentative", "Maybe", AMBER));
			row.add(answerButton(e, "absent", "Can't", RED));
			card.add(Box.createVerticalStrut(4));
			card.add(row);
		}
		body.add(card);
		body.add(Box.createVerticalStrut(6));
	}

	private JButton answerButton(Feed.Event e, String status, String label, Color on)
	{
		boolean picked = status.equals(e.me) || ("signed".equals(status) && "late".equals(e.me));
		JButton b = new JButton(label);
		b.setFocusPainted(false);
		b.setFont(FontManager.getRunescapeSmallFont());
		if (picked)
		{
			b.setBorder(BorderFactory.createLineBorder(on));
			b.setForeground(on);
		}
		// Pressing your current answer again takes it back.
		b.addActionListener(a -> answer.accept(e.id, picked ? "withdrawn" : status));
		return b;
	}

	private void title(String name, String sub)
	{
		JLabel l = new JLabel("<html>" + esc(name) + "</html>");
		l.setFont(FontManager.getRunescapeBoldFont());
		l.setForeground(GOLD);
		l.setAlignmentX(LEFT_ALIGNMENT);
		body.add(l);
		if (sub != null)
		{
			text(sub, ColorScheme.LIGHT_GRAY_COLOR);
		}
		body.add(Box.createVerticalStrut(4));
	}

	private void heading(String s)
	{
		body.add(Box.createVerticalStrut(8));
		JLabel l = new JLabel(s.toUpperCase());
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		l.setAlignmentX(LEFT_ALIGNMENT);
		body.add(l);
		body.add(Box.createVerticalStrut(4));
	}

	private void text(String s, Color c)
	{
		JLabel l = new JLabel("<html>" + esc(s) + "</html>");
		l.setForeground(c);
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setAlignmentX(LEFT_ALIGNMENT);
		l.setMaximumSize(new Dimension(PANEL_WIDTH, Integer.MAX_VALUE));
		body.add(l);
		body.add(Box.createVerticalStrut(2));
	}

	private void link(String label, String url)
	{
		button(label, () -> LinkBrowser.browse(url));
	}

	private void button(String label, Runnable run)
	{
		JButton b = new JButton(label);
		b.setFocusPainted(false);
		b.setAlignmentX(LEFT_ALIGNMENT);
		b.addActionListener(a -> run.run());
		body.add(b);
		body.add(Box.createVerticalStrut(4));
	}

	private static String esc(String s)
	{
		return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
