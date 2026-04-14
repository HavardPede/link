package com.github.havardpede.partylink;

import com.google.inject.Provides;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PartyChanged;
import net.runelite.client.party.PartyService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import okhttp3.OkHttpClient;

@Slf4j
@PluginDescriptor(
		name = "Party Link",
		description =
				"Connects RuneLite to an external server for remote party management. "
						+ "Pair with your server to sync parties and receive commands in real time.")
public class LinkPlugin extends Plugin {
	private static final String JOIN_COMMAND = "join";
	private static final String LEAVE_COMMAND = "leave";

	@Inject private Client client;
	@Inject private PartyService partyService;
	@Inject private OkHttpClient okHttpClient;
	@Inject private ScheduledExecutorService executorService;
	@Inject private ClientThread clientThread;
	@Inject private ChatMessageManager chatMessageManager;
	@Inject private ConfigManager configManager;
	@Inject private LinkConfig config;

	private WebSocketManager webSocketManager;
	private RsnDetector rsnDetector;
	private CommandExecutor commandExecutor;

	@Override
	protected void startUp() {
		String wsUrl = config.websocketUrl();
		if (wsUrl == null || wsUrl.isEmpty()) {
			log.warn("Link plugin not started: WebSocket URL is not configured");
			return;
		}

		OkHttpClient wsClient =
				okHttpClient.newBuilder().pingInterval(30, TimeUnit.SECONDS).build();
		commandExecutor = new CommandExecutor(this::changeParty, this::sendChatMessage);
		webSocketManager =
				new WebSocketManager(
						wsClient,
						wsUrl,
						this::getStoredToken,
						this::getPairingCode,
						this::onPairResult,
						commandExecutor,
						executorService);
		rsnDetector = new RsnDetector(webSocketManager::sendIdentify, this::getPlayerName);
		log.info(
				"Link plugin started: gameState={}, enabled={}, hasToken={}, hasPairingCode={}",
				client.getGameState(),
				config.enabled(),
				hasToken(),
				hasPairingCode());

		if (client.getGameState() == GameState.LOGGED_IN) {
			if (config.enabled() && (hasToken() || hasPairingCode())) {
				webSocketManager.connect();
			}
			rsnDetector.onLoggedIn();
		}
	}

	@Override
	protected void shutDown() {
		commandExecutor = null;
		if (webSocketManager != null) {
			webSocketManager.disconnect();
		}
		log.info("Link plugin stopped");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event) {
		switch (event.getGameState()) {
			case LOGGED_IN:
				if (config.enabled() && (hasToken() || hasPairingCode())) {
					webSocketManager.connect();
				}
				rsnDetector.onLoggedIn();
				break;
			case LOGIN_SCREEN:
				webSocketManager.disconnect();
				break;
		}
	}

	@Subscribe
	public void onCommandExecuted(CommandExecuted event) {
		if (JOIN_COMMAND.equalsIgnoreCase(event.getCommand())) {
			onJoinCommand();
		} else if (LEAVE_COMMAND.equalsIgnoreCase(event.getCommand())) {
			changeParty(null);
		}
	}

	@Subscribe
	public void onGameTick(GameTick event) {
		rsnDetector.onGameTick();
	}

	@Subscribe
	public void onPartyChanged(PartyChanged event) {
		if (webSocketManager == null) {
			return;
		}
		String passphrase = event.getPassphrase();
		if (passphrase != null) {
			webSocketManager.sendPartyState("JOINED", passphrase);
		} else {
			webSocketManager.sendPartyState("LEFT", null);
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event) {
		if (!LinkConfig.CONFIG_GROUP.equals(event.getGroup())) {
			return;
		}
		switch (event.getKey()) {
			case "pairingKey":
				handlePairingKeyChange(event.getNewValue());
				break;
			case "enabled":
			case "websocketUrl":
				restart();
				break;
			case LinkConfig.BEARER_TOKEN_KEY:
				if (!hasToken() && webSocketManager != null) {
					webSocketManager.disconnect();
				}
				break;
		}
	}

	@Provides
	LinkConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(LinkConfig.class);
	}

	private String getPlayerName() {
		return client.getLocalPlayer() != null ? client.getLocalPlayer().getName() : null;
	}

	private void onJoinCommand() {
		if (commandExecutor == null || !commandExecutor.executePendingJoin()) {
			sendChatMessage("No pending party invitation.");
		}
	}

	private void changeParty(String passphrase) {
		clientThread.invokeLater(() -> partyService.changeParty(passphrase));
	}

	private String getStoredToken() {
		String token =
				configManager.getConfiguration(
						LinkConfig.CONFIG_GROUP, LinkConfig.BEARER_TOKEN_KEY);
		return token != null ? token : "";
	}

	private boolean hasToken() {
		return !getStoredToken().isEmpty();
	}

	private String getPairingCode() {
		String code = config.pairingKey();
		return code != null ? code : "";
	}

	private boolean hasPairingCode() {
		return !getPairingCode().isEmpty();
	}

	private void restart() {
		shutDown();
		startUp();
	}

	private void handlePairingKeyChange(String pairingKey) {
		if (pairingKey == null || pairingKey.isEmpty()) {
			if (hasToken()) {
				return;
			}
			if (webSocketManager != null) {
				webSocketManager.disconnect();
			}
			sendChatMessage("Party Link: Unpaired.");
			return;
		}
		restart();
	}

	private void onPairResult(String token) {
		if (token != null) {
			configManager.setConfiguration(
					LinkConfig.CONFIG_GROUP, LinkConfig.BEARER_TOKEN_KEY, token);
		}
		configManager.unsetConfiguration(LinkConfig.CONFIG_GROUP, "pairingKey");
		if (token == null) {
			sendChatMessage("Party Link: Pairing failed. Check your code and try again.");
		} else {
			sendChatMessage("Party Link: Paired successfully!");
		}
	}

	private void sendChatMessage(String message) {
		chatMessageManager.queue(
				QueuedMessage.builder()
						.type(ChatMessageType.GAMEMESSAGE)
						.runeLiteFormattedMessage("<col=ff7700>" + message + "</col>")
						.build());
	}
}
