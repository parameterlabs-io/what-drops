package com.whatdrops.ui;

import com.whatdrops.WhatDropsConfig;
import com.whatdrops.RarityFormat;
import com.whatdrops.data.DropDataService;
import com.whatdrops.data.DropIndex;
import java.awt.BorderLayout;
import java.util.concurrent.TimeUnit;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;
import net.runelite.client.util.LinkBrowser;

/** The side panel: an Items tab (what drops this?) and a Monsters tab (what does this drop?). */
public class WhatDropsPanel extends PluginPanel implements DropDataService.Listener
{
	private final WhatDropsConfig config;
	private final ConfigManager configManager;
	private final DropDataService dataService;
	private final ItemIcons icons;

	private final ItemsView itemsView;
	private final MonstersView monstersView;
	private final MaterialTabGroup tabs;
	private final MaterialTab itemsTab;
	private final MaterialTab monstersTab;

	private final JLabel statusLabel = new JLabel();
	private final JComponent retryButton;
	private final JLabel footer = new JLabel();

	private DropIndex index;
	private String loadingMessage = "Loading drop data…";

	public WhatDropsPanel(WhatDropsConfig config, ConfigManager configManager, ItemManager itemManager,
		Client client, ClientThread clientThread, DropDataService dataService)
	{
		super(false);
		this.config = config;
		this.configManager = configManager;
		this.dataService = dataService;
		this.icons = new ItemIcons(itemManager, client, clientThread, this::onIconsReady);

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(BorderFactory.createEmptyBorder(8, 6, 6, 6));

		itemsView = new ItemsView(this);
		monstersView = new MonstersView(this);

		JPanel display = new JPanel();
		display.setBackground(ColorScheme.DARK_GRAY_COLOR);
		tabs = new MaterialTabGroup(display);
		tabs.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
		itemsTab = new MaterialTab("Items", tabs, itemsView);
		monstersTab = new MaterialTab("Monsters", tabs, monstersView);
		tabs.addTab(itemsTab);
		tabs.addTab(monstersTab);
		tabs.select(itemsTab);

		statusLabel.setFont(Styles.bodyFont());
		statusLabel.setForeground(Styles.MUTED_TEXT);
		statusLabel.setVisible(false);
		retryButton = ResultsList.linkButton("Retry", dataService::refresh);
		retryButton.setVisible(false);

		JPanel status = new JPanel(new BorderLayout());
		status.setOpaque(false);
		status.add(statusLabel, BorderLayout.CENTER);
		status.add(retryButton, BorderLayout.EAST);

		JPanel north = new JPanel(new BorderLayout());
		north.setOpaque(false);
		north.add(tabs, BorderLayout.NORTH);
		north.add(status, BorderLayout.SOUTH);

		footer.setFont(Styles.bodyFont());
		footer.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
		footer.setBorder(BorderFactory.createEmptyBorder(6, 2, 0, 2));
		footer.setToolTipText("<html>Drop data from the Old School RuneScape Wiki,<br>"
			+ "available under CC BY-NC-SA 3.0</html>");

		add(north, BorderLayout.NORTH);
		add(display, BorderLayout.CENTER);
		add(footer, BorderLayout.SOUTH);
		updateFooter();
	}

	@Override
	public void onActivate()
	{
		SwingUtilities.invokeLater(() ->
		{
			if (itemsTab.isSelected())
			{
				itemsView.getSearch().requestFocusInWindow();
			}
			else
			{
				monstersView.getSearch().requestFocusInWindow();
			}
		});
	}

	@Override
	public void onDeactivate()
	{
		itemsView.getSearch().hidePopup();
		monstersView.getSearch().hidePopup();
	}

	@Override
	public void onDataStatus(DropDataService.Status status, String message)
	{
		SwingUtilities.invokeLater(() ->
		{
			// Routine loading and background updates stay quiet; only failures get the status row
			boolean error = status == DropDataService.Status.ERROR;
			statusLabel.setText(error && message != null ? message : "");
			statusLabel.setVisible(error);
			retryButton.setVisible(error);

			if (index == null)
			{
				loadingMessage = error
					? "No drop data yet. Check your connection, then press Retry."
					: message != null ? message : "Loading drop data…";
				itemsView.refresh();
				monstersView.refresh();
			}
			revalidate();
			repaint();
		});
	}

	@Override
	public void onDataIndex(DropIndex newIndex)
	{
		SwingUtilities.invokeLater(() ->
		{
			index = newIndex;
			itemsView.setIndex(newIndex);
			monstersView.setIndex(newIndex);
			updateFooter();
		});
	}

	/** Stops the item-name index. Safe to call more than once. */
	public void stop()
	{
		icons.stop();
	}

	/** Re-reads config after a settings change. */
	public void refreshFromConfig()
	{
		itemsView.refresh();
		monstersView.refresh();
	}

	/** Shows a monster's drop table on the Monsters tab. Call on the Swing thread. */
	public void lookupNpc(String npcName)
	{
		openSource(npcName, null);
	}

	void openItem(String item)
	{
		tabs.select(itemsTab);
		itemsView.show(item);
	}

	void openSource(String page, String variant)
	{
		tabs.select(monstersTab);
		monstersView.show(page, variant);
	}

	void openWiki(String page)
	{
		LinkBrowser.browse(Styles.wikiUrl(page));
	}

	boolean includeNonCombat()
	{
		return config.includeNonCombat();
	}

	void setIncludeNonCombat(boolean include)
	{
		// Saving fires ConfigChanged, which calls refreshFromConfig
		configManager.setConfiguration(WhatDropsConfig.GROUP, WhatDropsConfig.KEY_INCLUDE_NON_COMBAT, include);
	}

	RarityFormat rarityFormat()
	{
		RarityFormat format = config.rarityFormat();
		return format == null ? RarityFormat.FRACTION : format;
	}

	ItemIcons getIcons()
	{
		return icons;
	}

	String loadingMessage()
	{
		return loadingMessage;
	}

	private void onIconsReady()
	{
		itemsView.refresh();
		monstersView.refresh();
	}

	private void updateFooter()
	{
		if (index == null)
		{
			footer.setText("Drop data from the OSRS Wiki");
			return;
		}
		long days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - index.getFetchedAt());
		String age = days <= 0 ? "today" : days == 1 ? "yesterday" : days + " days ago";
		footer.setText("OSRS Wiki · updated " + age);
	}
}
