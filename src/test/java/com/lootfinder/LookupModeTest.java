package com.lootfinder;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class LookupModeTest
{
	@Test
	public void showsAccordingToModeAndShift()
	{
		assertTrue(LookupMode.shows(LookupMode.ALWAYS, false));
		assertTrue(LookupMode.shows(LookupMode.ALWAYS, true));

		assertFalse(LookupMode.shows(LookupMode.SHIFT, false));
		assertTrue(LookupMode.shows(LookupMode.SHIFT, true));

		assertFalse(LookupMode.shows(LookupMode.NEVER, false));
		assertFalse(LookupMode.shows(LookupMode.NEVER, true));
	}

	@Test
	public void unreadableSettingActsAsTheDefault()
	{
		assertTrue(LookupMode.shows(null, false));
	}
}
