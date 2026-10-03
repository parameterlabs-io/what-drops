package com.whatdrops.data;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parses responses from the OSRS Wiki's Bucket API for the {@code dropsline} bucket.
 *
 * <p>Each result row looks like:
 * <pre>
 * {"page_name": "Abyssal demon", "page_name_sub": "Abyssal demon#Standard", "item_name": "Abyssal whip",
 *  "rare_drop_table": "", "drop_json": "{\"Rarity\":\"1/512\", \"Drop Quantity\":\"1\", ...}"}
 * </pre>
 * {@code rare_drop_table} is only present on rows that come from the rare drop table.
 */
public final class BucketParser
{
	private final Gson gson;

	public BucketParser(Gson gson)
	{
		this.gson = gson;
	}

	/** One page of API results. */
	public static final class Page
	{
		private final int rawCount;
		private final List<DropRow> rows;

		Page(int rawCount, List<DropRow> rows)
		{
			this.rawCount = rawCount;
			this.rows = rows;
		}

		/** Rows the API returned, including any that were skipped as malformed. Used for paging. */
		public int getRawCount()
		{
			return rawCount;
		}

		public List<DropRow> getRows()
		{
			return rows;
		}
	}

	public Page parse(Reader reader) throws IOException
	{
		JsonObject root;
		try
		{
			root = gson.fromJson(reader, JsonObject.class);
		}
		catch (JsonParseException e)
		{
			throw new IOException("Malformed response from the wiki", e);
		}

		if (root == null)
		{
			throw new IOException("Empty response from the wiki");
		}
		if (root.has("error"))
		{
			throw new IOException("Wiki API error: " + describe(root.get("error")));
		}

		JsonElement bucket = root.get("bucket");
		if (bucket == null || !bucket.isJsonArray())
		{
			return new Page(0, Collections.emptyList());
		}

		JsonArray array = bucket.getAsJsonArray();
		List<DropRow> rows = new ArrayList<>(array.size());
		for (JsonElement element : array)
		{
			if (!element.isJsonObject())
			{
				continue;
			}
			DropRow row = parseRow(element.getAsJsonObject());
			if (row != null)
			{
				rows.add(row);
			}
		}
		return new Page(array.size(), rows);
	}

	DropRow parseRow(JsonObject raw)
	{
		String page = string(raw, "page_name");
		String item = string(raw, "item_name");
		if (page.isEmpty() || item.isEmpty())
		{
			return null;
		}

		JsonObject drop;
		try
		{
			String dropJson = string(raw, "drop_json");
			drop = dropJson.isEmpty() ? new JsonObject() : gson.fromJson(dropJson, JsonObject.class);
		}
		catch (JsonParseException e)
		{
			return null;
		}
		if (drop == null)
		{
			drop = new JsonObject();
		}

		String sub = string(raw, "page_name_sub");
		if (sub.isEmpty())
		{
			sub = string(drop, "Dropped from");
		}
		String variant = null;
		int hash = sub.indexOf('#');
		if (hash >= 0 && hash < sub.length() - 1)
		{
			variant = sub.substring(hash + 1).trim();
		}

		return new DropRow(
			page,
			variant,
			item,
			string(drop, "Rarity"),
			string(drop, "Alt Rarity"),
			string(drop, "Drop Quantity"),
			integer(drop, "Quantity Low"),
			integer(drop, "Quantity High"),
			integer(drop, "Rolls"),
			string(drop, "Drop type"),
			string(drop, "Drop level"),
			raw.has("rare_drop_table") && !raw.get("rare_drop_table").isJsonNull()
		);
	}

	private static String string(JsonObject obj, String key)
	{
		JsonElement e = obj.get(key);
		if (e == null || e.isJsonNull())
		{
			return "";
		}
		if (e.isJsonPrimitive())
		{
			return e.getAsString().trim();
		}
		return e.toString();
	}

	private static int integer(JsonObject obj, String key)
	{
		JsonElement e = obj.get(key);
		if (e == null || !e.isJsonPrimitive())
		{
			return 0;
		}
		try
		{
			return (int) Math.round(e.getAsDouble());
		}
		catch (NumberFormatException ex)
		{
			return 0;
		}
	}

	private static String describe(JsonElement error)
	{
		if (error.isJsonPrimitive())
		{
			return error.getAsString();
		}
		if (error.isJsonObject() && error.getAsJsonObject().has("info"))
		{
			return error.getAsJsonObject().get("info").getAsString();
		}
		return error.toString();
	}
}
