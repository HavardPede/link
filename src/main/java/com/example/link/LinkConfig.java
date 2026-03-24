package com.example.link;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("link")
public interface LinkConfig extends Config
{
	@ConfigItem(
		keyName = "enabled",
		name = "Enable Link",
		description = "Enable automatic party joining",
		position = 0
	)
	default boolean enabled()
	{
		return true;
	}
}
