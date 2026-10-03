package com.whatdrops.ui;

import com.whatdrops.RarityFormat;
import com.whatdrops.data.DropIndex;
import com.whatdrops.data.DropRow;
import com.whatdrops.data.Rarity;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;

/** The Monsters tab: search for a monster (or chest, activity...) and see its whole drop table. */
final class MonstersView extends JPanel
{
	private static final Comparator<DropRow> MOST_LIKELY_FIRST = Comparator
		.comparingDouble((DropRow r) -> r.getRarity().sortKey()).reversed()
		.thenComparing(DropRow::getItem, String.CASE_INSENSITIVE_ORDER);

	private final WhatDropsPanel owner;
	private final TypeaheadField search = new TypeaheadField();
	private final ResultsList results = new ResultsList();

	private DropIndex index;
	private String currentPage;
	private String currentVariant;

	MonstersView(WhatDropsPanel owner)
	{
		this.owner = owner;
		setLayout(new BorderLayout(0, 6));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		search.setSuggestionProvider(q -> index == null
			? new ArrayList<>()
			: index.suggestPages(q, 8, owner.includeNonCombat()));
		search.setOnSubmit(page -> show(page, null));

		add(search, BorderLayout.NORTH);
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

	void refresh()
	{
		render();
	}

	/**
	 * @param variant drop table to open on pages with several (e.g. "Wilderness Slayer Cave"), or null
	 */
	void show(String page, String variant)
	{
		String canonical = index == null ? null : index.findPage(page);
		String name = canonical != null ? canonical : page;

		search.hidePopup();
		search.setText(name);
		currentPage = name;
		currentVariant = variant;
		render();
	}

	private void render()
	{
		if (index == null)
		{
			results.showMessage(owner.loadingMessage());
			return;
		}
		if (currentPage == null)
		{
			results.showMessage("Search for a monster, chest or activity to see its drop table.");
			return;
		}

		String page = index.findPage(currentPage);
		if (page == null)
		{
			results.showMessage("No drop table on the OSRS Wiki for \"" + currentPage + "\".");
			return;
		}

		Map<String, List<DropRow>> variants = index.dropsOn(page);
		List<String> names = new ArrayList<>(variants.keySet());
		String selected;
		if (currentVariant != null && variants.containsKey(currentVariant))
		{
			selected = currentVariant;
		}
		else if (variants.containsKey(page))
		{
			// Rows without a variant are filed under the page name
			selected = page;
		}
		else
		{
			selected = names.get(0);
		}
		List<DropRow> rows = variants.get(selected);

		results.clear();
		results.add(header(page, rows.size()));
		if (names.size() > 1)
		{
			results.add(variantPicker(names, selected));
		}
		addGroupedRows(rows, owner.rarityFormat());
		results.done();
	}

	private JComponent header(String page, int dropCount)
	{
		ResultRow row = new ResultRow(false, page,
			dropCount == 1 ? "1 drop" : dropCount + " drops",
			"Wiki", ColorScheme.BRAND_ORANGE, null,
			"Open " + page + " on the OSRS Wiki",
			() -> owner.openWiki(page));
		row.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
		return row;
	}

	private JComponent variantPicker(List<String> names, String selected)
	{
		JComboBox<String> box = new JComboBox<>(names.toArray(new String[0]));
		box.setSelectedItem(selected);
		box.setFont(Styles.bodyFont());
		box.setForeground(Color.WHITE);
		box.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		box.setFocusable(false);
		box.setPreferredSize(new Dimension(0, 25));
		box.setToolTipText("This page has " + names.size() + " drop tables");
		box.addActionListener(e ->
		{
			Object value = box.getSelectedItem();
			if (value != null && !value.equals(selected))
			{
				currentVariant = value.toString();
				render();
			}
		});
		return box;
	}

	private void addGroupedRows(List<DropRow> rows, RarityFormat format)
	{
		Map<Rarity.Tier, List<DropRow>> byTier = new EnumMap<>(Rarity.Tier.class);
		List<DropRow> rareDropTable = new ArrayList<>();
		for (DropRow row : rows)
		{
			if (row.isRareDropTable())
			{
				rareDropTable.add(row);
			}
			else
			{
				byTier.computeIfAbsent(row.getRarity().getTier(), t -> new ArrayList<>()).add(row);
			}
		}

		for (Map.Entry<Rarity.Tier, List<DropRow>> entry : byTier.entrySet())
		{
			List<DropRow> group = entry.getValue();
			group.sort(MOST_LIKELY_FIRST);
			results.add(ResultsList.section(entry.getKey().getLabel() + " (" + group.size() + ")",
				Styles.tierColor(entry.getKey())));
			for (DropRow row : group)
			{
				results.add(dropRow(row, format));
			}
		}

		if (!rareDropTable.isEmpty())
		{
			rareDropTable.sort(MOST_LIKELY_FIRST);
			results.add(ResultsList.section("Rare drop table (" + rareDropTable.size() + ")", Styles.MUTED_TEXT));
			for (DropRow row : rareDropTable)
			{
				results.add(dropRow(row, format));
			}
		}
	}

	private JComponent dropRow(DropRow row, RarityFormat format)
	{
		String rate = Styles.rate(row.getRarity(), format);
		String detail = "×" + row.getQuantity();
		if (row.getRolls() > 1)
		{
			detail += " · " + row.getRolls() + " rolls";
		}

		String tooltip = Styles.rowTooltip(row, format);
		String hint = "Click to see everything that drops " + row.getItem();
		tooltip = tooltip == null
			? hint
			: tooltip.replace("</html>", "<br>" + Styles.escape(hint) + "</html>");

		ResultRow panel = new ResultRow(true, row.getItem(), detail, rate,
			Styles.tierColor(row.getRarity().getTier()), null, tooltip,
			() -> owner.openItem(row.getItem()));
		int quantity = row.getQuantityHigh() > 0 ? row.getQuantityHigh() : 1;
		owner.getIcons().apply(panel.getIconLabel(), row.getItem(), quantity);
		return panel;
	}
}
