# Roadmap

Ideas that were discussed but kept out of v0.1 so they can be judged on their own. Each has a note
on how it would work with what's already built.

## Candidates

### Right-click "Drop sources" on items
Add a menu option to items in the inventory and bank (and possibly the Grand Exchange) that opens
the Items tab for that item. Other drop plugins didn't offer this entry point when we checked.
- Hook `MenuEntryAdded` for inventory and bank widgets and add a client-side entry. It must not
  send anything to the server, which is a Plugin Hub rule.
- Get the name with `ItemManager.getItemComposition(id).getName()` on the client thread, then
  call `LootFinderPanel.openItem(name)` on the Swing thread and open the panel.
- Needs a config toggle (on, hold Shift, off), like Loot Lookup's NPC option.

### Grand Exchange prices on drops
Show each drop's value next to its rate in the Monsters tab.
- `ItemManager.getItemPrice(id)` gives live prices for tradeable items. `ItemIcons` already maps
  names to IDs.
- The wiki rows also carry a `Drop Value` field. `BucketParser` ignores it for now; keeping it
  would cover untradeables that have an alch or store value.

### Expected GP per kill
Sum rate × average quantity × price over a drop table. Show common drops and rare uniques
separately so one lucky drop doesn't inflate the total.
- Depends on GE prices (above).
- `DropRow` already has `quantityLow` and `quantityHigh`, and `getPerKillChance()` handles
  multiple rolls.
- Words-only rates ("Rare", "Varies") have no number and would need to be left out and flagged.

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

### Slayer task integration
When a task is assigned, open the Monsters tab for that monster and compare its variants and
locations.
- Read the task from the Slayer plugin's config or the chat message.
- Variants such as "Wilderness Slayer Cave" already come through as separate drop tables.

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

- **Right-click "Lookup drops" on NPCs.** This is Loot Lookup's core feature. Copying it would
  make this plugin read as a duplicate on the Plugin Hub.
