package com.whatdrops.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Lookups over the full set of drop rows, in both directions:
 * item → every table that drops it, and page → every drop on that page.
 * Immutable once built, so it can be shared between threads.
 */
public final class DropIndex
{
	private final long fetchedAt;
	private final int rowCount;

	private final Map<String, List<DropRow>> byItem = new HashMap<>();
	private final Map<String, List<DropRow>> byPage = new HashMap<>();
	private final Map<String, String> itemDisplayNames = new HashMap<>();
	private final Map<String, String> pageDisplayNames = new HashMap<>();
	/** Same names keyed the way the typeahead matches (no apostrophes or periods), for loose lookups. */
	private final Map<String, String> itemLooseNames = new HashMap<>();
	private final Map<String, String> pageLooseNames = new HashMap<>();
	private final Set<String> itemsWithCombatSource = new HashSet<>();
	private final Set<String> pagesWithCombatDrops = new HashSet<>();

	private final NameSearch itemSearch;
	private final NameSearch pageSearch;

	public DropIndex(DropDataset dataset)
	{
		this.fetchedAt = dataset.getFetchedAt();
		List<DropRow> rows = dataset.getRows() == null ? Collections.emptyList() : dataset.getRows();
		this.rowCount = rows.size();

		for (DropRow row : rows)
		{
			String itemKey = key(row.getItem());
			String pageKey = key(row.getPage());

			byItem.computeIfAbsent(itemKey, k -> new ArrayList<>()).add(row);
			byPage.computeIfAbsent(pageKey, k -> new ArrayList<>()).add(row);
			itemDisplayNames.putIfAbsent(itemKey, row.getItem());
			pageDisplayNames.putIfAbsent(pageKey, row.getPage());
			itemLooseNames.putIfAbsent(NameSearch.normalize(row.getItem()), row.getItem());
			pageLooseNames.putIfAbsent(NameSearch.normalize(row.getPage()), row.getPage());

			if (row.isCombat())
			{
				itemsWithCombatSource.add(itemKey);
				pagesWithCombatDrops.add(pageKey);
			}
		}

		itemSearch = new NameSearch(itemDisplayNames.values());
		pageSearch = new NameSearch(pageDisplayNames.values());
	}

	private static String key(String name)
	{
		return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
	}

	public long getFetchedAt()
	{
		return fetchedAt;
	}

	public int getRowCount()
	{
		return rowCount;
	}

	public int getItemCount()
	{
		return byItem.size();
	}

	public int getPageCount()
	{
		return byPage.size();
	}

	/** Typeahead suggestions for the Items tab. */
	public List<String> suggestItems(String query, int limit, boolean includeNonCombat)
	{
		return itemSearch.search(query, limit,
			includeNonCombat ? null : name -> itemsWithCombatSource.contains(key(name)));
	}

	/** Typeahead suggestions for the Monsters tab. */
	public List<String> suggestPages(String query, int limit, boolean includeNonCombat)
	{
		return pageSearch.search(query, limit,
			includeNonCombat ? null : name -> pagesWithCombatDrops.contains(key(name)));
	}

	/**
	 * The item's name as the wiki spells it, or null if nothing drops it. Case, apostrophes, periods
	 * and extra spaces are ignored, so "ahrims staff" finds "Ahrim's staff".
	 */
	public String findItem(String name)
	{
		String exact = itemDisplayNames.get(key(name));
		return exact != null ? exact : itemLooseNames.get(NameSearch.normalize(name));
	}

	/** The page's name as the wiki spells it, or null if it has no drop table. Matching as for {@link #findItem}. */
	public String findPage(String name)
	{
		String exact = pageDisplayNames.get(key(name));
		return exact != null ? exact : pageLooseNames.get(NameSearch.normalize(name));
	}

	/** Pages that have at least one combat drop, i.e. things you kill. */
	public boolean hasCombatDrops(String page)
	{
		return pagesWithCombatDrops.contains(key(page));
	}

	/**
	 * Every drop table that gives this item, most likely first.
	 *
	 * @param includeNonCombat whether to include chests, minigames, skilling and other non-combat sources
	 */
	public List<SourceSummary> sourcesOf(String item, boolean includeNonCombat)
	{
		List<DropRow> rows = byItem.get(key(item));
		if (rows == null)
		{
			return Collections.emptyList();
		}

		Map<String, List<DropRow>> grouped = new LinkedHashMap<>();
		for (DropRow row : rows)
		{
			if (!includeNonCombat && !row.isCombat())
			{
				continue;
			}
			grouped.computeIfAbsent(row.getSourceKey(), k -> new ArrayList<>()).add(row);
		}

		List<SourceSummary> out = new ArrayList<>(grouped.size());
		for (List<DropRow> group : grouped.values())
		{
			DropRow first = group.get(0);
			out.add(new SourceSummary(first.getPage(), first.getVariant(), group));
		}

		out.sort(Comparator
			.comparingDouble(SourceSummary::sortKey).reversed()
			.thenComparing(SourceSummary::getLabel, String.CASE_INSENSITIVE_ORDER));
		return out;
	}

	/**
	 * The page's drop tables keyed by variant, in the order the wiki lists them. Pages with a single
	 * table use the page name as the only key.
	 */
	public Map<String, List<DropRow>> dropsOn(String page)
	{
		List<DropRow> rows = byPage.get(key(page));
		if (rows == null)
		{
			return Collections.emptyMap();
		}

		Map<String, List<DropRow>> variants = new LinkedHashMap<>();
		for (DropRow row : rows)
		{
			String label = row.getVariant() == null ? row.getPage() : row.getVariant();
			variants.computeIfAbsent(label, k -> new ArrayList<>()).add(row);
		}
		return variants;
	}
}
