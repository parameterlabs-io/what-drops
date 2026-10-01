# Loot Finder

A RuneLite plugin that answers "what drops this?" Search for any item and see every monster,
chest, minigame and skilling activity that gives it, ranked by drop rate. Drop rates come from the
[OSRS Wiki](https://oldschool.runescape.wiki).

## Features

- **Items tab:** search for an item and get every source, most likely first. Sources with several
  drop tables (e.g. Abyssal demon's standard and Wilderness Slayer Cave tables) are listed
  separately. Where a table rolls more than once per kill or lists an item more than once, the row
  shows the combined chance per kill (marked ≈).
- **Monsters tab:** search for a monster, chest or activity and see its full drop table, grouped
  by rarity (Always, Common, Uncommon, Rare, Very rare) with rare drop table items in their own
  group. Pages with several drop tables get a picker.
- **Linked tabs:** click a source in the Items tab to open its drop table. Click a drop in the
  Monsters tab to see everything else that drops it.
- **Typeahead search:** suggestions update as you type, from data already on your computer. Use
  Up/Down to pick one, Enter to search and Esc to close the list.
- **Non-combat sources:** chests, minigames, thieving, skilling and other reward sources are
  included by default. Untick "Include non-combat sources" to see only monsters.
- **Drop rate format:** show rates as fractions (1/512) or percentages (0.195%) in the plugin's
  settings.

## Where the data comes from

The plugin reads the wiki's structured drop data (the `dropsline` bucket of its
[Bucket API](https://oldschool.runescape.wiki/w/Bucket:Dropsline)) instead of scraping page HTML.
That's what makes reverse lookup possible, and it doesn't break when the wiki's page layout changes.

- About 40,000 rows, downloaded in 8 requests (under 1 MB gzipped in total).
- Saved to `.runelite/plugin-data/loot-finder/` and refreshed once a week. Saved data is used
  straight away on startup and kept if a refresh fails.
- The request goes only to the OSRS Wiki and contains no player information.

Wiki content is available under [CC BY-NC-SA 3.0](https://creativecommons.org/licenses/by-nc-sa/3.0/).
The panel credits the wiki in its footer.

## Building and running

You need JDK 11 or newer.

```bash
./gradlew run    # starts a RuneLite dev client with Loot Finder loaded
./gradlew test   # unit tests for the data layer
```

To log in to the dev client with a Jagex account, follow
[Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

### What to test in-game

1. Open the Loot Finder panel (magnifying glass over a loot bag in the sidebar). On first run it
   should say it's downloading drop data, then show the search hint.
2. **Items tab:** type `aby`. Suggestions should appear under the box. Press Down to highlight
   "Abyssal whip" and press Enter. Expect Unsired at the top, then Greater abyssal demon (≈1/171,
   because it rolls 3 times), then both Abyssal demon tables at 1/512.
3. Click the Abyssal demon row. It should switch to the Monsters tab on the right drop table.
4. Use the drop table picker to switch between Standard, Catacombs of Kourend and Wilderness Slayer
   Cave.
5. Click any drop (e.g. Clue scroll (hard)). It should jump back to the Items tab showing every
   source of that item, with a "Show all" button past 50 results.
6. Untick "Include non-combat sources". Unsired and Mystery box should disappear, and Heron should
   say only non-combat sources drop it.
7. Change "Drop rate format" in the plugin settings to Percent. Rates should update without a
   restart.
8. Restart the client. Results should appear immediately without downloading again.
9. Edge cases: click outside the search box while suggestions are open, switch tabs while
   suggestions are open, and search for something with no drops.

## Project layout

```
src/main/java/com/lootfinder/
  LootFinderPlugin.java      plugin entry point, wires up the panel and data service
  LootFinderConfig.java      settings
  data/
    WikiDropClient.java      pages through the wiki Bucket API
    BucketParser.java        turns API responses into DropRow objects
    DropCache.java           gzipped JSON cache in the plugin data folder
    DropDataService.java     load cache → refresh if stale → publish a DropIndex
    DropIndex.java           item → sources and page → drops lookups, typeahead
    Rarity.java              parses "1/512", "4/128", "1/5,000", "Always", ...
  ui/
    LootFinderPanel.java     side panel with Items and Monsters tabs
    ItemsView.java           reverse lookup
    MonstersView.java        drop table view
    TypeaheadField.java      search box with a suggestion list
```

See [ROADMAP.md](ROADMAP.md) for features that are planned but not built yet.

## Submitting to the Plugin Hub

1. Push this repo to GitHub.
2. Fork [runelite/plugin-hub](https://github.com/runelite/plugin-hub) and add a file
   `plugins/loot-finder` containing:
   ```
   repository=https://github.com/<you>/loot-finder.git
   commit=<full commit hash>
   ```
3. Open a pull request. The hub builds the plugin and a reviewer checks it.

Before submitting, update `author` in `runelite-plugin.properties` and the copyright line in
`LICENSE` if you want a different name there.

## License

BSD 2-Clause. See [LICENSE](LICENSE).
