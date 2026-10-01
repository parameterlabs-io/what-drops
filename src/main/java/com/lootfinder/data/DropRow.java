package com.lootfinder.data;

import java.util.Locale;

/**
 * One line of a wiki drop table: an item, the page (monster, chest, activity) it comes from, and how
 * often. Plain fields so the row can be written to and read from the local cache with Gson.
 */
public final class DropRow
{
	public static final String TYPE_COMBAT = "combat";

	/** Wiki page the drop table lives on, e.g. "Abyssal demon". */
	private String page;
	/** Drop table variant on that page, e.g. "Wilderness Slayer Cave", or null if the page has one table. */
	private String variant;
	private String item;
	private String rarity;
	private String altRarity;
	private String quantity;
	private int quantityLow;
	private int quantityHigh;
	private int rolls;
	/** "combat", "reward", "thieving", "mining", ... */
	private String dropType;
	/** Combat level(s) of the source as written on the wiki, e.g. "124" or "49,64,68". */
	private String level;
	private boolean rareDropTable;

	private transient Rarity parsedRarity;
	private transient Rarity parsedAltRarity;

	/** Required by Gson. */
	DropRow()
	{
	}

	public DropRow(String page, String variant, String item, String rarity, String altRarity, String quantity,
		int quantityLow, int quantityHigh, int rolls, String dropType, String level, boolean rareDropTable)
	{
		this.page = page;
		this.variant = variant;
		this.item = item;
		this.rarity = rarity;
		this.altRarity = altRarity;
		this.quantity = quantity;
		this.quantityLow = quantityLow;
		this.quantityHigh = quantityHigh;
		this.rolls = rolls;
		this.dropType = dropType;
		this.level = level;
		this.rareDropTable = rareDropTable;
	}

	public String getPage()
	{
		return page;
	}

	public String getVariant()
	{
		return variant;
	}

	public String getItem()
	{
		return item;
	}

	public String getQuantity()
	{
		return quantity == null || quantity.isEmpty() ? "1" : quantity;
	}

	public int getQuantityLow()
	{
		return quantityLow;
	}

	public int getQuantityHigh()
	{
		return quantityHigh;
	}

	public boolean isNoted()
	{
		return quantity != null && quantity.toLowerCase(Locale.ROOT).contains("noted");
	}

	/** Number of times the table is rolled per kill; at least 1. */
	public int getRolls()
	{
		return Math.max(1, rolls);
	}

	public String getDropType()
	{
		return dropType == null || dropType.isEmpty() ? "other" : dropType;
	}

	public boolean isCombat()
	{
		return TYPE_COMBAT.equalsIgnoreCase(dropType);
	}

	public String getLevel()
	{
		return level == null ? "" : level;
	}

	public boolean isRareDropTable()
	{
		return rareDropTable;
	}

	public Rarity getRarity()
	{
		if (parsedRarity == null)
		{
			parsedRarity = Rarity.parse(rarity);
		}
		return parsedRarity;
	}

	/** The wiki's alternative rate (e.g. with a ring of wealth or on task), or null if there is none. */
	public Rarity getAltRarity()
	{
		if (altRarity == null || altRarity.trim().isEmpty())
		{
			return null;
		}
		if (parsedAltRarity == null)
		{
			parsedAltRarity = Rarity.parse(altRarity);
		}
		return parsedAltRarity;
	}

	/** Chance of getting this line at least once in a kill, accounting for multiple rolls. NaN if unknown. */
	public double getPerKillChance()
	{
		double p = getRarity().getProbability();
		if (Double.isNaN(p))
		{
			return p;
		}
		int n = getRolls();
		return n == 1 ? p : 1 - Math.pow(1 - p, n);
	}

	/** Identifies the exact drop table this row belongs to, e.g. "Abyssal demon#Standard". */
	public String getSourceKey()
	{
		return variant == null ? page : page + "#" + variant;
	}

	@Override
	public String toString()
	{
		return item + " from " + getSourceKey() + " (" + rarity + ")";
	}
}
