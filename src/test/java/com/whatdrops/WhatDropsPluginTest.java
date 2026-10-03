package com.whatdrops;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class WhatDropsPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(WhatDropsPlugin.class);
		RuneLite.main(args);
	}
}
