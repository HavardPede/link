package com.example.link;

import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.party.PartyService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import com.google.inject.Provides;

@Slf4j
@PluginDescriptor(name = "Link")
public class LinkPlugin extends Plugin
{
	private static final String SPIKE_COMMAND = "::linktest";

	@Inject
	private Client client;

	@Inject
	private PartyService partyService;

	@Inject
	private LinkConfig config;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Link plugin started — spike mode");
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Link plugin stopped");
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.PUBLICCHAT
			&& event.getType() != ChatMessageType.MODCHAT)
		{
			return;
		}

		String message = event.getMessage();
		if (!message.startsWith(SPIKE_COMMAND))
		{
			return;
		}

		String[] parts = message.split(" ", 2);
		if (parts.length > 1)
		{
			String passphrase = parts[1].trim();
			log.info("Spike: joining party with passphrase '{}'", passphrase);
			partyService.changeParty(passphrase);
		}
		else
		{
			log.info("Spike: leaving party");
			partyService.changeParty(null);
		}
	}

	@Provides
	LinkConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(LinkConfig.class);
	}
}
