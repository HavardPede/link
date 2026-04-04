package com.github.havardpede.partylink;

class FakeConfig implements LinkConfig {
	String websocketUrl;
	boolean isEnabled;

	FakeConfig(String websocketUrl, boolean isEnabled) {
		this.websocketUrl = websocketUrl;
		this.isEnabled = isEnabled;
	}

	@Override
	public String websocketUrl() {
		return websocketUrl;
	}

	@Override
	public String pairingKey() {
		return "";
	}

	@Override
	public boolean enabled() {
		return isEnabled;
	}
}
