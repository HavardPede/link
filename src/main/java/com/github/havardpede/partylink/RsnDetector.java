package com.github.havardpede.partylink;

import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class RsnDetector {
	private final Consumer<String> onNameDetected;
	private final Supplier<String> playerNameSupplier;

	private boolean pending;
	private String detectedName;

	RsnDetector(Consumer<String> onNameDetected, Supplier<String> playerNameSupplier) {
		this.onNameDetected = onNameDetected;
		this.playerNameSupplier = playerNameSupplier;
	}

	void onLoggedIn() {
		pending = true;
		tryDetect(true);
	}

	void onGameTick() {
		if (!pending) {
			return;
		}
		tryDetect(false);
	}

	String getDetectedName() {
		return detectedName;
	}

	private void tryDetect(boolean canRetry) {
		String name = playerNameSupplier.get();
		if (name != null && !name.isEmpty()) {
			detectedName = name;
			pending = false;
			onNameDetected.accept(name);
		} else if (!canRetry) {
			log.warn("Could not detect RSN after login");
			pending = false;
		}
	}
}
