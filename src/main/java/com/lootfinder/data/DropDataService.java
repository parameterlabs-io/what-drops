package com.lootfinder.data;

import com.google.gson.Gson;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps an up-to-date {@link DropIndex}: loads the local cache on start, then downloads fresh data
 * from the wiki when the cache is missing or more than a week old. A stale cache is used while
 * the download runs, and kept if the download fails.
 */
public final class DropDataService
{
	private static final Logger log = LoggerFactory.getLogger(DropDataService.class);

	static final long MAX_AGE_MILLIS = TimeUnit.DAYS.toMillis(7);
	/** A download with fewer rows than this is treated as broken rather than replacing good data. */
	static final int MIN_EXPECTED_ROWS = 1000;

	public enum Status
	{
		LOADING,
		READY,
		UPDATING,
		ERROR
	}

	/** Opens the cache. Called on a background thread because it touches the disk. */
	public interface CacheProvider
	{
		DropCache open() throws Exception;
	}

	public interface Listener
	{
		/** Called on a background thread. */
		void onDataStatus(Status status, String message);

		/** Called on a background thread whenever a new index is available. */
		void onDataIndex(DropIndex index);
	}

	private final WikiDropClient client;
	private final CacheProvider cacheProvider;
	private final ScheduledExecutorService executor;
	private final List<Listener> listeners = new CopyOnWriteArrayList<>();

	private volatile DropCache cache;
	private volatile DropIndex index;
	private volatile boolean stopped;
	private volatile boolean downloading;

	public DropDataService(OkHttpClient httpClient, Gson gson, ScheduledExecutorService executor,
		CacheProvider cacheProvider)
	{
		this.client = new WikiDropClient(httpClient, gson);
		this.cacheProvider = cacheProvider;
		this.executor = executor;
	}

	public void addListener(Listener listener)
	{
		listeners.add(listener);
		DropIndex current = index;
		if (current != null)
		{
			listener.onDataIndex(current);
		}
	}

	public void removeListener(Listener listener)
	{
		listeners.remove(listener);
	}

	public DropIndex getIndex()
	{
		return index;
	}

	/** Loads the cache and refreshes from the wiki if needed. Returns immediately. */
	public void start()
	{
		stopped = false;
		publishStatus(Status.LOADING, "Loading drop data…");
		executor.execute(this::loadCacheThenRefresh);
	}

	public void stop()
	{
		stopped = true;
		client.cancel();
		listeners.clear();
	}

	/** Downloads fresh data now, e.g. after an error. Ignored if a download is already running. */
	public void refresh()
	{
		if (!stopped)
		{
			executor.execute(this::download);
		}
	}

	private void loadCacheThenRefresh()
	{
		try
		{
			cache = cacheProvider == null ? null : cacheProvider.open();
		}
		catch (Exception e)
		{
			log.warn("Loot Finder can't use its data folder; drop data won't be saved between sessions", e);
		}

		DropDataset cached = null;
		if (cache != null)
		{
			try
			{
				cached = cache.load();
			}
			catch (Exception e)
			{
				log.warn("Couldn't read the Loot Finder cache, downloading fresh data", e);
			}
		}

		if (cached != null)
		{
			publishIndex(new DropIndex(cached));
		}

		boolean stale = cached == null || System.currentTimeMillis() - cached.getFetchedAt() > MAX_AGE_MILLIS;
		if (stale)
		{
			download();
		}
		else
		{
			publishStatus(Status.READY, null);
		}
	}

	private synchronized void download()
	{
		if (stopped || downloading)
		{
			return;
		}
		downloading = true;

		boolean haveData = index != null;
		publishStatus(haveData ? Status.UPDATING : Status.LOADING,
			haveData ? "Updating drop data from the OSRS Wiki…" : "Downloading drop data from the OSRS Wiki…");

		client.fetchAll().whenComplete((rows, error) ->
		{
			downloading = false;
			if (stopped)
			{
				return;
			}

			if (error != null || rows == null || rows.size() < MIN_EXPECTED_ROWS)
			{
				if (error != null)
				{
					log.warn("Loot Finder couldn't download drop data", error);
				}
				else
				{
					log.warn("Loot Finder got an unexpectedly small download ({} rows), ignoring it",
						rows == null ? 0 : rows.size());
				}

				publishStatus(Status.ERROR, index != null
					? "Update failed. Using saved data."
					: "Couldn't reach the OSRS Wiki.");
				return;
			}

			DropDataset dataset = new DropDataset(System.currentTimeMillis(), rows);
			publishIndex(new DropIndex(dataset));
			publishStatus(Status.READY, null);
			log.debug("Loot Finder downloaded {} drop rows", rows.size());

			DropCache target = cache;
			if (target != null)
			{
				executor.execute(() ->
				{
					try
					{
						target.save(dataset);
					}
					catch (Exception e)
					{
						log.warn("Couldn't save the Loot Finder cache", e);
					}
				});
			}
		});
	}

	private void publishIndex(DropIndex newIndex)
	{
		index = newIndex;
		for (Listener l : listeners)
		{
			l.onDataIndex(newIndex);
		}
	}

	private void publishStatus(Status status, String message)
	{
		for (Listener l : listeners)
		{
			l.onDataStatus(status, message);
		}
	}
}
