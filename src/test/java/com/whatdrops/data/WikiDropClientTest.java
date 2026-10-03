package com.whatdrops.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class WikiDropClientTest
{
	@Test
	public void buildsBucketQuery()
	{
		assertEquals("bucket('dropsline').select('page_name','page_name_sub','item_name','rare_drop_table','drop_json')"
			+ ".limit(5000).offset(10000).run()", WikiDropClient.query(10000));
	}

	@Test
	public void encodesQueryInUrl()
	{
		okhttp3.HttpUrl url = WikiDropClient.url(0);
		assertTrue(url.toString().startsWith("https://oldschool.runescape.wiki/api.php?action=bucket&format=json&query="));
		assertEquals(WikiDropClient.query(0), url.queryParameter("query"));
	}
}
