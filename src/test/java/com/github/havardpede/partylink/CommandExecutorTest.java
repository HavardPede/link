package com.github.havardpede.partylink;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.junit.Before;
import org.junit.Test;

public class CommandExecutorTest {
	private List<String> changePartyCalls;
	private List<String> chatMessageCalls;
	private AtomicInteger ackCount;
	private boolean shouldThrowOnChangeParty;
	private CommandExecutor executor;

	@Before
	public void setUp() {
		changePartyCalls = new ArrayList<>();
		chatMessageCalls = new ArrayList<>();
		ackCount = new AtomicInteger(0);
		shouldThrowOnChangeParty = false;

		Consumer<String> spyChangeParty =
				passphrase -> {
					if (shouldThrowOnChangeParty) {
						throw new RuntimeException("PartyService error");
					}
					changePartyCalls.add(passphrase);
				};

		executor = new CommandExecutor(spyChangeParty, chatMessageCalls::add);
	}

	// JOIN_PARTY: execute() behaviour

	@Test
	public void joinPartyDoesNotCallChangePartyImmediately() {
		executor.execute(joinCommand("test-pass", null), ack());
		assertEquals(0, changePartyCalls.size());
	}

	@Test
	public void joinPartyDoesNotAckImmediately() {
		executor.execute(joinCommand("test-pass", null), ack());
		assertEquals(0, ackCount.get());
	}

	@Test
	public void joinPartySendsInviteMessage() {
		executor.execute(joinCommand("test-pass", null), ack());
		assertEquals(1, chatMessageCalls.size());
		assertEquals(
				"You have been invited to join a party. Type ::join to accept.",
				chatMessageCalls.get(0));
	}

	@Test
	public void joinPartySendsInviteMessageWithRole() {
		executor.execute(joinCommand("test-pass", "tank"), ack());
		assertEquals(
				"You have been invited to join a party as tank. Type ::join to accept.",
				chatMessageCalls.get(0));
	}

	@Test
	public void secondJoinOverwritesFirstPending() {
		executor.execute(joinCommand("old-pass", null), ack());
		executor.execute(joinCommand("new-pass", null), ack());
		executor.executePendingJoin();
		assertEquals(1, changePartyCalls.size());
		assertEquals("new-pass", changePartyCalls.get(0));
	}

	// JOIN_PARTY: executePendingJoin() behaviour

	@Test
	public void executePendingJoinCallsChangePartyWithPassphrase() {
		executor.execute(joinCommand("test-pass", null), ack());
		executor.executePendingJoin();
		assertEquals(1, changePartyCalls.size());
		assertEquals("test-pass", changePartyCalls.get(0));
	}

	@Test
	public void executePendingJoinSendsJoinedMessage() {
		executor.execute(joinCommand("test-pass", null), ack());
		executor.executePendingJoin();
		assertEquals(2, chatMessageCalls.size());
		assertEquals("You have joined the party.", chatMessageCalls.get(1));
	}

	@Test
	public void executePendingJoinSendsJoinedMessageWithRole() {
		executor.execute(joinCommand("test-pass", "healer"), ack());
		executor.executePendingJoin();
		assertEquals("You have joined the party. Your role is healer.", chatMessageCalls.get(1));
	}

	@Test
	public void executePendingJoinSendsAck() {
		executor.execute(joinCommand("test-pass", null), ack());
		executor.executePendingJoin();
		assertEquals(1, ackCount.get());
	}

	@Test
	public void executePendingJoinReturnsTrueWhenPending() {
		executor.execute(joinCommand("test-pass", null), ack());
		assertTrue(executor.executePendingJoin());
	}

	@Test
	public void executePendingJoinReturnsFalseWhenNoPending() {
		assertFalse(executor.executePendingJoin());
	}

	@Test
	public void executePendingJoinClearsPendingAfterExecution() {
		executor.execute(joinCommand("test-pass", null), ack());
		executor.executePendingJoin();
		assertFalse(executor.executePendingJoin());
	}

	// JOIN then LEAVE sequencing

	@Test
	public void joinThenLeaveCallsChangePartyWithNull() {
		executor.execute(joinCommand("test-pass", null), ack());
		executor.execute(leaveCommand("KICKED"), ack());
		assertEquals(1, changePartyCalls.size());
		assertNull(changePartyCalls.get(0));
	}

	@Test
	public void joinThenLeaveShowsOnlyLeaveMessage() {
		executor.execute(joinCommand("test-pass", null), ack());
		chatMessageCalls.clear();
		executor.execute(leaveCommand("KICKED"), ack());
		assertEquals(1, chatMessageCalls.size());
		assertEquals("You have been kicked from the party.", chatMessageCalls.get(0));
	}

	@Test
	public void joinThenLeaveAcksBothCommands() {
		executor.execute(joinCommand("test-pass", null), ack());
		executor.execute(leaveCommand(null), ack());
		assertEquals(2, ackCount.get());
	}

	@Test
	public void joinThenLeaveClearsPendingJoin() {
		executor.execute(joinCommand("test-pass", null), ack());
		executor.execute(leaveCommand(null), ack());
		assertFalse(executor.executePendingJoin());
	}

	// LEAVE_PARTY

	@Test
	public void leavePartyCallsChangePartyWithNull() {
		executor.execute(leaveCommand(null), ack());
		assertEquals(1, changePartyCalls.size());
		assertNull(changePartyCalls.get(0));
	}

	@Test
	public void leavePartyKickedSendsChatMessage() {
		executor.execute(leaveCommand("KICKED"), ack());
		assertEquals(1, chatMessageCalls.size());
		assertEquals("You have been kicked from the party.", chatMessageCalls.get(0));
	}

	@Test
	public void leavePartyClosedSendsChatMessage() {
		executor.execute(leaveCommand("CLOSED"), ack());
		assertEquals(1, chatMessageCalls.size());
		assertEquals("The party has been closed by the leader.", chatMessageCalls.get(0));
	}

	@Test
	public void leavePartyLeftSendsNoChatMessage() {
		executor.execute(leaveCommand("LEFT"), ack());
		assertEquals(0, chatMessageCalls.size());
	}

	@Test
	public void leavePartyNullReasonSendsNoChatMessage() {
		executor.execute(leaveCommand(null), ack());
		assertEquals(0, chatMessageCalls.size());
	}

	@Test
	public void leavePartyAcksImmediately() {
		executor.execute(leaveCommand(null), ack());
		assertEquals(1, ackCount.get());
	}

	// ROLE_CHANGE

	@Test
	public void roleChangeSendsChatMessage() {
		executor.execute(roleChangeCommand("healer"), ack());
		assertEquals(1, chatMessageCalls.size());
		assertEquals("Your role has been changed to healer.", chatMessageCalls.get(0));
	}

	@Test
	public void roleChangeDoesNotCallChangeParty() {
		executor.execute(roleChangeCommand("healer"), ack());
		assertEquals(0, changePartyCalls.size());
	}

	@Test
	public void roleChangeAcksImmediately() {
		executor.execute(roleChangeCommand("healer"), ack());
		assertEquals(1, ackCount.get());
	}

	// Exception handling

	@Test(expected = RuntimeException.class)
	public void exceptionInChangePartyPropagatesFromExecutePendingJoin() {
		shouldThrowOnChangeParty = true;
		executor.execute(joinCommand("test-pass", null), ack());
		executor.executePendingJoin();
	}

	// Unknown command

	@Test
	public void unknownCommandTypeIsIgnored() {
		executor.execute(commandFromJson("UNKNOWN_CMD", null, null, null), ack());
		assertEquals(0, changePartyCalls.size());
		assertEquals(0, chatMessageCalls.size());
	}

	private Runnable ack() {
		return ackCount::incrementAndGet;
	}

	private static Command joinCommand(String passphrase, String role) {
		return commandFromJson("JOIN_PARTY", passphrase, null, role);
	}

	private static Command leaveCommand(String reason) {
		return commandFromJson("LEAVE_PARTY", null, reason, null);
	}

	private static Command roleChangeCommand(String role) {
		return commandFromJson("ROLE_CHANGE", null, null, role);
	}

	private static Command commandFromJson(
			String commandType, String passphrase, String reason, String role) {
		StringBuilder json = new StringBuilder();
		json.append("{\"type\":\"COMMAND\",\"id\":\"cmd-1\",\"command\":\"");
		json.append(commandType);
		json.append("\",\"passphrase\":");
		json.append(passphrase != null ? "\"" + passphrase + "\"" : "null");
		json.append(",\"partyId\":\"p1\",\"reason\":");
		json.append(reason != null ? "\"" + reason + "\"" : "null");
		json.append(",\"role\":");
		json.append(role != null ? "\"" + role + "\"" : "null");
		json.append("}");
		return Command.fromJson(json.toString());
	}
}
