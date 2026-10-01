package com.lootfinder;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(LootFinderConfig.GROUP)
public interface LootFinderConfig extends Config
{
	String GROUP = "lootfinder";
	String KEY_INCLUDE_NON_COMBAT = "includeNonCombat";
	String KEY_RARITY_FORMAT = "rarityFormat";

	@ConfigItem(
		keyName = KEY_INCLUDE_NON_COMBAT,
		name = "Include non-combat sources",
		description = "Also list chests, minigames, skilling and other reward sources, not just monsters",
		position = 1
	)
	default boolean includeNonCombat()
	{
		return true;
	}

	@ConfigItem(
		keyName = KEY_RARITY_FORMAT,
		name = "Drop rate format",
		description = "Show drop rates as fractions (1/512) or percentages (0.195%)",
		position = 2
	)
	default RarityFormat rarityFormat()
	{
		return RarityFormat.FRACTION;
	}
}
