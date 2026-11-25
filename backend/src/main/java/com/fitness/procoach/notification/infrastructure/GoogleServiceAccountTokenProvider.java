package com.fitness.procoach.notification.infrastructure;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Obtém o token OAuth2 do FCM a partir da conta de serviço: assina um JWT RS256 e o troca no
 * endpoint de token do Google. O token fica em cache até pouco antes de expirar.
 */
final class GoogleServiceAccountTokenProvider implements PushAccessTokenProvider {

	private static final String SCOPE = "https://www.googleapis.com/auth/firebase.messaging";

	private static final String GRANT_TYPE = "urn:ietf:params:oauth:grant-type:jwt-bearer";

	private static final Duration MARGIN = Duration.ofSeconds(60);

	private final GoogleServiceAccount account;

	private final ObjectMapper mapper;

	private final HttpClient http;

	private final Clock clock;

	private String token;

	private Instant expiresAt = Instant.EPOCH;

	GoogleServiceAccountTokenProvider(GoogleServiceAccount account, ObjectMapper mapper, HttpClient http,
			Clock clock) {
		this.account = account;
		this.mapper = mapper;
		this.http = http;
		this.clock = clock;
	}

	@Override
	public synchronized String accessToken() {
		Instant now = clock.instant();
		if (token != null && now.isBefore(expiresAt)) {
			return token;
		}
		String body = "grant_type=" + form(GRANT_TYPE) + "&assertion=" + form(assertion(now));
		HttpRequest request = HttpRequest.newBuilder(URI.create(account.tokenUri()))
			.header("Content-Type", "application/x-www-form-urlencoded")
			.timeout(Duration.ofSeconds(10))
			.POST(HttpRequest.BodyPublishers.ofString(body))
			.build();
		HttpResponse<String> response;
		try {
			response = http.send(request, HttpResponse.BodyHandlers.ofString());
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("interrompido ao pedir token do FCM", ex);
		}
		catch (Exception ex) {
			throw new IllegalStateException("falha ao pedir token do FCM: " + ex.getMessage(), ex);
		}
		if (response.statusCode() / 100 != 2) {
			throw new IllegalStateException("FCM recusou as credenciais (" + response.statusCode() + "): "
					+ response.body());
		}
		JsonNode json = mapper.readTree(response.body());
		JsonNode accessToken = json.get("access_token");
		if (accessToken == null || accessToken.asText().isBlank()) {
			throw new IllegalStateException("resposta do token do FCM sem access_token");
		}
		long expiresIn = json.hasNonNull("expires_in") ? json.get("expires_in").asLong() : 3600;
		token = accessToken.asText();
		expiresAt = now.plusSeconds(expiresIn).minus(MARGIN);
		return token;
	}

	private String assertion(Instant now) {
		String header = base64Url("{\"alg\":\"RS256\",\"typ\":\"JWT\"}");
		String payload = base64Url("{\"iss\":\"" + account.clientEmail() + "\",\"scope\":\"" + SCOPE + "\",\"aud\":\""
				+ account.tokenUri() + "\",\"iat\":" + now.getEpochSecond() + ",\"exp\":"
				+ now.plusSeconds(3600).getEpochSecond() + "}");
		String signingInput = header + "." + payload;
		try {
			Signature signature = Signature.getInstance("SHA256withRSA");
			signature.initSign(account.privateKey());
			signature.update(signingInput.getBytes(StandardCharsets.US_ASCII));
			return signingInput + "." + base64Url(signature.sign());
		}
		catch (GeneralSecurityException ex) {
			throw new IllegalStateException("falha ao assinar o JWT da conta de serviço do FCM", ex);
		}
	}

	private static String base64Url(String value) {
		return base64Url(value.getBytes(StandardCharsets.UTF_8));
	}

	private static String base64Url(byte[] value) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
	}

	private static String form(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}
}
