package com.lootfinder.data;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Downloads every drop table row from the OSRS Wiki's Bucket API.
 *
 * <p>The {@code dropsline} bucket holds roughly 40,000 rows. The API returns at most 5,000 per
 * request, so this pages through it one request at a time (8 requests, under 1 MB gzipped in total).
 */
public final class WikiDropClient
{
	static final HttpUrl API = HttpUrl.get("https://oldschool.runescape.wiki/api.php");
	static final int PAGE_SIZE = 5000;
	/** Stops a misbehaving API from paging forever. 40 pages is 200k rows, 5x the current size. */
	static final int MAX_PAGES = 40;

	private final OkHttpClient httpClient;
	private final BucketParser parser;

	private volatile Call inFlight;
	private volatile boolean cancelled;

	public WikiDropClient(OkHttpClient httpClient, Gson gson)
	{
		this.httpClient = httpClient;
		this.parser = new BucketParser(gson);
	}

	static String query(int offset)
	{
		return "bucket('dropsline')"
			+ ".select('page_name','page_name_sub','item_name','rare_drop_table','drop_json')"
			+ ".limit(" + PAGE_SIZE + ").offset(" + offset + ").run()";
	}

	static HttpUrl url(int offset)
	{
		return API.newBuilder()
			.addQueryParameter("action", "bucket")
			.addQueryParameter("format", "json")
			.addQueryParameter("query", query(offset))
			.build();
	}

	/** Fetches all rows. Completes on an OkHttp thread. After {@link #cancel()} this always fails. */
	public CompletableFuture<List<DropRow>> fetchAll()
	{
		CompletableFuture<List<DropRow>> result = new CompletableFuture<>();
		fetchPage(0, new ArrayList<>(45_000), result);
		return result;
	}

	/** Stops the current download and any future ones. */
	public void cancel()
	{
		cancelled = true;
		Call call = inFlight;
		if (call != null)
		{
			call.cancel();
		}
	}

	private void fetchPage(int pageIndex, List<DropRow> collected, CompletableFuture<List<DropRow>> result)
	{
		if (cancelled)
		{
			result.completeExceptionally(new IOException("Cancelled"));
			return;
		}

		Request request = new Request.Builder()
			.url(url(pageIndex * PAGE_SIZE))
			.header("Accept", "application/json")
			.build();

		Call call = httpClient.newCall(request);
		inFlight = call;
		call.enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				result.completeExceptionally(e);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (Response res = response)
				{
					if (!res.isSuccessful())
					{
						throw new IOException("Wiki returned HTTP " + res.code());
					}
					ResponseBody body = res.body();
					if (body == null)
					{
						throw new IOException("Wiki returned an empty body");
					}

					BucketParser.Page page = parser.parse(body.charStream());
					collected.addAll(page.getRows());

					boolean lastPage = page.getRawCount() < PAGE_SIZE || pageIndex + 1 >= MAX_PAGES;
					if (lastPage)
					{
						inFlight = null;
						result.complete(collected);
					}
					else
					{
						fetchPage(pageIndex + 1, collected, result);
					}
				}
				catch (Exception e)
				{
					result.completeExceptionally(e);
				}
			}
		});
	}
}
