package com.whatdrops;

import com.google.gson.Gson;
import com.google.inject.Provides;
import com.whatdrops.data.DropCache;
import com.whatdrops.data.DropDataService;
import com.whatdrops.data.DropIndex;
import com.whatdrops.ui.WhatDropsPanel;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.KeyCode;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;
import okhttp3.OkHttpClient;

@PluginDescriptor(
	name = "What Drops",
	description = "Find every monster, chest and activity that drops an item, with drop rates from the OSRS Wiki",
	tags = {"drops", "drop table", "loot", "wiki", "rates", "reverse", "lookup", "item", "source"},
	internalName = "what-drops"
)
public class WhatDropsPlugin extends Plugin
{
	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private WhatDropsConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private OkHttpClient okHttpClient;

	@Inject
	private Gson gson;

	@Inject
	private ScheduledExecutorService executor;

	private DropDataService dataService;
	private WhatDropsPanel panel;
	private NavigationButton navButton;

	@Override
	protected void startUp() throws Exception
	{
		dataService = new DropDataService(okHttpClient, gson, executor,
			() -> new DropCache(gson, getPluginDirectory()));

		panel = new WhatDropsPanel(config, configManager, itemManager, client, clientThread, dataService);

		BufferedImage icon = ImageUtil.loadImageResource(WhatDropsPlugin.class, "panel_icon.png");
		navButton = NavigationButton.builder()
			.tooltip("What Drops")
			.icon(icon)
			.priority(8)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		dataService.addListener(panel);
		dataService.start();
	}

	@Override
	protected void shutDown() throws Exception
	{
		clientToolbar.removeNavigation(navButton);
		panel.stop();
		dataService.stop();
		navButton = null;
		panel = null;
		dataService = null;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		WhatDropsPanel p = panel;
		if (p != null && WhatDropsConfig.GROUP.equals(event.getGroup()))
		{
			SwingUtilities.invokeLater(p::refreshFromConfig);
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		// Every NPC menu has exactly one Examine entry, so hooking it adds one lookup entry per NPC
		if (event.getType() != MenuAction.EXAMINE_NPC.getId()
			|| !LookupMode.shows(config.lookupMode(), client.isKeyPressed(KeyCode.KC_SHIFT)))
		{
			return;
		}

		DropDataService data = dataService;
		DropIndex index = data == null ? null : data.getIndex();
		NPC npc = event.getMenuEntry().getNpc();
		NPCComposition composition = npc == null ? null : npc.getTransformedComposition();
		if (index == null || composition == null)
		{
			return;
		}

		// Only things you attack or pickpocket have drop tables worth offering
		List<String> actions = Arrays.asList(composition.getActions());
		if (!actions.contains("Attack") && !actions.contains("Pickpocket"))
		{
			return;
		}

		String page = index.findPage(Text.removeTags(composition.getName()));
		if (page != null)
		{
			// Client-side only: the RUNELITE type never sends an action to the server
			client.getMenu().createMenuEntry(-1)
				.setOption("Lookup drops")
				.setTarget(event.getTarget())
				.setType(MenuAction.RUNELITE)
				.onClick(e -> openLookup(page));
		}
	}

	/** Runs on the client thread, so hands off to the Swing thread. */
	private void openLookup(String page)
	{
		WhatDropsPanel p = panel;
		NavigationButton button = navButton;
		if (p != null && button != null)
		{
			SwingUtilities.invokeLater(() ->
			{
				p.lookupNpc(page);
				clientToolbar.openPanel(button);
			});
		}
	}

	@Provides
	WhatDropsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(WhatDropsConfig.class);
	}
}
