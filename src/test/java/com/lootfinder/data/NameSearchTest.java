package com.lootfinder.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class NameSearchTest
{
	private final NameSearch search = new NameSearch(Arrays.asList(
		"Abyssal whip",
		"Abyssal demon",
		"Greater abyssal demon",
		"Abyssal Sire",
		"Ahrim's staff",
		"H.A.M. Member",
		"Black demon",
		"Kq head (tattered)",
		"Demonic gorilla"
	));

	@Test
	public void ranksPrefixBeforeWordBeforeContains()
	{
		List<String> result = search.search("demon", 10, null);
		// "Demonic gorilla" starts with the query; the others have it as a later word
		assertEquals("Demonic gorilla", result.get(0));
		assertTrue(result.indexOf("Black demon") < result.indexOf("Greater abyssal demon"));
		assertEquals(4, result.size());
	}

	@Test
	public void exactMatchComesFirst()
	{
		List<String> result = search.search("abyssal demon", 10, null);
		assertEquals("Abyssal demon", result.get(0));
		assertEquals("Greater abyssal demon", result.get(1));
	}

	@Test
	public void ignoresCaseApostrophesAndPeriods()
	{
		assertEquals(Arrays.asList("Ahrim's staff"), search.search("AHRIMS", 10, null));
		assertEquals(Arrays.asList("H.A.M. Member"), search.search("ham m", 10, null));
	}

	@Test
	public void matchesWordsInsideParentheses()
	{
		assertEquals(Arrays.asList("Kq head (tattered)"), search.search("tatt", 10, null));
	}

	@Test
	public void respectsLimitAndFilter()
	{
		assertEquals(2, search.search("abyssal", 2, null).size());
		List<String> filtered = search.search("abyssal", 10, name -> !name.contains("whip"));
		assertTrue(!filtered.contains("Abyssal whip"));
		assertEquals(3, filtered.size());
	}

	@Test
	public void emptyQueryReturnsNothing()
	{
		assertTrue(search.search("   ", 10, null).isEmpty());
	}
}
