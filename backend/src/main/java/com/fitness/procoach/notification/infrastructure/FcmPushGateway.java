package com.fitness.procoach.notification.infrastructure;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Provedor de push real via FCM HTTP v1 ({@code procoach.notification.push.provider=fcm}). Vale
 * para Android, iOS e Web Push, que o FCM encaminha ao APNs/navegador. As credenciais vêm da
 * conta de serviço por ambiente ({@code procoach.notification.push.fcm.credentials}) e o projeto
 * de {@code procoach.notification.push.fcm.project-id} (ou do próprio JSON). Tokens recusados como
 * definitivos levantam {@link InvalidPushTokenException}, para o canal removê-los.
 */
@Component
@ConditionalOnProperty(name = "procoach.notification.push.provider", havingValue = "fcm")
public class FcmPushGateway implements PushGateway {

	private static final Duration TIMEOUT = Duration.ofSeconds(10);

	private static final java.util.Set<String> INVALID_TOKEN_CODES = java.util.Set.of("UNREGISTERED",
			"SENDER_ID_MISMATCH", "registration-token-not-registered");

	private final String projectId;

	private final String endpoint;

	private final ObjectMapper mapper;

	private final HttpClient http;

	private final PushAccessTokenProvider tokens;

	@Autowired
	public FcmPushGateway(@Value("${procoach.notification.push.fcm.credentials:}") String credentials,
			@Value("${procoach.notification.push.fcm.project-id:}") String projectId,
			@Value("${procoach.notification.push.fcm.endpoint:https://fcm.googleapis.com}") String endpoint) {
		this(GoogleServiceAccount.load(credentials, projectId), endpoint, JsonMapper.builder().build(),
				HttpClient.newBuilder().connectTimeout(TIMEOUT).build());
	}

	FcmPushGateway(GoogleServiceAccount account, String endpoint, ObjectMapper mapper, HttpClient http) {
		this(account.projectId(), endpoint, mapper, http,
				new GoogleServiceAccountTokenProvider(account, mapper, http, Clock.systemUTC()));
	}

	FcmPushGateway(String projectId, String endpoint, ObjectMapper mapper, HttpClient http,
			PushAccessTokenProvider tokens) {
		this.projectId = projectId;
		this.endpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
		this.mapper = mapper;
		this.http = http;
		this.tokens = tokens;
	}

	@Override
	public void send(String deviceToken, String title, String body) {
		ObjectNode message = mapper.createObjectNode();
		ObjectNode payload = message.putObject("message");
		payload.put("token", deviceToken);
		ObjectNode notification = payload.putObject("notification");
		notification.put("title", title);
		notification.put("body", body);
		HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint + "/v1/projects/" + projectId + "/messages:send"))
			.header("Authorization", "Bearer " + tokens.accessToken())
			.header("Content-Type", "application/json")
			.timeout(TIMEOUT)
			.POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(message)))
			.build();
		HttpResponse<String> response;
		try {
			response = http.send(request, HttpResponse.BodyHandlers.ofString());
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("interrompido ao enviar push pelo FCM", ex);
		}
		catch (Exception ex) {
			throw new IllegalStateException("falha ao enviar push pelo FCM: " + ex.getMessage(), ex);
		}
		int status = response.statusCode();
		if (status / 100 == 2) {
			return;
		}
		String reply = response.body() == null ? "" : response.body();
		if (invalidToken(reply)) {
			throw new InvalidPushTokenException(deviceToken, "FCM respondeu " + status + ": " + reply);
		}
		throw new IllegalStateException("FCM respondeu " + status + ": " + reply);
	}

	/**
	 * Só trata como token inválido a assinatura específica do FCM. Um {@code INVALID_ARGUMENT}
	 * de payload (mensagem grande/inválida) ou um 404 de projeto errado não pode purgar um
	 * dispositivo válido.
	 */
	private boolean invalidToken(String reply) {
		JsonNode error;
		try {
			JsonNode root = mapper.readTree(reply);
			error = root == null ? null : root.get("error");
		}
		catch (RuntimeException ex) {
			return false;
		}
		if (error == null) {
			return false;
		}
		for (JsonNode detail : error.path("details")) {
			if (INVALID_TOKEN_CODES.contains(detail.path("errorCode").asText(""))) {
				return true;
			}
		}
		String message = error.path("message").asText("");
		if (message.toLowerCase(java.util.Locale.ROOT).contains("registration token")) {
			return true;
		}
		// Resposta mínima de token malformado: INVALID_ARGUMENT sem diagnóstico de payload.
		return "INVALID_ARGUMENT".equals(error.path("status").asText("")) && message.isBlank();
	}
}
