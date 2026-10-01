package com.lootfinder.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class RarityTest
{
	private static final double EPS = 1e-9;

	@Test
	public void parsesSimpleFractions()
	{
		assertEquals(1.0 / 512, Rarity.parse("1/512").getProbability(), EPS);
		assertEquals(4.0 / 128, Rarity.parse("4/128").getProbability(), EPS);
		assertEquals("4/128", Rarity.parse("4/128").getText());
	}

	@Test
	public void parsesThousandsSeparatorsAndDecimals()
	{
		assertEquals(1.0 / 5000, Rarity.parse("1/5,000").getProbability(), EPS);
		assertEquals(1.0 / 390.1, Rarity.parse("1/390.1").getProbability(), EPS);
		assertEquals(1.0 / 1_000_000, Rarity.parse("1/1,000,000").getProbability(), EPS);
		assertEquals(1.0 / 128, Rarity.parse("~1/128").getProbability(), EPS);
	}

	@Test
	public void parsesWords()
	{
		assertEquals(1.0, Rarity.parse("Always").getProbability(), EPS);
		assertEquals(Rarity.Tier.ALWAYS, Rarity.parse("always").getTier());
		assertEquals(Rarity.Tier.RARE, Rarity.parse("Rare").getTier());
		assertEquals(Rarity.Tier.VERY_RARE, Rarity.parse("Very rare").getTier());
		assertFalse(Rarity.parse("Rare").isKnown());
		assertEquals(Rarity.Tier.OTHER, Rarity.parse("Varies").getTier());
		assertEquals(Rarity.Tier.OTHER, Rarity.parse("Once").getTier());
		assertEquals(Rarity.Tier.OTHER, Rarity.parse("").getTier());
		assertEquals(Rarity.Tier.OTHER, Rarity.parse(null).getTier());
	}

	@Test
	public void assignsTiersFromProbability()
	{
		assertEquals(Rarity.Tier.COMMON, Rarity.parse("1/25").getTier());
		assertEquals(Rarity.Tier.COMMON, Rarity.parse("17/128").getTier());
		assertEquals(Rarity.Tier.UNCOMMON, Rarity.parse("4/128").getTier());
		assertEquals(Rarity.Tier.RARE, Rarity.parse("1/100").getTier());
		assertEquals(Rarity.Tier.RARE, Rarity.parse("1/512").getTier());
		assertEquals(Rarity.Tier.VERY_RARE, Rarity.parse("1/1,000").getTier());
		assertEquals(Rarity.Tier.VERY_RARE, Rarity.parse("1/5,000").getTier());
	}

	@Test
	public void sortsWordsAmongNumbers()
	{
		assertTrue(Rarity.parse("Always").sortKey() > Rarity.parse("1/2").sortKey());
		assertTrue(Rarity.parse("Rare").sortKey() > Rarity.parse("Very rare").sortKey());
		assertTrue(Rarity.parse("1/5,000").sortKey() > Rarity.parse("Unknown").sortKey());
	}

	@Test
	public void formatsOneIn()
	{
		assertEquals("1/512", Rarity.formatOneIn(1.0 / 512));
		assertEquals("1/5,000", Rarity.formatOneIn(1.0 / 5000));
		assertEquals("1/32.5", Rarity.formatOneIn(1.0 / 32.5));
		assertEquals("1/16.5", Rarity.formatOneIn(1 - Math.pow(1 - 1.0 / 32.5, 2)));
		assertEquals("Always", Rarity.formatOneIn(1.0));
		assertEquals("Unknown", Rarity.formatOneIn(Double.NaN));
	}

	@Test
	public void formatsPercent()
	{
		assertEquals("0.195%", Rarity.formatPercent(1.0 / 512));
		assertEquals("3.13%", Rarity.formatPercent(4.0 / 128));
		assertEquals("0.02%", Rarity.formatPercent(1.0 / 5000));
		assertEquals("100%", Rarity.formatPercent(1.0));
	}
}
