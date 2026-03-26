package com.example.link;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("link")
public interface LinkConfig extends Config {
	@ConfigSection(name = "Connection", description = "Server connection settings", position = 0)
	String connectionSection = "connection";

	@ConfigItem(
			keyName = "bearerToken",
			name = "Bearer Token",
			description = "Paste your token from the party finder website settings page",
			secret = true,
			position = 0,
			section = connectionSection)
	default String bearerToken() {
		return "";
	}

	@ConfigItem(
			keyName = "serverUrl",
			name = "Server URL",
			description =
					"Base URL of the party finder server (e.g. http://localhost:3000 for local dev)",
			position = 1,
			section = connectionSection)
	default String serverUrl() {
		return "";
	}

	@ConfigItem(
			keyName = "enabled",
			name = "Enable Link",
			description = "Enable automatic party sync (polling and command execution)",
			position = 2,
			section = connectionSection)
	default boolean enabled() {
		return true;
	}
}
