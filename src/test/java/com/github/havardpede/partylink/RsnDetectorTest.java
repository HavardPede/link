package com.github.havardpede.partylink;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class RsnDetectorTest {
	private List<String> detectedNames;

	@Before
	public void setUp() {
		detectedNames = new ArrayList<>();
	}

	private RsnDetector buildDetector(String nameOnLogin, String nameOnTick) {
		boolean[] loginCalled = {false};
		return new RsnDetector(
				detectedNames::add,
				() -> {
					if (!loginCalled[0]) {
						loginCalled[0] = true;
						return nameOnLogin;
					}
					return nameOnTick;
				});
	}

	@Test
	public void nameAvailableAtLoginCallsBackImmediately() {
		RsnDetector detector = buildDetector("Zezima", null);

		detector.onLoggedIn();

		assertEquals("Zezima", detector.getDetectedName());
		assertEquals(1, detectedNames.size());
		assertEquals("Zezima", detectedNames.get(0));
	}

	@Test
	public void nameNullAtLoginFoundOnNextTick() {
		RsnDetector detector = buildDetector(null, "Zezima");

		detector.onLoggedIn();
		detector.onGameTick();

		assertEquals("Zezima", detector.getDetectedName());
		assertEquals(1, detectedNames.size());
	}

	@Test
	public void nameNullAtLoginAndTickGivesUp() {
		RsnDetector detector = buildDetector(null, null);

		detector.onLoggedIn();
		detector.onGameTick();

		assertNull(detector.getDetectedName());
		assertEquals(0, detectedNames.size());
	}

	@Test
	public void gameTickIsNoOpWhenNotPending() {
		RsnDetector detector = buildDetector("Zezima", null);

		detector.onLoggedIn();
		detector.onGameTick();
		detector.onGameTick();

		assertEquals(1, detectedNames.size());
	}

	@Test
	public void secondLoginResetsDetection() {
		RsnDetector detector = buildDetector(null, null);

		detector.onLoggedIn();
		detector.onGameTick();
		assertNull(detector.getDetectedName());

		RsnDetector detector2 = new RsnDetector(detectedNames::add, () -> "Retry");
		detector2.onLoggedIn();

		assertEquals("Retry", detector2.getDetectedName());
		assertEquals(1, detectedNames.size());
	}

	@Test
	public void reloginWithSameNameCallsBackAgain() {
		RsnDetector detector = new RsnDetector(detectedNames::add, () -> "Zezima");

		detector.onLoggedIn();
		assertEquals(1, detectedNames.size());

		detector.onLoggedIn();
		assertEquals(2, detectedNames.size());
	}

	@Test
	public void reloginWithDifferentNameCallsBackAgain() {
		String[] name = {"Zezima"};
		RsnDetector detector = new RsnDetector(detectedNames::add, () -> name[0]);

		detector.onLoggedIn();
		assertEquals(1, detectedNames.size());

		name[0] = "Lynx Titan";
		detector.onLoggedIn();
		assertEquals(2, detectedNames.size());
		assertEquals("Lynx Titan", detectedNames.get(1));
	}
}
