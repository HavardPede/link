package com.github.havardpede.partylink;

import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class CommandExecutor {
	private final Consumer<String> changeParty;
	private final Consumer<String> sendChatMessage;

	CommandExecutor(Consumer<String> changeParty, Consumer<String> sendChatMessage) {
		this.changeParty = changeParty;
		this.sendChatMessage = sendChatMessage;
	}

	void execute(Command command) {
		if (command.type == null) {
			log.warn("Unknown command type (command={})", command.id);
			return;
		}

		try {
			switch (command.type) {
				case JOIN_PARTY:
					log.info("Executing JOIN_PARTY (command={})", command.id);
					changeParty.accept(command.passphrase);
					sendChatMessage.accept("You have joined the party.");
					break;
				case LEAVE_PARTY:
					log.info(
							"Executing LEAVE_PARTY (command={}, reason={})",
							command.id,
							command.reason);
					if (command.reason == LeaveReason.KICKED) {
						sendChatMessage.accept("You have been kicked from the party.");
					} else if (command.reason == LeaveReason.CLOSED) {
						sendChatMessage.accept("The party has been closed by the leader.");
					}
					changeParty.accept(null);
					break;
			}
		} catch (RuntimeException e) {
			log.error(
					"Failed to execute command {} (type={}): {}",
					command.id,
					command.type,
					e.getMessage());
		}
	}
}
