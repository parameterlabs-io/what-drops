package com.lootfinder.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything one drop table gives of a single item. Usually one row, but some tables list an item
 * more than once (e.g. Mithril dragon drops Coins at 17/128 and at 7/128 in different amounts).
 */
public final class SourceSummary
{
	private final String page;
	private final String variant;
	private final List<DropRow> rows;

	SourceSummary(String page, String variant, List<DropRow> rows)
	{
		this.page = page;
		this.variant = variant;
		this.rows = Collections.unmodifiableList(rows);
	}

	public String getPage()
	{
		return page;
	}

	public String getVariant()
	{
		return variant;
	}

	public List<DropRow> getRows()
	{
		return rows;
	}

	public DropRow getFirst()
	{
		return rows.get(0);
	}

	/** True when the rate shown should be computed rather than copied from the wiki. */
	public boolean isCombined()
	{
		return rows.size() > 1 || rows.get(0).getRolls() > 1;
	}

	/**
	 * Chance of getting the item at least once per kill (or per chest, per game, ...) across every
	 * line of this table that has a numeric rate. NaN if no line has one.
	 *
	 * <p>Lines of one table are alternatives on the same roll (a kill gives 17/128 Coins <em>or</em>
	 * 7/128 Coins, not both), so their rates add up per roll before the number of rolls is applied.
	 */
	public double getPerKillChance()
	{
		Map<Integer, Double> perRollByRolls = new HashMap<>();
		for (DropRow row : rows)
		{
			double p = row.getRarity().getProbability();
			if (!Double.isNaN(p))
			{
				perRollByRolls.merge(row.getRolls(), p, Double::sum);
			}
		}
		if (perRollByRolls.isEmpty())
		{
			return Double.NaN;
		}

		double missAll = 1.0;
		for (Map.Entry<Integer, Double> e : perRollByRolls.entrySet())
		{
			double perRoll = Math.min(1.0, e.getValue());
			missAll *= Math.pow(1 - perRoll, e.getKey());
		}
		return 1 - missAll;
	}

	public double sortKey()
	{
		double p = getPerKillChance();
		if (!Double.isNaN(p))
		{
			return p;
		}
		double best = -1;
		for (DropRow row : rows)
		{
			best = Math.max(best, row.getRarity().sortKey());
		}
		return best;
	}

	public Rarity.Tier getTier()
	{
		double p = getPerKillChance();
		if (!Double.isNaN(p))
		{
			return Rarity.tierFor(p);
		}
		return rows.get(0).getRarity().getTier();
	}

	public boolean isCombat()
	{
		for (DropRow row : rows)
		{
			if (row.isCombat())
			{
				return true;
			}
		}
		return false;
	}

	/** "Abyssal demon" or "Abyssal demon (Wilderness Slayer Cave)". */
	public String getLabel()
	{
		return variant == null ? page : page + " (" + variant + ")";
	}
}
