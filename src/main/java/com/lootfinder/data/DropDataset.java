package com.lootfinder.data;

import java.util.List;

/** The full set of wiki drop rows plus when they were downloaded. This is what gets cached to disk. */
public final class DropDataset
{
	public static final int FORMAT_VERSION = 1;

	private int version;
	private long fetchedAt;
	private List<DropRow> rows;

	/** Required by Gson. */
	DropDataset()
	{
	}

	public DropDataset(long fetchedAt, List<DropRow> rows)
	{
		this.version = FORMAT_VERSION;
		this.fetchedAt = fetchedAt;
		this.rows = rows;
	}

	public int getVersion()
	{
		return version;
	}

	public long getFetchedAt()
	{
		return fetchedAt;
	}

	public List<DropRow> getRows()
	{
		return rows;
	}

	public boolean isUsable()
	{
		return version == FORMAT_VERSION && rows != null && !rows.isEmpty();
	}
}
