package com.github.havardpede.partylink;

import com.google.gson.JsonObject;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
class LinkApiClient {
	private static final MediaType JSON = MediaType.parse("application/json");

	private LinkApiClient() {}

	static String pair(OkHttpClient httpClient, String serverUrl, String pairingKey)
			throws IOException {
		String url = validateServerUrl(serverUrl) + "/api/plugin/pair";
		JsonObject body = new JsonObject();
		body.addProperty("code", pairingKey);

		Request request =
				new Request.Builder()
						.url(url)
						.post(RequestBody.create(JSON, body.toString()))
						.build();

		try (Response response = httpClient.newCall(request).execute()) {
			if (!response.isSuccessful()) {
				throw new IOException("Pairing failed (HTTP " + response.code() + ")");
			}
			String responseBody = response.body() != null ? response.body().string() : "";
			JsonObject json =
					new com.google.gson.JsonParser().parse(responseBody).getAsJsonObject();
			if (!json.has("token")) {
				throw new IOException("Invalid pairing response from server");
			}
			return json.get("token").getAsString();
		}
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
}
