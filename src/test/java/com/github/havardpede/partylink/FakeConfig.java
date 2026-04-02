package com.github.havardpede.partylink;

class FakeConfig implements LinkConfig {
	String serverUrl;
	String websocketUrl;
	boolean isEnabled;

	FakeConfig(String serverUrl, String websocketUrl, boolean isEnabled) {
		this.serverUrl = serverUrl;
		this.websocketUrl = websocketUrl;
		this.isEnabled = isEnabled;
	}

	@Override
	public String serverUrl() {
		return serverUrl;
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
