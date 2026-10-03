package com.whatdrops.data;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardCopyOption;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import net.runelite.client.util.Filepath;

/**
 * Stores the downloaded drop rows in the plugin's data folder (.runelite/plugin-data/what-drops),
 * gzipped, so the panel works immediately on the next start and the wiki is only asked once a week.
 */
public final class DropCache
{
	static final String FILE_NAME = "drops-v" + DropDataset.FORMAT_VERSION + ".json.gz";

	private final Gson gson;
	private final Filepath directory;

	public DropCache(Gson gson, Filepath directory)
	{
		this.gson = gson;
		this.directory = directory;
	}

	/** The cached dataset, or null if there is none or it can't be read. */
	public DropDataset load() throws IOException
	{
		Filepath file = directory.joinSegment(FILE_NAME);
		if (!file.exists())
		{
			return null;
		}

		try (Reader reader = new InputStreamReader(new GZIPInputStream(file.openInputStream()), StandardCharsets.UTF_8))
		{
			DropDataset dataset = gson.fromJson(reader, DropDataset.class);
			return dataset != null && dataset.isUsable() ? dataset : null;
		}
		catch (JsonParseException e)
		{
			throw new IOException("Cache file is corrupt", e);
		}
	}

	public void save(DropDataset dataset) throws IOException
	{
		directory.createDirectories();
		Filepath file = directory.joinSegment(FILE_NAME);
		Filepath temp = directory.joinSegment(FILE_NAME + ".tmp");

		try (Writer writer = new OutputStreamWriter(new GZIPOutputStream(temp.openOutputStream()), StandardCharsets.UTF_8))
		{
			gson.toJson(dataset, writer);
		}
		temp.moveTo(file, StandardCopyOption.REPLACE_EXISTING);
	}
}
