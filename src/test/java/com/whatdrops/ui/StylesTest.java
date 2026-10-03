package com.whatdrops.ui;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class StylesTest
{
	@Test
	public void replacesGlyphsTheRunescapeFontCannotDraw()
	{
		assertEquals("~1/9.3", Styles.shown("\u22481/9.3"));
		assertEquals("1-12 (noted)", Styles.shown("1\u201312 (noted)"));
		assertEquals("Lvl 558-908", Styles.shown("Lvl 558\u2013908"));
		assertEquals("1/45", Styles.shown("1/45"));
	}
}
