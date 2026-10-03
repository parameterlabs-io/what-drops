# Roadmap

## Direction

The goal is a better [Loot Lookup](https://github.com/donth77/loot-lookup-plugin), the Plugin Hub's
main drop table plugin (about 141,000 installs on 2026-10-01). The Items tab (reverse lookup) and
Monsters tab are two views over one drop index, so they stay in one plugin.

What we have that Loot Lookup doesn't: the whole wiki dataset held locally (it scrapes the wiki on
every lookup), instant typeahead, reverse lookup, and combined per-kill chances for tables that roll
more than once. What it has that we don't: the in-game right-click entry and item prices.

Other plugins in the space, checked on 2026-10-01:

| Plugin | Overlap |
|---|---|
| Drop Finder, Item Sources, Item Lookup | Reverse lookup, each under 600 installs. Item Sources also has a right-click entry on items. |
| Drop Rates Clog, Drop Rate Properties | Reverse lookup inside the collection log only. |
| Loot Table Viewer | Drop tables next to your own loot history. |
| Drop Rate Display, Droppy | Drop rate shown on loot you just received. |

Loot Lookup's open issues show what its users are asking for: reverse lookup, average gp per kill
on the item list, and separate free and members' drop tables.

## Planned, in order

### 1. Right-click "Lookup drops" on NPCs
Implemented and tested in the dev client on 2026-10-02 with no issues found. This is how most Loot
Lookup users reach it, so without it nobody has a reason to switch.
- `WhatDropsPlugin.onMenuEntryAdded` hooks the Examine entry (one per NPC) and adds a client-side
  `MenuAction.RUNELITE` entry for NPCs that have an Attack or Pickpocket option and a page in the
  drop index. It sends nothing to the server, which is a Plugin Hub rule.
- Clicking it calls `WhatDropsPanel.lookupNpc(page)` on the Swing thread and opens the panel. NPCs
  with several drop tables open on the first one and get the table picker.
- "Right-click lookup" setting: Always (default), Hold Shift or Never.
- Not done: a list of NPCs to exclude, and picking the table variant from the NPC's variant.

### 2. Grand Exchange prices on drops
Show each drop's value next to its rate in the Monsters tab.
- `ItemManager.getItemPrice(id)` gives live prices for tradeable items. `ItemIcons` already maps
  names to IDs.
- The wiki rows also carry a `Drop Value` field. `BucketParser` ignores it for now; keeping it
  would cover untradeables that have an alch or store value.
- Offer GE or High Alch as the price source, as Loot Lookup does.

### 3. Expected GP per kill
Sum rate × average quantity × price over a drop table. Show common drops and rare uniques
separately so one lucky drop doesn't inflate the total. Matches an open Loot Lookup issue.
- Depends on GE prices (above).
- `DropRow` already has `quantityLow` and `quantityHigh`, and `getPerKillChance()` handles
  multiple rolls.
- Words-only rates ("Rare", "Varies") have no number and would need to be left out and flagged.

## Candidates

### Free and members' drop tables
Let players hide members-only sources and drops, or show only free-to-play ones. Another open Loot
Lookup issue.
- Needs a members flag on each row. Check whether the `dropsline` bucket carries one; if not, it
  needs another wiki bucket.

### Right-click "Drop sources" on items
Add a menu option to items in the inventory and bank (and possibly the Grand Exchange) that opens
the Items tab for that item. Item Sources already offers this, so it is expected rather than a
selling point.
- Same approach as the NPC entry: client-side only. Get the name with
  `ItemManager.getItemComposition(id).getName()` on the client thread, then call
  `WhatDropsPanel.openItem(name)` on the Swing thread and open the panel.
- Same always / Shift / off toggle.

### Time to drop
Add kills per hour to get "expected hours to get item X" per source in the Items tab, and
GP per hour per monster.
- The wiki doesn't list kill rates for most monsters, so this needs user input, or timing the
  player's own kills with the NPC despawn and loot events.

### Compare and rank
Put 2–3 monsters side by side, or rank every monster matching a filter (e.g. "best GP/hr at
80 Slayer").
- `DropIndex` already holds every table in memory, so ranking is a loop over `dropsOn()`.
- Filters by combat level are possible from `DropRow.getLevel()`. Slayer requirements aren't in
  the drop data and would need another wiki bucket.

## Known gaps in v0.1

- **Some wiki names don't match an in-game item.** Sprites are looked up by the game's item name.
  Disambiguated wiki names such as "Cabbage (Draynor Manor)" or "Varrock teleport (tablet)" have no
  sprite.
- **No wiki section headings.** The bucket data doesn't include the wiki's own groupings
  ("Weapons and armour", "Tertiary"), so drops are grouped by rarity instead.
- **Paging isn't atomic.** The download is 8 separate requests. A wiki edit landing mid-download
  could duplicate or skip a row until the next weekly refresh.
- **No manual refresh except on error.** A "Refresh now" action could be added, rate-limited to
  keep load on the wiki low.
- **Wiki links open the page, not the section.** Linking straight to the drops section or the
  item's "Item sources" section would be nicer.

## Deliberately not planned

- **Slayer features.** Slayer needs the current task, kill counts and chat parsing, and the space
  already has Slayer Helper (about 33,000 installs) and several task loot trackers. It belongs in
  its own plugin. Hub plugins can't depend on each other, so that plugin would carry its own copy of
  the data layer or use only task data.
- **DMM-specific drop tables.** No data source identified.
