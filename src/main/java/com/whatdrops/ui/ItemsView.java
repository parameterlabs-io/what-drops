package com.whatdrops.ui;

import com.whatdrops.RarityFormat;
import com.whatdrops.data.DropIndex;
import com.whatdrops.data.DropRow;
import com.whatdrops.data.SourceSummary;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;

/** The Items tab: search for an item, see every source that drops it, most likely first. */
final class ItemsView extends JPanel
{
	/** Items like Coins have well over a thousand sources; show the best ones first. */
	private static final int INITIAL_LIMIT = 50;

	private final WhatDropsPanel owner;
	private final TypeaheadField search = new TypeaheadField();
	private final JCheckBox includeNonCombat = new JCheckBox("Include non-combat sources");
	private final ResultsList results = new ResultsList();

	private DropIndex index;
	private String currentItem;
	private boolean showAll;

	ItemsView(WhatDropsPanel owner)
	{
		this.owner = owner;
		setLayout(new BorderLayout(0, 6));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		search.setSuggestionProvider(q -> index == null
			? new ArrayList<>()
			: index.suggestItems(q, 8, owner.includeNonCombat()));
		search.setOnSubmit(this::show);

		includeNonCombat.setFont(Styles.bodyFont());
		includeNonCombat.setForeground(Styles.MUTED_TEXT);
		includeNonCombat.setBackground(ColorScheme.DARK_GRAY_COLOR);
		includeNonCombat.setFocusable(false);
		includeNonCombat.setToolTipText("Chests, minigames, skilling and other reward sources");
		includeNonCombat.setSelected(owner.includeNonCombat());
		includeNonCombat.addActionListener(e -> owner.setIncludeNonCombat(includeNonCombat.isSelected()));

		JPanel top = new JPanel(new BorderLayout(0, 4));
		top.setOpaque(false);
		top.add(search, BorderLayout.NORTH);
		top.add(includeNonCombat, BorderLayout.SOUTH);

		add(top, BorderLayout.NORTH);
		add(results, BorderLayout.CENTER);
		render();
	}

	TypeaheadField getSearch()
	{
		return search;
	}

	void setIndex(DropIndex index)
	{
		this.index = index;
		render();
	}

	/** Re-reads settings (non-combat filter, rate format) and redraws. */
	void refresh()
	{
		includeNonCombat.setSelected(owner.includeNonCombat());
		render();
	}

	void show(String item)
	{
		// Use the wiki's spelling, e.g. "ahrims staff" -> "Ahrim's staff"
		String canonical = index == null ? null : index.findItem(item);
		String name = canonical != null ? canonical : item;

		search.hidePopup();
		search.setText(name);
		currentItem = name;
		showAll = false;
		render();
	}

	private void render()
	{
		if (index == null)
		{
			results.showMessage(owner.loadingMessage());
			return;
		}
		if (currentItem == null)
		{
			results.showMessage("Search for an item to see every monster, chest and activity that drops it.");
			return;
		}

		String item = index.findItem(currentItem);
		if (item == null)
		{
			results.showMessage("Nothing on the OSRS Wiki drops \"" + currentItem + "\".");
			return;
		}

		boolean nonCombat = owner.includeNonCombat();
		List<SourceSummary> sources = index.sourcesOf(item, nonCombat);
		RarityFormat format = owner.rarityFormat();

		results.clear();
		results.add(header(item, sources.size()));

		if (sources.isEmpty())
		{
			int hidden = index.sourcesOf(item, true).size();
			results.add(ResultsList.message(hidden > 0
				? "Only non-combat sources drop this. Tick \"Include non-combat sources\" to see them."
				: "No sources found."));
		}

		int limit = showAll ? sources.size() : Math.min(INITIAL_LIMIT, sources.size());
		for (int i = 0; i < limit; i++)
		{
			results.add(sourceRow(sources.get(i), format));
		}
		if (limit < sources.size())
		{
			results.add(ResultsList.linkButton("Show all " + sources.size() + " sources", () ->
			{
				showAll = true;
				render();
			}));
		}
		results.done();
	}

	private JComponent header(String item, int sourceCount)
	{
		ResultRow row = new ResultRow(true, item,
			sourceCount == 1 ? "1 source" : sourceCount + " sources",
			"Wiki", ColorScheme.BRAND_ORANGE, null,
			"Open " + item + " on the OSRS Wiki",
			() -> owner.openWiki(item));
		row.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
		owner.getIcons().apply(row.getIconLabel(), item);
		return row;
	}

	private JComponent sourceRow(SourceSummary source, RarityFormat format)
	{
		DropRow first = source.getFirst();

		double chance = source.getPerKillChance();
		String rate = source.isCombined() && !Double.isNaN(chance)
			? "≈" + Styles.rate(chance, format)
			: Styles.rate(first.getRarity(), format);
		Color rateColor = Styles.tierColor(source.getTier());

		List<String> details = new ArrayList<>();
		if (source.getVariant() != null)
		{
			details.add(source.getVariant());
		}
		String type = Styles.sourceType(first);
		if (!type.isEmpty())
		{
			details.add(type);
		}
		String level = Styles.level(first);
		if (!level.isEmpty())
		{
			details.add(level);
		}

		Set<String> amounts = new LinkedHashSet<>();
		for (DropRow row : source.getRows())
		{
			amounts.add(row.getQuantity());
		}

		return new ResultRow(false, source.getPage(), String.join(" · ", details), rate, rateColor,
			"×" + String.join(", ", amounts), sourceTooltip(source, format),
			() -> owner.openSource(source.getPage(), source.getVariant()));
	}

	private static String sourceTooltip(SourceSummary source, RarityFormat format)
	{
		StringBuilder sb = new StringBuilder();
		Styles.line(sb, source.getLabel());
		if (source.getRows().size() > 1)
		{
			Styles.line(sb, "Listed " + source.getRows().size() + " times in this table:");
			for (DropRow row : source.getRows())
			{
				Styles.line(sb, "  " + Styles.rate(row.getRarity(), format) + " for ×" + row.getQuantity());
			}
			Styles.line(sb, "Chance per kill combines all of them.");
		}
		else
		{
			DropRow row = source.getFirst();
			if (row.getRolls() > 1)
			{
				Styles.line(sb, Styles.rate(row.getRarity(), format) + " per roll, "
					+ row.getRolls() + " rolls per kill");
			}
			if (row.getAltRarity() != null)
			{
				Styles.line(sb, "Alternative rate: " + Styles.rate(row.getAltRarity(), format));
			}
			if (row.isRareDropTable())
			{
				Styles.line(sb, "From the rare drop table");
			}
		}
		Styles.line(sb, "Click to see this drop table");
		return "<html>" + sb + "</html>";
	}
}
