package com.lootfinder.ui;

import com.lootfinder.RarityFormat;
import com.lootfinder.data.DropRow;
import com.lootfinder.data.Rarity;
import java.awt.Color;
import java.awt.Font;
import java.util.Locale;
import javax.swing.JLabel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import okhttp3.HttpUrl;

/** Colours, fonts and text formatting shared by the panel's views. */
final class Styles
{
	// The same rarity colours the OSRS Wiki uses in its drop tables
	static final Color ALWAYS = new Color(0xAF, 0xEE, 0xEE);
	static final Color COMMON = new Color(0x56, 0xE1, 0x56);
	static final Color UNCOMMON = new Color(0xFF, 0xED, 0x4C);
	static final Color RARE = new Color(0xFF, 0x86, 0x3C);
	static final Color VERY_RARE = new Color(0xFF, 0x62, 0x62);

	static final Color ROW_BACKGROUND = ColorScheme.DARKER_GRAY_COLOR;
	static final Color ROW_HOVER = ColorScheme.DARKER_GRAY_HOVER_COLOR;
	static final Color MUTED_TEXT = ColorScheme.LIGHT_GRAY_COLOR;

	private static final HttpUrl WIKI = HttpUrl.get("https://oldschool.runescape.wiki");

	private Styles()
	{
	}

	static Color tierColor(Rarity.Tier tier)
	{
		switch (tier)
		{
			case ALWAYS:
				return ALWAYS;
			case COMMON:
				return COMMON;
			case UNCOMMON:
				return UNCOMMON;
			case RARE:
				return RARE;
			case VERY_RARE:
				return VERY_RARE;
			default:
				return MUTED_TEXT;
		}
	}

	static Font bodyFont()
	{
		return FontManager.getRunescapeSmallFont();
	}

	static Font titleFont()
	{
		return FontManager.getRunescapeBoldFont();
	}

	static JLabel label(String text, Font font, Color color)
	{
		JLabel label = new JLabel(text);
		label.setFont(font);
		label.setForeground(color);
		return label;
	}

	/** A single wiki rate, e.g. "1/512" or "0.195%". Word rates ("Rare") are shown as-is. */
	static String rate(Rarity rarity, RarityFormat format)
	{
		if (format == RarityFormat.PERCENT && rarity.isKnown())
		{
			return Rarity.formatPercent(rarity.getProbability());
		}
		return rarity.getText();
	}

	/** A computed per-kill chance. */
	static String rate(double probability, RarityFormat format)
	{
		return format == RarityFormat.PERCENT
			? Rarity.formatPercent(probability)
			: Rarity.formatOneIn(probability);
	}

	/** "Reward", "Thieving", ... for non-combat sources; empty for combat drops. */
	static String sourceType(DropRow row)
	{
		if (row.isCombat())
		{
			return "";
		}
		String type = row.getDropType();
		return type.substring(0, 1).toUpperCase(Locale.ROOT) + type.substring(1);
	}

	/** "Lvl 124" or "Lvl 49–68" for sources with several combat levels. */
	static String level(DropRow row)
	{
		String level = row.getLevel().trim();
		if (level.isEmpty())
		{
			return "";
		}
		String[] parts = level.split("\\s*,\\s*");
		if (parts.length == 1)
		{
			return "Lvl " + parts[0];
		}
		return "Lvl " + parts[0] + "–" + parts[parts.length - 1];
	}

	/** Tooltip lines explaining a single drop row. Returns null when there's nothing to add. */
	static String rowTooltip(DropRow row, RarityFormat format)
	{
		StringBuilder sb = new StringBuilder();
		if (row.getRolls() > 1)
		{
			line(sb, rate(row.getRarity(), format) + " per roll, " + row.getRolls() + " rolls per kill");
		}
		Rarity alt = row.getAltRarity();
		if (alt != null)
		{
			line(sb, "Alternative rate: " + rate(alt, format));
		}
		if (row.isRareDropTable())
		{
			line(sb, "From the rare drop table");
		}
		return sb.length() == 0 ? null : "<html>" + sb + "</html>";
	}

	static void line(StringBuilder sb, String text)
	{
		if (sb.length() > 0)
		{
			sb.append("<br>");
		}
		sb.append(escape(text));
	}

	static String escape(String s)
	{
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	static String wikiUrl(String page)
	{
		return WIKI.newBuilder()
			.addPathSegment("w")
			.addPathSegment(page.replace(' ', '_'))
			.build()
			.toString();
	}
}
