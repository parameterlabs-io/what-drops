package com.whatdrops;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(WhatDropsConfig.GROUP)
public interface WhatDropsConfig extends Config
{
	String GROUP = "whatdrops";
	String KEY_INCLUDE_NON_COMBAT = "includeNonCombat";
	String KEY_RARITY_FORMAT = "rarityFormat";
	String KEY_LOOKUP_MODE = "lookupMode";

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

	@ConfigItem(
		keyName = KEY_LOOKUP_MODE,
		name = "Right-click lookup",
		description = "When to add a \"Lookup drops\" entry to the right-click menu of monsters you can attack or pickpocket",
		position = 3
	)
	default LookupMode lookupMode()
	{
		return LookupMode.ALWAYS;
	}
}
