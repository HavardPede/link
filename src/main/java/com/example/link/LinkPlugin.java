package com.example.link;

import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.api.events.CommandExecuted;
import net.runelite.client.party.PartyService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import com.google.inject.Provides;

@Slf4j
@PluginDescriptor(name = "Link")
public class LinkPlugin extends Plugin
{
	private static final String SPIKE_COMMAND = "linktest";

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
	public void onCommandExecuted(CommandExecuted event)
	{
		if (!event.getCommand().equals(SPIKE_COMMAND))
		{
			return;
		}

		String[] args = event.getArguments();
		if (args.length > 0)
		{
			String passphrase = String.join(" ", args);
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
