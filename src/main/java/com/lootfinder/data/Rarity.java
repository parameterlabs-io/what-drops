package com.lootfinder.data;

import java.math.BigDecimal;
import java.math.MathContext;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A drop rate as written on the OSRS Wiki, e.g. "1/512", "4/128", "1/5,000", "1/390.1",
 * "Always", "Common" or "Unknown".
 */
public final class Rarity
{
	public enum Tier
	{
		ALWAYS("Always"),
		COMMON("Common"),
		UNCOMMON("Uncommon"),
		RARE("Rare"),
		VERY_RARE("Very rare"),
		OTHER("Other");

		private final String label;

		Tier(String label)
		{
			this.label = label;
		}

		public String getLabel()
		{
			return label;
		}
	}

	private static final Pattern FRACTION = Pattern.compile(
		"^~?\\s*([0-9][0-9,]*(?:\\.[0-9]+)?)\\s*/\\s*([0-9][0-9,]*(?:\\.[0-9]+)?)$");

	private static final Rarity UNKNOWN = new Rarity("Unknown", Double.NaN, Tier.OTHER);

	private final String text;
	private final double probability;
	private final Tier tier;

	private Rarity(String text, double probability, Tier tier)
	{
		this.text = text;
		this.probability = probability;
		this.tier = tier;
	}

	public static Rarity parse(String raw)
	{
		if (raw == null || raw.trim().isEmpty())
		{
			return UNKNOWN;
		}

		String text = raw.trim();
		Matcher m = FRACTION.matcher(text);
		if (m.matches())
		{
			double numerator = parseNumber(m.group(1));
			double denominator = parseNumber(m.group(2));
			if (denominator > 0 && numerator >= 0)
			{
				double p = Math.min(1.0, numerator / denominator);
				return new Rarity(text, p, tierFor(p));
			}
			return new Rarity(text, Double.NaN, Tier.OTHER);
		}

		switch (text.toLowerCase(Locale.ROOT))
		{
			case "always":
				return new Rarity(text, 1.0, Tier.ALWAYS);
			case "common":
				return new Rarity(text, Double.NaN, Tier.COMMON);
			case "uncommon":
				return new Rarity(text, Double.NaN, Tier.UNCOMMON);
			case "rare":
				return new Rarity(text, Double.NaN, Tier.RARE);
			case "very rare":
				return new Rarity(text, Double.NaN, Tier.VERY_RARE);
			default:
				// Unknown, Varies, Once, Never, ...
				return new Rarity(text, Double.NaN, Tier.OTHER);
		}
	}

	/** Same bands the wiki's Module:DropsLine uses to colour its drop tables. */
	static Tier tierFor(double p)
	{
		if (p >= 1.0)
		{
			return Tier.ALWAYS;
		}
		if (p >= 1.0 / 25)
		{
			return Tier.COMMON;
		}
		if (p >= 1.0 / 99.99)
		{
			return Tier.UNCOMMON;
		}
		if (p >= 1.0 / 999.99)
		{
			return Tier.RARE;
		}
		return Tier.VERY_RARE;
	}

	private static double parseNumber(String s)
	{
		return Double.parseDouble(s.replace(",", ""));
	}

	/** The rate exactly as the wiki writes it. */
	public String getText()
	{
		return text;
	}

	/** Chance per roll in [0, 1], or NaN when the wiki gives no numeric rate. */
	public double getProbability()
	{
		return probability;
	}

	public boolean isKnown()
	{
		return !Double.isNaN(probability);
	}

	public Tier getTier()
	{
		return tier;
	}

	/**
	 * A value for ordering drops from most to least likely. Numeric rates use their probability;
	 * word-only rates ("Rare") get a representative value for their tier so they sort sensibly.
	 * Never shown to the user.
	 */
	public double sortKey()
	{
		if (isKnown())
		{
			return probability;
		}
		return tierSortKey(tier);
	}

	/** The values the wiki itself uses to sort word-only rates. */
	static double tierSortKey(Tier tier)
	{
		switch (tier)
		{
			case ALWAYS:
				return 1.0;
			case COMMON:
				return 1.0 / 16;
			case UNCOMMON:
				return 1.0 / 64;
			case RARE:
				return 1.0 / 128;
			case VERY_RARE:
				return 1.0 / 1024;
			default:
				return -1;
		}
	}

	/** Formats a probability as "1/x", e.g. 1/512, 1/32.5, 1/5,000. */
	public static String formatOneIn(double p)
	{
		if (Double.isNaN(p) || p <= 0)
		{
			return "Unknown";
		}
		if (p >= 1.0)
		{
			return "Always";
		}
		double x = 1.0 / p;
		DecimalFormat format = x >= 100
			? new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US))
			: new DecimalFormat("#,##0.#", DecimalFormatSymbols.getInstance(Locale.US));
		return "1/" + format.format(x);
	}

	/** Formats a probability as a percentage with three significant figures, e.g. 0.195%. */
	public static String formatPercent(double p)
	{
		if (Double.isNaN(p) || p < 0)
		{
			return "Unknown";
		}
		if (p >= 1.0)
		{
			return "100%";
		}
		if (p == 0)
		{
			return "0%";
		}
		BigDecimal pct = new BigDecimal(p * 100).round(new MathContext(3));
		return pct.stripTrailingZeros().toPlainString() + "%";
	}

	@Override
	public String toString()
	{
		return text;
	}
}
