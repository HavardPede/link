package com.github.havardpede.partylink;

import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class CommandExecutor {
	private final Consumer<String> changeParty;
	private final Consumer<String> sendChatMessage;

	private volatile Command pendingJoin;
	private volatile Runnable pendingAck;

	CommandExecutor(Consumer<String> changeParty, Consumer<String> sendChatMessage) {
		this.changeParty = changeParty;
		this.sendChatMessage = sendChatMessage;
	}

	void execute(Command command, Runnable ack) {
		if (command.type == null) {
			log.warn("Unknown command type (command={})", command.id);
			return;
		}

		switch (command.type) {
			case JOIN_PARTY:
				log.info("Queueing JOIN_PARTY (command={}, role={})", command.id, command.role);
				pendingJoin = command;
				pendingAck = ack;
				sendChatMessage.accept(buildInviteMessage(command.role));
				break;
			case LEAVE_PARTY:
				log.info(
						"Executing LEAVE_PARTY (command={}, reason={})",
						command.id,
						command.reason);
				clearPendingJoin();
				changeParty.accept(null);
				sendLeaveMessage(command.reason);
				ack.run();
				break;
			case ROLE_CHANGE:
				log.info("Executing ROLE_CHANGE (command={}, role={})", command.id, command.role);
				sendChatMessage.accept("Your role has been changed to " + command.role + ".");
				ack.run();
				break;
		}
	}

	boolean executePendingJoin() {
		Command cmd = pendingJoin;
		Runnable ack = pendingAck;
		if (cmd == null) {
			return false;
		}
		pendingJoin = null;
		pendingAck = null;
		log.info("Player accepted JOIN_PARTY (command={}, role={})", cmd.id, cmd.role);
		changeParty.accept(cmd.passphrase);
		sendChatMessage.accept(buildJoinedMessage(cmd.role));
		ack.run();
		return true;
	}

	private void clearPendingJoin() {
		Runnable ack = pendingAck;
		pendingJoin = null;
		pendingAck = null;
		if (ack != null) {
			ack.run();
		}
	}

	private static String buildInviteMessage(String role) {
		if (role != null) {
			return "You have been invited to join a party as "
					+ role
					+ ". Type ::join to accept.";
		}
		return "You have been invited to join a party. Type ::join to accept.";
	}

	private static String buildJoinedMessage(String role) {
		if (role != null) {
			return "You have joined the party. Your role is " + role + ".";
		}
		return "You have joined the party.";
	}

	private void sendLeaveMessage(LeaveReason reason) {
		if (reason == LeaveReason.KICKED) {
			sendChatMessage.accept("You have been kicked from the party.");
		} else if (reason == LeaveReason.CLOSED) {
			sendChatMessage.accept("The party has been closed by the leader.");
		}
	}
}
