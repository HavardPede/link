package com.example.link;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
class LinkApiClient {
	private static final MediaType JSON = MediaType.parse("application/json");
	private static final RequestBody EMPTY_JSON = RequestBody.create(JSON, "{}");
	private static final Pattern SAFE_ID = Pattern.compile("^[a-zA-Z0-9_-]+$");

	private final OkHttpClient httpClient;
	private final LinkConfig config;
	private final String baseUrl;

	LinkApiClient(OkHttpClient httpClient, LinkConfig config) {
		this.httpClient = httpClient;
		this.config = config;
		this.baseUrl = validateServerUrl(config.serverUrl()) + "/api/plugin";
	}

	private static String validateServerUrl(String url) {
		if (url == null || url.isEmpty()) {
			throw new IllegalArgumentException("Server URL must not be empty");
		}
		if (!url.startsWith("https://") && !url.startsWith("http://")) {
			throw new IllegalArgumentException("Server URL must use http or https: " + url);
		}
		return url;
	}

	String fetchCommands(String playerName) throws IOException {
		Request request = buildRequest("/commands", playerName).build();

		try (Response response = httpClient.newCall(request).execute()) {
			if (!response.isSuccessful()) {
				throw new IOException("HTTP " + response.code());
			}
			return response.body() != null ? response.body().string() : "{}";
		}
	}

	void ackCommand(String commandId, String playerName) {
		if (!SAFE_ID.matcher(commandId).matches()) {
			log.warn("Rejected invalid command ID: {}", commandId);
			return;
		}

		Request request =
				buildRequest("/commands/" + commandId + "/ack", playerName)
						.post(EMPTY_JSON)
						.build();

		try (Response response = httpClient.newCall(request).execute()) {
			if (!response.isSuccessful()) {
				log.warn("ACK failed for command {} (HTTP {})", commandId, response.code());
			}
		} catch (IOException e) {
			log.warn("ACK request failed for command {}: {}", commandId, e.getMessage());
		}
	}

	void postRsn(String playerName) {
		if (!hasToken()) {
			log.warn("Cannot post RSN: bearer token is empty");
			return;
		}

		JsonObject body = new JsonObject();
		body.addProperty("playerName", playerName);

		Request request =
				buildRequest("/rsn", playerName)
						.post(RequestBody.create(JSON, body.toString()))
						.build();

		try (Response response = httpClient.newCall(request).execute()) {
			if (response.isSuccessful()) {
				log.info("RSN registered: {}", playerName);
			} else {
				log.warn("RSN registration failed (HTTP {})", response.code());
			}
		} catch (IOException e) {
			log.warn("RSN registration request failed: {}", e.getMessage());
		}
	}

	boolean hasToken() {
		String token = config.bearerToken();
		return token != null && !token.isEmpty();
	}

	private Request.Builder buildRequest(String path, String playerName) {
		Request.Builder builder =
				new Request.Builder()
						.url(baseUrl + path)
						.header("Authorization", "Bearer " + config.bearerToken());
		if (playerName != null) {
			builder.header("X-Player-Name", playerName);
		}
		return builder;
	}
}
