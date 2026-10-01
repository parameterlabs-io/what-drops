package com.lootfinder.ui;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Finds item sprites by name. Wiki rows name the item but don't give an id, so this indexes the
 * client's item definitions (tradeable and not). A wiki name that isn't an in-game name, such as
 * "Cabbage (Draynor Manor)", gets no sprite.
 */
final class ItemIcons
{
	private static final Logger log = LoggerFactory.getLogger(ItemIcons.class);
	/** ponytail: 1000 defs per client frame. Lower this if login hitches. */
	private static final int IDS_PER_FRAME = 1000;

	private final ItemManager itemManager;
	private final Client client;
	private final ClientThread clientThread;
	private final Runnable onReady;

	private final Map<String, Integer> building = new HashMap<>();
	private final Set<String> tradeableNames = new HashSet<>();
	private volatile Map<String, Integer> idsByName = Collections.emptyMap();
	private volatile boolean stopped;
	private int nextId;

	ItemIcons(ItemManager itemManager, Client client, ClientThread clientThread, Runnable onReady)
	{
		this.itemManager = itemManager;
		this.client = client;
		this.clientThread = clientThread;
		this.onReady = onReady;
		clientThread.invokeLater(this::indexSome);
	}

	void stop()
	{
		stopped = true;
	}

	/** Sets the item's sprite on the label if one is available. Call on the Swing thread. */
	void apply(JLabel label, String itemName)
	{
		apply(label, itemName, 1);
	}

	/**
	 * @param quantity selects the coin pile. The number is not drawn on the sprite; the row
	 *                 already shows it.
	 */
	void apply(JLabel label, String itemName, int quantity)
	{
		Integer id = resolve(idsByName, itemName);
		if (id == null)
		{
			return;
		}
		int qty = quantity > 0 ? quantity : 1;
		AsyncBufferedImage image = itemManager.getImage(id, qty, false);
		if (image != null)
		{
			image.addTo(label);
		}
	}

	/** Index the next batch. Returns false to run again next frame. */
	private boolean indexSome()
	{
		if (stopped)
		{
			return true;
		}
		if (client.getGameState().ordinal() < GameState.LOGIN_SCREEN.ordinal() || client.getItemCount() <= 0)
		{
			return false;
		}
		// Definitions are still "null" until the cache is open. Coins is the canary.
		if (nextId == 0 && !"Coins".equals(client.getItemDefinition(ItemID.COINS).getName()))
		{
			return false;
		}

		int count = client.getItemCount();
		int end = Math.min(count, nextId + IDS_PER_FRAME);
		for (int id = nextId; id < end; id++)
		{
			ItemComposition comp = client.getItemDefinition(id);
			remember(building, tradeableNames, comp.getName(), id,
				comp.getNote() != -1, comp.getPlaceholderTemplateId() != -1, comp.isTradeable());
		}
		nextId = end;
		if (nextId < count)
		{
			return false;
		}

		idsByName = building;
		log.debug("Indexed {} item names for sprites", building.size());
		if (!stopped)
		{
			SwingUtilities.invokeLater(onReady);
		}
		return true;
	}

	/** Keeps one id per name: skips notes, placeholders and "null", and prefers a tradeable id. */
	static void remember(Map<String, Integer> ids, Set<String> tradeableNames, String name, int id,
		boolean noted, boolean placeholder, boolean tradeable)
	{
		if (name == null || name.isEmpty() || name.equalsIgnoreCase("null") || noted || placeholder)
		{
			return;
		}
		String key = name.toLowerCase(Locale.ROOT);
		if (!ids.containsKey(key) || (tradeable && !tradeableNames.contains(key)))
		{
			ids.put(key, id);
			if (tradeable)
			{
				tradeableNames.add(key);
			}
		}
	}

	/** Case-insensitive. A wiki "#version" suffix falls back to the name before the hash. */
	static Integer resolve(Map<String, Integer> ids, String name)
	{
		if (name == null || ids.isEmpty())
		{
			return null;
		}
		String key = name.toLowerCase(Locale.ROOT);
		Integer id = ids.get(key);
		if (id != null)
		{
			return id;
		}
		int hash = key.indexOf('#');
		return hash > 0 ? ids.get(key.substring(0, hash)) : null;
	}
}
