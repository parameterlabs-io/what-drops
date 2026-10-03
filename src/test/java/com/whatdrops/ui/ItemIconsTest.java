package com.whatdrops.ui;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class ItemIconsTest
{
	@Test
	public void prefersTheTradeableIdAndSkipsNotesAndPlaceholders()
	{
		Map<String, Integer> ids = new HashMap<>();
		Set<String> tradeable = new HashSet<>();

		ItemIcons.remember(ids, tradeable, "Coins", 617, false, false, false);
		ItemIcons.remember(ids, tradeable, "Coins", 995, false, false, true);
		ItemIcons.remember(ids, tradeable, "Coins", 996, false, false, true);
		ItemIcons.remember(ids, tradeable, "Coins", 997, true, false, true);
		ItemIcons.remember(ids, tradeable, "null", 1, false, false, true);
		ItemIcons.remember(ids, tradeable, "Abyssal whip", 4152, false, true, true);

		assertEquals(Integer.valueOf(995), ItemIcons.resolve(ids, "Coins"));
		assertEquals(Integer.valueOf(995), ItemIcons.resolve(ids, "coins"));
		assertNull(ItemIcons.resolve(ids, "Abyssal whip"));
	}

	@Test
	public void hashSuffixFallsBackToTheBaseName()
	{
		Map<String, Integer> ids = new HashMap<>();
		Set<String> tradeable = new HashSet<>();
		ItemIcons.remember(ids, tradeable, "Bird nest (egg)", 5070, false, false, false);

		assertEquals(Integer.valueOf(5070), ItemIcons.resolve(ids, "Bird nest (egg)#Blue egg"));
		assertNull(ItemIcons.resolve(ids, "Missing"));
		assertNull(ItemIcons.resolve(Collections.emptyMap(), "Coins"));
	}
}
