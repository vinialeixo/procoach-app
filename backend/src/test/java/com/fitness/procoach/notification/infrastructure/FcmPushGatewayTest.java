package com.fitness.procoach.notification.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/** Provedor FCM HTTP v1: requisição autenticada, token inválido e falha transitória. */
class FcmPushGatewayTest {

	private final ObjectMapper mapper = JsonMapper.builder().build();

	private HttpServer server;

	@AfterEach
	void stopServer() {
		if (server != null) {
			server.stop(0);
		}
	}

	private record Captured(String path, String authorization, String body) {
	}

	private static final class FakeGoogle {

		final List<Captured> requests = new CopyOnWriteArrayList<>();

		final Map<String, Reply> replies = new ConcurrentHashMap<>();

		record Reply(int status, String body) {
		}

	}

	private String start(FakeGoogle google) throws Exception {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", exchange -> {
			String path = exchange.getRequestURI().getPath();
			String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
			google.requests.add(new Captured(path,
					exchange.getRequestHeaders().getFirst("Authorization"), body));
			FakeGoogle.Reply reply = google.replies.getOrDefault(path, new FakeGoogle.Reply(200, "{}"));
			byte[] bytes = reply.body().getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().set("Content-Type", "application/json");
			exchange.sendResponseHeaders(reply.status(), bytes.length);
			exchange.getResponseBody().write(bytes);
			exchange.close();
		});
		server.start();
		return "http://127.0.0.1:" + server.getAddress().getPort();
	}

	private FcmPushGateway gateway(String endpoint, PushAccessTokenProvider tokens) {
		return new FcmPushGateway("projeto-teste", endpoint, mapper, HttpClient.newHttpClient(), tokens);
	}

	@Test
	void enviaMensagemAutenticadaParaOMessageSendDoProjeto() throws Exception {
		FakeGoogle google = new FakeGoogle();
		String endpoint = start(google);

		gateway(endpoint, () -> "token-oauth").send("tok-dispositivo", "ProCoach", "Seu treino mudou.");

		assertThat(google.requests).hasSize(1);
		Captured sent = google.requests.get(0);
		assertThat(sent.path()).isEqualTo("/v1/projects/projeto-teste/messages:send");
		assertThat(sent.authorization()).isEqualTo("Bearer token-oauth");
		JsonNode body = mapper.readTree(sent.body());
		assertThat(body.get("message").get("token").asText()).isEqualTo("tok-dispositivo");
		assertThat(body.get("message").get("notification").get("title").asText()).isEqualTo("ProCoach");
		assertThat(body.get("message").get("notification").get("body").asText()).isEqualTo("Seu treino mudou.");
	}

	@Test
	void tokenUnregisteredViraInvalidPushTokenException() throws Exception {
		FakeGoogle google = new FakeGoogle();
		google.replies.put("/v1/projects/projeto-teste/messages:send", new FakeGoogle.Reply(404,
				"{\"error\":{\"status\":\"NOT_FOUND\",\"details\":[{\"errorCode\":\"UNREGISTERED\"}]}}"));
		String endpoint = start(google);

		assertThatThrownBy(() -> gateway(endpoint, () -> "token-oauth").send("tok-morto", "ProCoach", "oi"))
			.isInstanceOf(InvalidPushTokenException.class)
			.hasMessageContaining("UNREGISTERED");
	}

	@Test
	void erroTransitorioNaoEhTratadoComoTokensInvalido() throws Exception {
		FakeGoogle google = new FakeGoogle();
		google.replies.put("/v1/projects/projeto-teste/messages:send",
				new FakeGoogle.Reply(503, "{\"error\":{\"status\":\"UNAVAILABLE\"}}"));
		String endpoint = start(google);

		assertThatThrownBy(() -> gateway(endpoint, () -> "token-oauth").send("tok-bom", "ProCoach", "oi"))
			.isInstanceOf(IllegalStateException.class)
			.isNotInstanceOf(InvalidPushTokenException.class)
			.hasMessageContaining("503");
	}

	@Test
	void trocaJwtDaContaDeServicoPorTokenDeAcesso() throws Exception {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		KeyPair keys = generator.generateKeyPair();
		FakeGoogle google = new FakeGoogle();
		String endpoint = start(google);
		String pem = "-----BEGIN PRIVATE KEY-----\n"
				+ Base64.getMimeEncoder(64, new byte[] { '\n' }).encodeToString(keys.getPrivate().getEncoded())
				+ "\n-----END PRIVATE KEY-----\n";
		ObjectNode serviceAccount = mapper.createObjectNode();
		serviceAccount.put("client_email", "svc@projeto-teste.iam.gserviceaccount.com");
		serviceAccount.put("private_key", pem);
		serviceAccount.put("token_uri", endpoint + "/token");
		serviceAccount.put("project_id", "projeto-teste");
		google.replies.put("/token", new FakeGoogle.Reply(200, "{\"access_token\":\"acesso\",\"expires_in\":3600}"));
		FcmPushGateway gateway = new FcmPushGateway(
				GoogleServiceAccount.load(mapper.writeValueAsString(serviceAccount), null), endpoint, mapper,
				HttpClient.newHttpClient());

		gateway.send("tok-dispositivo", "ProCoach", "oi");

		assertThat(google.requests).extracting(Captured::path).containsExactly("/token",
				"/v1/projects/projeto-teste/messages:send");
		assertThat(google.requests.get(1).authorization()).isEqualTo("Bearer acesso");
		String form = google.requests.get(0).body();
		String assertion = form.substring(form.indexOf("assertion=") + "assertion=".length());
		assertThat(assertion).contains(".");
		String[] parts = assertion.split("\\.");
		assertThat(parts).hasSize(3);
		String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
		assertThat(payload).contains("svc@projeto-teste.iam.gserviceaccount.com")
			.contains("https://www.googleapis.com/auth/firebase.messaging")
			.contains(endpoint + "/token");
		Signature verifier = Signature.getInstance("SHA256withRSA");
		verifier.initVerify(keys.getPublic());
		verifier.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
		assertThat(verifier.verify(Base64.getUrlDecoder().decode(parts[2]))).isTrue();
	}

	@Test
	void tokenFicaEmCacheAteExpirar() throws Exception {
		FakeGoogle google = new FakeGoogle();
		String endpoint = start(google);
		google.replies.put("/token", new FakeGoogle.Reply(200, "{\"access_token\":\"acesso\",\"expires_in\":3600}"));
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		KeyPair keys = generator.generateKeyPair();
		GoogleServiceAccount account = new GoogleServiceAccount("svc@projeto-teste.iam.gserviceaccount.com",
				keys.getPrivate(), endpoint + "/token", "projeto-teste");
		GoogleServiceAccountTokenProvider tokens = new GoogleServiceAccountTokenProvider(account, mapper,
				HttpClient.newHttpClient(), Clock.fixed(Instant.parse("2026-11-25T10:00:00Z"), ZoneOffset.UTC));

		assertThat(tokens.accessToken()).isEqualTo("acesso");
		assertThat(tokens.accessToken()).isEqualTo("acesso");
		assertThat(google.requests).extracting(Captured::path).containsExactly("/token");
	}

}
