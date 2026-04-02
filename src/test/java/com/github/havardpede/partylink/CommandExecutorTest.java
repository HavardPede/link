package com.github.havardpede.partylink;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.Before;
import org.junit.Test;

public class CommandExecutorTest {
	private List<String> changePartyCalls;
	private List<String> chatMessageCalls;
	private boolean shouldThrowOnChangeParty;
	private CommandExecutor executor;

	@Before
	public void setUp() {
		changePartyCalls = new ArrayList<>();
		chatMessageCalls = new ArrayList<>();
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

	@Test
	public void joinPartyCallsChangePartyWithPassphrase() {
		executor.execute(commandFromJson("JOIN_PARTY", "test-pass", null));

		assertEquals(1, changePartyCalls.size());
		assertEquals("test-pass", changePartyCalls.get(0));
	}

	@Test
	public void leavePartyCallsChangePartyWithNull() {
		executor.execute(commandFromJson("LEAVE_PARTY", null, null));

		assertEquals(1, changePartyCalls.size());
		assertNull(changePartyCalls.get(0));
	}

	@Test
	public void exceptionInChangePartyDoesNotPropagate() {
		shouldThrowOnChangeParty = true;

		executor.execute(commandFromJson("JOIN_PARTY", "test-pass", null));

		assertEquals(0, changePartyCalls.size());
	}

	@Test
	public void unknownCommandTypeIsIgnored() {
		executor.execute(commandFromJson("UNKNOWN_CMD", null, null));

		assertEquals(0, changePartyCalls.size());
	}

	@Test
	public void joinPartySendsChatMessage() {
		executor.execute(commandFromJson("JOIN_PARTY", "test-pass", null));

		assertEquals(1, chatMessageCalls.size());
		assertEquals("You have joined the party.", chatMessageCalls.get(0));
	}

	@Test
	public void leavePartyKickedSendsChatMessage() {
		executor.execute(commandFromJson("LEAVE_PARTY", null, "KICKED"));

		assertEquals(1, chatMessageCalls.size());
		assertEquals("You have been kicked from the party.", chatMessageCalls.get(0));
	}

	@Test
	public void leavePartyClosedSendsChatMessage() {
		executor.execute(commandFromJson("LEAVE_PARTY", null, "CLOSED"));

		assertEquals(1, chatMessageCalls.size());
		assertEquals("The party has been closed by the leader.", chatMessageCalls.get(0));
	}

	@Test
	public void leavePartyLeftSendsNoChatMessage() {
		executor.execute(commandFromJson("LEAVE_PARTY", null, "LEFT"));

		assertEquals(0, chatMessageCalls.size());
	}

	@Test
	public void leavePartyNullReasonSendsNoChatMessage() {
		executor.execute(commandFromJson("LEAVE_PARTY", null, null));

		assertEquals(0, chatMessageCalls.size());
	}

	private static Command commandFromJson(String commandType, String passphrase, String reason) {
		StringBuilder json = new StringBuilder();
		json.append("{\"type\":\"COMMAND\",\"id\":\"cmd-1\",\"command\":\"");
		json.append(commandType);
		json.append("\",\"passphrase\":");
		json.append(passphrase != null ? "\"" + passphrase + "\"" : "null");
		json.append(",\"partyId\":\"p1\",\"reason\":");
		json.append(reason != null ? "\"" + reason + "\"" : "null");
		json.append("}");
		return Command.fromJson(json.toString());
	}
}
