package com.lootfinder.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * In-memory name matching for the typeahead. Ranks exact matches first, then names that start with
 * the query, then names with a word that starts with it, then names that contain it anywhere.
 * Apostrophes and periods are ignored, so "ahrims" finds "Ahrim's staff" and "ham" finds "H.A.M. Member".
 */
public final class NameSearch
{
	private static final int EXACT = 0;
	private static final int PREFIX = 1;
	private static final int WORD_PREFIX = 2;
	private static final int CONTAINS = 3;
	private static final int NO_MATCH = -1;

	private final List<String> names;
	private final List<String> normalized;

	public NameSearch(Collection<String> names)
	{
		this.names = new ArrayList<>(names);
		this.names.sort(String.CASE_INSENSITIVE_ORDER);
		this.normalized = new ArrayList<>(this.names.size());
		for (String name : this.names)
		{
			normalized.add(normalize(name));
		}
	}

	public List<String> search(String query, int limit, Predicate<String> filter)
	{
		String q = normalize(query);
		List<String> out = new ArrayList<>();
		if (q.isEmpty() || limit <= 0)
		{
			return out;
		}

		List<Match> matches = new ArrayList<>();
		for (int i = 0; i < names.size(); i++)
		{
			int score = score(normalized.get(i), q);
			if (score == NO_MATCH)
			{
				continue;
			}
			String name = names.get(i);
			if (filter != null && !filter.test(name))
			{
				continue;
			}
			matches.add(new Match(name, score));
		}

		matches.sort(Comparator
			.comparingInt((Match m) -> m.score)
			.thenComparingInt(m -> m.name.length())
			.thenComparing(m -> m.name, String.CASE_INSENSITIVE_ORDER));

		for (int i = 0; i < matches.size() && i < limit; i++)
		{
			out.add(matches.get(i).name);
		}
		return out;
	}

	static int score(String name, String q)
	{
		if (name.equals(q))
		{
			return EXACT;
		}
		if (name.startsWith(q))
		{
			return PREFIX;
		}
		int idx = name.indexOf(q);
		if (idx < 0)
		{
			return NO_MATCH;
		}
		while (idx >= 0)
		{
			char before = name.charAt(idx - 1);
			if (before == ' ' || before == '(' || before == '-' || before == '/')
			{
				return WORD_PREFIX;
			}
			idx = name.indexOf(q, idx + 1);
		}
		return CONTAINS;
	}

	public static String normalize(String s)
	{
		if (s == null)
		{
			return "";
		}
		StringBuilder sb = new StringBuilder(s.length());
		boolean lastSpace = true;
		for (char c : s.toLowerCase(Locale.ROOT).toCharArray())
		{
			if (c == '\'' || c == '’' || c == '.')
			{
				continue;
			}
			if (Character.isWhitespace(c) || c == '_')
			{
				if (!lastSpace)
				{
					sb.append(' ');
					lastSpace = true;
				}
				continue;
			}
			sb.append(c);
			lastSpace = false;
		}
		int len = sb.length();
		if (len > 0 && sb.charAt(len - 1) == ' ')
		{
			sb.setLength(len - 1);
		}
		return sb.toString();
	}

	private static final class Match
	{
		private final String name;
		private final int score;

		private Match(String name, int score)
		{
			this.name = name;
			this.score = score;
		}
	}
}
