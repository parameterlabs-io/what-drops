package com.lootfinder.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

/** Parses a sample of real wiki rows and checks both lookup directions. */
public class DropIndexTest
{
	private static final double EPS = 1e-9;

	private final Gson gson = new Gson();
	private List<DropRow> rows;
	private DropIndex index;

	@Before
	public void setUp() throws IOException
	{
		try (InputStream in = getClass().getResourceAsStream("/com/lootfinder/dropsline-sample.json"))
		{
			BucketParser.Page page = new BucketParser(gson).parse(new InputStreamReader(in, StandardCharsets.UTF_8));
			assertEquals(20, page.getRawCount());
			rows = page.getRows();
		}
		index = new DropIndex(new DropDataset(123L, rows));
	}

	@Test
	public void parsesRowFields()
	{
		DropRow whip = rows.get(0);
		assertEquals("Abyssal demon", whip.getPage());
		assertEquals("Standard", whip.getVariant());
		assertEquals("Abyssal whip", whip.getItem());
		assertEquals(1.0 / 512, whip.getRarity().getProbability(), EPS);
		assertEquals("124", whip.getLevel());
		assertTrue(whip.isCombat());
		assertFalse(whip.isRareDropTable());
		assertNull(whip.getAltRarity());

		DropRow coins = find("Abyssal demon", "Coins");
		assertTrue(coins.isRareDropTable());

		DropRow coal = find("Catablepon", "Coal");
		assertEquals("3–7 (noted)", coal.getQuantity());
		assertTrue(coal.isNoted());
		assertEquals(3, coal.getQuantityLow());
		assertEquals(7, coal.getQuantityHigh());
		assertNull(coal.getVariant());

		DropRow sapphire = find("Abyssal demon", "Uncut sapphire");
		assertEquals(1.0 / 48.94, sapphire.getAltRarity().getProbability(), EPS);

		DropRow shrimps = find("Fishing Trawler", "Raw shrimps");
		assertFalse(shrimps.isCombat());
		assertEquals("reward", shrimps.getDropType());
		assertFalse(shrimps.getRarity().isKnown());
	}

	@Test
	public void reverseLookupRanksSourcesByChance()
	{
		List<SourceSummary> sources = index.sourcesOf("abyssal whip", true);
		assertEquals(3, sources.size());
		// Unsired (12/128) beats both Abyssal demon tables (1/512)
		assertEquals("Unsired", sources.get(0).getPage());
		assertEquals("Abyssal demon (Standard)", sources.get(1).getLabel());
		assertEquals("Abyssal demon (Wilderness Slayer Cave)", sources.get(2).getLabel());
	}

	@Test
	public void reverseLookupCanHideNonCombatSources()
	{
		List<SourceSummary> sources = index.sourcesOf("Abyssal whip", false);
		assertEquals(2, sources.size());
		for (SourceSummary s : sources)
		{
			assertEquals("Abyssal demon", s.getPage());
		}
		assertTrue(index.sourcesOf("Heron", false).isEmpty());
		assertEquals(1, index.sourcesOf("Heron", true).size());
	}

	@Test
	public void combinesRepeatedLinesAndRolls()
	{
		SourceSummary coins = only(index.sourcesOf("Coins", true), "Mithril dragon");
		assertEquals(2, coins.getRows().size());
		assertTrue(coins.isCombined());
		// Alternatives on the same roll add up: 17/128 + 7/128
		assertEquals(24.0 / 128, coins.getPerKillChance(), EPS);

		SourceSummary toadflax = only(index.sourcesOf("Toadflax seed", true), "Nechryael");
		assertTrue(toadflax.isCombined());
		assertEquals(1 - Math.pow(1 - 1 / 32.5, 2), toadflax.getPerKillChance(), EPS);
	}

	@Test
	public void monsterLookupKeepsVariantsInWikiOrder()
	{
		Map<String, List<DropRow>> drops = index.dropsOn("ABYSSAL DEMON");
		assertEquals("Standard", new ArrayList<>(drops.keySet()).get(0));
		assertTrue(drops.containsKey("Wilderness Slayer Cave"));
		assertEquals(8, drops.get("Standard").size());

		Map<String, List<DropRow>> single = index.dropsOn("Nechryael");
		assertEquals(1, single.size());
		assertTrue(single.containsKey("Nechryael"));
	}

	@Test
	public void suggestionsRespectNonCombatFilter()
	{
		assertTrue(index.suggestPages("fish", 10, true).contains("Fishing Trawler"));
		assertFalse(index.suggestPages("fish", 10, false).contains("Fishing Trawler"));
		assertEquals("Abyssal whip", index.suggestItems("whip", 10, false).get(0));
		assertTrue(index.suggestItems("heron", 10, false).isEmpty());
	}

	@Test
	public void findsCanonicalNames()
	{
		assertEquals("Abyssal whip", index.findItem("  abyssal WHIP "));
		assertEquals("H.A.M. Member", index.findPage("h.a.m. member"));
		assertEquals("H.A.M. Member", index.findPage("ham  member"));
		assertEquals("Kq head (tattered)", index.findItem("kq head (tattered)"));
		assertNull(index.findItem("Twisted bow"));
		assertEquals(20, index.getRowCount());
		assertEquals(10, index.getPageCount());
		assertEquals(16, index.getItemCount());
	}

	private DropRow find(String page, String item)
	{
		for (DropRow row : rows)
		{
			if (row.getPage().equals(page) && row.getItem().equals(item))
			{
				return row;
			}
		}
		throw new AssertionError("missing " + item + " from " + page);
	}

	private static SourceSummary only(List<SourceSummary> sources, String page)
	{
		SourceSummary found = null;
		for (SourceSummary s : sources)
		{
			if (s.getPage().equals(page))
			{
				assertNull("more than one table for " + page, found);
				found = s;
			}
		}
		if (found == null)
		{
			throw new AssertionError("no source " + page);
		}
		return found;
	}
}
