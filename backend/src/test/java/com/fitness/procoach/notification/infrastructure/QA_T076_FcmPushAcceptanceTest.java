package com.fitness.procoach.notification.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fitness.procoach.notification.application.NotificationChannel;
import com.fitness.procoach.notification.application.NotificationDispatcher;
import com.fitness.procoach.notification.domain.DevicePlatform;
import com.fitness.procoach.notification.domain.DeviceToken;
import com.fitness.procoach.notification.domain.DeviceTokenRepository;
import com.fitness.procoach.notification.domain.Delivery;
import com.fitness.procoach.notification.domain.DeliveryRepository;
import com.fitness.procoach.notification.domain.DeliveryStatus;
import com.fitness.procoach.notification.domain.Notification;
import com.fitness.procoach.notification.domain.NotificationChannelType;
import com.fitness.procoach.notification.domain.NotificationPreferenceRepository;
import com.fitness.procoach.notification.domain.NotificationRepository;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * QA independente da T-076 — provedor de push real (FCM HTTP v1) como implementação de
 * {@link PushGateway}, credenciais por ambiente, seleção por configuração, remoção de tokens
 * inválidos e falha de push tratada como falha de entrega (nunca derruba a ação do coach).
 *
 * <p>Testes derivados do escopo da T-076 e das regras inegociáveis do projeto — não do código.
 */
class QA_T076_FcmPushAcceptanceTest {

	private static final ObjectMapper MAPPER = JsonMapper.builder().build();

	private static final KeyPair SERVICE_ACCOUNT_KEYS = generateKeys();

	private static final String CLIENT_EMAIL = "procoach-push@projeto-teste.iam.gserviceaccount.com";

	private HttpServer server;

	@AfterEach
	void stopServer() {
		if (server != null) {
			server.stop(0);
			server = null;
		}
	}

	// ------------------------------------------------------------------
	// Critério: provedor real selecionado por configuração (fcm x log x none)
	// ------------------------------------------------------------------

	@Test
	void fcmSubstituiOProvedorLogQuandoConfigurado() {
		try (AnnotationConfigApplicationContext context = gatewayContext(
				"procoach.notification.push.provider=fcm",
				"procoach.notification.push.fcm.credentials=" + inlineServiceAccount(null, null))) {
			assertThat(context.getBeansOfType(PushGateway.class)).hasSize(1);
			assertThat(context.getBean(PushGateway.class)).isInstanceOf(FcmPushGateway.class);
			assertThat(context.getBeansOfType(LoggingPushGateway.class)).isEmpty();
		}
	}

	@Test
	void provedorLogContinuaDisponivelComoFallback() {
		try (AnnotationConfigApplicationContext context = gatewayContext(
				"procoach.notification.push.provider=log")) {
			assertThat(context.getBeansOfType(PushGateway.class)).hasSize(1);
			assertThat(context.getBean(PushGateway.class)).isInstanceOf(LoggingPushGateway.class);
			assertThat(context.getBeansOfType(FcmPushGateway.class)).isEmpty();
		}
	}

	@Test
	void semProvedorConfiguradoNenhumPushEhInstalado() {
		try (AnnotationConfigApplicationContext context = gatewayContext()) {
			assertThat(context.getBeansOfType(PushGateway.class)).isEmpty();
		}
	}

	@Test
	void fcmSemCredencialFalhaRapidoNaSubidaEmVezDeSubirSemPush() {
		assertThatThrownBy(() -> gatewayContext("procoach.notification.push.provider=fcm").close())
			.satisfies(ex -> assertThat(rootCause(ex)).isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("procoach.notification.push.fcm.credentials"));
	}

	// ------------------------------------------------------------------
	// Critério: credenciais por ambiente (JSON inline, arquivo, project-id, endpoint)
	// ------------------------------------------------------------------

	@Test
	void credencialEmArquivoJsonEhLidaDoCaminhoDoAmbiente(@TempDir Path temp) throws Exception {
		Path credentials = temp.resolve("service-account.json");
		Files.writeString(credentials, inlineServiceAccount(null, null), StandardCharsets.UTF_8);

		GoogleServiceAccount account = GoogleServiceAccount.load(credentials.toString(), null);

		assertThat(account.clientEmail()).isEqualTo(CLIENT_EMAIL);
		assertThat(account.projectId()).isEqualTo("projeto-teste");
		assertThat(account.tokenUri()).isEqualTo("https://oauth2.googleapis.com/token");
		assertThat(account.privateKey()).isNotNull();
	}

	@Test
	void projectIdDoAmbienteSobrescreveODoJson(@TempDir Path temp) throws Exception {
		Path credentials = temp.resolve("service-account.json");
		Files.writeString(credentials, inlineServiceAccount(null, null), StandardCharsets.UTF_8);

		GoogleServiceAccount account = GoogleServiceAccount.load(credentials.toString(), "projeto-de-producao");

		assertThat(account.projectId()).isEqualTo("projeto-de-producao");
	}

	@Test
	void credencialAusenteExplicaQualConfiguracaoFalta() {
		assertThatThrownBy(() -> GoogleServiceAccount.load("   ", null))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("procoach.notification.push.fcm.credentials");
	}

	@Test
	void credencialInvalidaFalhaComMensagemDeConfiguracao() {
		assertThatThrownBy(() -> GoogleServiceAccount.load("{nao-e-json", null))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("JSON válido");
		assertThatThrownBy(() -> GoogleServiceAccount.load("{\"client_email\":\"" + CLIENT_EMAIL
				+ "\",\"project_id\":\"projeto-teste\"}", null))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("private_key");
	}

	@Test
	void endpointConfiguravelComBarraFinalNaoDuplicaBarraNemIgnoraOProjectIdDoAmbiente() throws Exception {
		FakeFcm fcm = FakeFcm.start();
		GoogleServiceAccount account = GoogleServiceAccount.load(inlineServiceAccount(null, fcm.endpoint() + "/token"),
				"projeto-override");
		FcmPushGateway gateway = new FcmPushGateway(account, fcm.endpoint() + "/", MAPPER,
				HttpClient.newHttpClient());

		gateway.send("tok-1", "ProCoach", "oi");

		assertThat(fcm.requests).hasSize(2);
		assertThat(fcm.requests.get(0).path()).isEqualTo("/token");
		assertThat(fcm.requests.get(1).path()).isEqualTo("/v1/projects/projeto-override/messages:send");
		assertThat(fcm.requests.get(1).authorization()).isEqualTo("Bearer acesso");
		JsonNode payload = MAPPER.readTree(fcm.requests.get(1).body());
		assertThat(payload.get("message").get("token").asText()).isEqualTo("tok-1");
		assertThat(payload.get("message").get("notification").get("title").asText()).isEqualTo("ProCoach");
		assertThat(payload.get("message").get("notification").get("body").asText()).isEqualTo("oi");
	}

	@Test
	void tokenOAuthRecusadoFalhaComoErroDeEntregaENaoComoTokenDeDispositivoInvalido() throws Exception {
		FakeFcm fcm = FakeFcm.start();
		fcm.tokenReply = new FakeFcm.Reply(401, "{\"error\":\"invalid_grant\"}");
		GoogleServiceAccount account = GoogleServiceAccount.load(inlineServiceAccount(null, fcm.endpoint() + "/token"),
				null);
		FcmPushGateway gateway = new FcmPushGateway(account, fcm.endpoint(), MAPPER, HttpClient.newHttpClient());

		assertThatThrownBy(() -> gateway.send("tok-bom", "ProCoach", "oi"))
			.isInstanceOf(IllegalStateException.class)
			.isNotInstanceOf(InvalidPushTokenException.class)
			.hasMessageContaining("401");
	}

	@Test
	void respostaSemAccessTokenFalhaClaramente() throws Exception {
		FakeFcm fcm = FakeFcm.start();
		fcm.tokenReply = new FakeFcm.Reply(200, "{\"expires_in\":3600}");
		GoogleServiceAccount account = GoogleServiceAccount.load(inlineServiceAccount(null, fcm.endpoint() + "/token"),
				null);
		FcmPushGateway gateway = new FcmPushGateway(account, fcm.endpoint(), MAPPER, HttpClient.newHttpClient());

		assertThatThrownBy(() -> gateway.send("tok-bom", "ProCoach", "oi"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("access_token");
	}

	// ------------------------------------------------------------------
	// Critério: tokens inválidos reconhecidos e removidos; falhas transitórias preservam tokens
	// ------------------------------------------------------------------

	@Test
	void fcmClassificaAssinaturasDeTokenInvalido() throws Exception {
		for (String reply : List.of("{\"error\":{\"status\":\"INVALID_ARGUMENT\"}}",
				"{\"error\":{\"details\":[{\"errorCode\":\"SENDER_ID_MISMATCH\"}]}}",
				"{\"error\":{\"details\":[{\"errorCode\":\"UNREGISTERED\"}]}}")) {
			FakeFcm fcm = FakeFcm.start();
			fcm.responder = body -> new FakeFcm.Reply(400, reply);
			FcmPushGateway gateway = new FcmPushGateway("projeto-teste", fcm.endpoint(), MAPPER,
					HttpClient.newHttpClient(), () -> "oauth");

			assertThatThrownBy(() -> gateway.send("tok-ruim", "ProCoach", "oi"))
				.as("resposta %s", reply)
				.isInstanceOf(InvalidPushTokenException.class)
				.satisfies(ex -> assertThat(((InvalidPushTokenException) ex).getDeviceToken()).isEqualTo("tok-ruim"));
			fcm.close();
		}
	}

	@Test
	void fcmNaoClassificaErroTransitorioOuDeCredencialComoTokenInvalido() throws Exception {
		for (int status : new int[] { 401, 403, 408, 429, 500, 503 }) {
			FakeFcm fcm = FakeFcm.start();
			fcm.responder = body -> new FakeFcm.Reply(status,
					"{\"error\":{\"status\":\"UNAVAILABLE\",\"message\":\"try again\"}}");
			FcmPushGateway gateway = new FcmPushGateway("projeto-teste", fcm.endpoint(), MAPPER,
					HttpClient.newHttpClient(), () -> "oauth");

			assertThatThrownBy(() -> gateway.send("tok-bom", "ProCoach", "oi")).as("status %d", status)
				.isInstanceOf(IllegalStateException.class)
				.isNotInstanceOf(InvalidPushTokenException.class);
			fcm.close();
		}
	}

	@Test
	void erroDeMensagemInvalidaNaoDeveSerConfundidoComTokenInvalido() throws Exception {
		FakeFcm fcm = FakeFcm.start();
		// Erro de payload (mensagem grande/inválida) devolve 400 INVALID_ARGUMENT sem a assinatura
		// específica de token (UNREGISTERED / SENDER_ID_MISMATCH).
		fcm.responder = body -> new FakeFcm.Reply(400,
				"{\"error\":{\"code\":400,\"status\":\"INVALID_ARGUMENT\",\"message\":\"The message is too large.\"}}");
		FcmPushGateway gateway = new FcmPushGateway("projeto-teste", fcm.endpoint(), MAPPER,
				HttpClient.newHttpClient(), () -> "oauth");

		assertThatThrownBy(() -> gateway.send("tok-valido", "ProCoach", "   "))
			.isInstanceOf(IllegalStateException.class)
			.isNotInstanceOf(InvalidPushTokenException.class);
	}

	@Test
	void canalRemoveSoODispositivoRecusadoPeloFcmESegueComOsDemais() throws Exception {
		FakeFcm fcm = FakeFcm.start();
		fcm.responder = body -> body.contains("tok-morto")
				? new FakeFcm.Reply(400, "{\"error\":{\"details\":[{\"errorCode\":\"UNREGISTERED\"}]}}")
				: new FakeFcm.Reply(200, "{}");
		InMemoryDevices devices = new InMemoryDevices();
		UUID athleteId = UUID.randomUUID();
		devices.save(DeviceToken.register(athleteId, "tok-bom", DevicePlatform.ANDROID, Instant.now()));
		devices.save(DeviceToken.register(athleteId, "tok-morto", DevicePlatform.IOS, Instant.now()));
		FcmPushGateway gateway = new FcmPushGateway("projeto-teste", fcm.endpoint(), MAPPER,
				HttpClient.newHttpClient(), () -> "oauth");
		PushNotificationChannel channel = new PushNotificationChannel(provider(() -> gateway), devices);

		NotificationChannel.Result result = channel.send(notification(athleteId));

		assertThat(result.skipped()).isFalse();
		assertThat(devices.store).extracting(DeviceToken::getToken).containsExactly("tok-bom");
	}

	@Test
	void canalComTodosOsTokensInvalidosRemoveTodosERetornaSkippedSemRepetir() throws Exception {
		FakeFcm fcm = FakeFcm.start();
		fcm.responder = body -> new FakeFcm.Reply(404,
				"{\"error\":{\"status\":\"NOT_FOUND\",\"details\":[{\"errorCode\":\"UNREGISTERED\"}]}}");
		InMemoryDevices devices = new InMemoryDevices();
		UUID athleteId = UUID.randomUUID();
		devices.save(DeviceToken.register(athleteId, "tok-1", DevicePlatform.ANDROID, Instant.now()));
		devices.save(DeviceToken.register(athleteId, "tok-2", DevicePlatform.WEB, Instant.now()));
		FcmPushGateway gateway = new FcmPushGateway("projeto-teste", fcm.endpoint(), MAPPER,
				HttpClient.newHttpClient(), () -> "oauth");
		PushNotificationChannel channel = new PushNotificationChannel(provider(() -> gateway), devices);

		NotificationChannel.Result result = channel.send(notification(athleteId));

		assertThat(result.skipped()).isTrue();
		assertThat(result.detail()).contains("token inválido");
		assertThat(devices.store).isEmpty();
	}

	@Test
	void falhaTransitoriaDoFcmPreservaOsDispositivosEPropagaComoFalhaDeEntrega() throws Exception {
		FakeFcm fcm = FakeFcm.start();
		fcm.responder = body -> new FakeFcm.Reply(503, "{\"error\":{\"status\":\"UNAVAILABLE\"}}");
		InMemoryDevices devices = new InMemoryDevices();
		UUID athleteId = UUID.randomUUID();
		devices.save(DeviceToken.register(athleteId, "tok-bom", DevicePlatform.ANDROID, Instant.now()));
		FcmPushGateway gateway = new FcmPushGateway("projeto-teste", fcm.endpoint(), MAPPER,
				HttpClient.newHttpClient(), () -> "oauth");
		PushNotificationChannel channel = new PushNotificationChannel(provider(() -> gateway), devices);

		assertThatThrownBy(() -> channel.send(notification(athleteId))).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("503");
		assertThat(devices.store).extracting(DeviceToken::getToken).containsExactly("tok-bom");
	}

	// ------------------------------------------------------------------
	// Critério: falha de push é tratada como entrega (não derruba a ação do coach,
	// não altera o aviso) — via NotificationDispatcher, como em produção.
	// ------------------------------------------------------------------

	@Test
	void falhaDoFcmViraEntregaFailedSemDerrubarODespachoNemAlterarOAviso() throws Exception {
		FakeFcm fcm = FakeFcm.start();
		fcm.responder = body -> new FakeFcm.Reply(503, "{\"error\":{\"status\":\"UNAVAILABLE\"}}");
		InMemoryDevices devices = new InMemoryDevices();
		UUID athleteId = UUID.randomUUID();
		devices.save(DeviceToken.register(athleteId, "tok-bom", DevicePlatform.ANDROID, Instant.now()));
		FcmPushGateway gateway = new FcmPushGateway("projeto-teste", fcm.endpoint(), MAPPER,
				HttpClient.newHttpClient(), () -> "oauth");
		PushNotificationChannel channel = new PushNotificationChannel(provider(() -> gateway), devices);

		NotificationRepository notifications = mock(NotificationRepository.class);
		NotificationPreferenceRepository preferences = mock(NotificationPreferenceRepository.class);
		DeliveryRepository deliveries = mock(DeliveryRepository.class);
		Notification notification = notification(athleteId);
		when(notifications.findById(notification.getId())).thenReturn(Optional.of(notification));
		when(preferences.findByAthleteId(athleteId)).thenReturn(Optional.empty());
		when(deliveries.find(notification.getId(), NotificationChannelType.PUSH)).thenReturn(Optional.empty());

		NotificationDispatcher dispatcher = new NotificationDispatcher(notifications, preferences, deliveries,
				List.of(channel));
		dispatcher.dispatch(notification.getId());

		ArgumentCaptor<Delivery> saved = ArgumentCaptor.forClass(Delivery.class);
		verify(deliveries).save(saved.capture());
		assertThat(saved.getValue().getStatus()).isEqualTo(DeliveryStatus.FAILED);
		assertThat(saved.getValue().getAttempts()).isEqualTo(1);
		assertThat(saved.getValue().getLastError()).contains("503");
		verify(notifications, never()).save(any());
		assertThat(notification.isRead()).isFalse();
		assertThat(notification.getMessage()).isEqualTo("Seu treino mudou.");
		assertThat(devices.store).extracting(DeviceToken::getToken).containsExactly("tok-bom");
	}

	// ------------------------------------------------------------------
	// infraestrutura de teste
	// ------------------------------------------------------------------

	/**
	 * Sobe os provedores reais (FcmPushGateway/LoggingPushGateway) com as propriedades dadas, como
	 * no ambiente. {@code @ConditionalOnProperty} decide qual (se algum) é instalado.
	 */
	private static AnnotationConfigApplicationContext gatewayContext(String... properties) {
		Map<String, Object> values = new java.util.HashMap<>();
		for (String property : properties) {
			int separator = property.indexOf('=');
			values.put(property.substring(0, separator), property.substring(separator + 1));
		}
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("qa-t076", values));
		context.register(FcmPushGateway.class, LoggingPushGateway.class);
		context.refresh();
		return context;
	}

	private static Notification notification(UUID athleteId) {
		return Notification.create(athleteId, "TRAINING_CHANGED",
				"Seu treino mudou.", "Training", UUID.randomUUID(), Instant.now());
	}

	private static ObjectProvider<PushGateway> provider(Supplier<PushGateway> supplier) {
		return new ObjectProvider<>() {
			@Override
			public PushGateway getObject(Object... args) {
				return supplier.get();
			}

			@Override
			public PushGateway getObject() {
				return supplier.get();
			}

			@Override
			public PushGateway getIfAvailable() {
				return supplier.get();
			}

			@Override
			public PushGateway getIfUnique() {
				return supplier.get();
			}
		};
	}

	private static Throwable rootCause(Throwable throwable) {
		Throwable current = throwable;
		while (current.getCause() != null && current.getCause() != current) {
			current = current.getCause();
		}
		return current;
	}

	private static KeyPair generateKeys() {
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(2048);
			return generator.generateKeyPair();
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static String inlineServiceAccount(String projectId, String tokenUri) {
		String pem = "-----BEGIN PRIVATE KEY-----\n"
				+ Base64.getMimeEncoder(64, new byte[] { '\n' }).encodeToString(SERVICE_ACCOUNT_KEYS.getPrivate()
					.getEncoded())
				+ "\n-----END PRIVATE KEY-----\n";
		ObjectNode node = MAPPER.createObjectNode();
		node.put("client_email", CLIENT_EMAIL);
		node.put("private_key", pem);
		node.put("project_id", projectId != null ? projectId : "projeto-teste");
		if (tokenUri != null) {
			node.put("token_uri", tokenUri);
		}
		return MAPPER.writeValueAsString(node);
	}

	/** Repositório de dispositivos em memória, visível para asserções. */
	private static final class InMemoryDevices implements DeviceTokenRepository {

		final List<DeviceToken> store = new ArrayList<>();

		@Override
		public DeviceToken save(DeviceToken device) {
			store.removeIf(existing -> existing.getId().equals(device.getId()));
			store.add(device);
			return device;
		}

		@Override
		public Optional<DeviceToken> findByToken(String token) {
			return store.stream().filter(device -> device.getToken().equals(token)).findFirst();
		}

		@Override
		public List<DeviceToken> findByAthleteId(UUID athleteId) {
			return store.stream().filter(device -> device.getAthleteId().equals(athleteId)).toList();
		}

		@Override
		public void delete(DeviceToken device) {
			store.removeIf(existing -> existing.getId().equals(device.getId()));
		}

	}

	/** Servidor HTTP que simula FCM (token OAuth + messages:send). */
	private static final class FakeFcm implements AutoCloseable {

		record Captured(String path, String authorization, String body) {
		}

		record Reply(int status, String body) {
		}

		final List<Captured> requests = new CopyOnWriteArrayList<>();

		volatile Reply tokenReply = new Reply(200, "{\"access_token\":\"acesso\",\"expires_in\":3600}");

		volatile java.util.function.Function<String, Reply> responder = body -> new Reply(200, "{}");

		final HttpServer server;

		private FakeFcm(HttpServer server) {
			this.server = server;
		}

		static FakeFcm start() throws Exception {
			HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
			FakeFcm fcm = new FakeFcm(server);
			server.createContext("/", exchange -> {
				String path = exchange.getRequestURI().getPath();
				String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
				fcm.requests.add(new Captured(path, exchange.getRequestHeaders().getFirst("Authorization"), body));
				Reply reply = path.equals("/token") ? fcm.tokenReply : fcm.responder.apply(body);
				byte[] bytes = reply.body().getBytes(StandardCharsets.UTF_8);
				exchange.getResponseHeaders().set("Content-Type", "application/json");
				exchange.sendResponseHeaders(reply.status(), bytes.length);
				exchange.getResponseBody().write(bytes);
				exchange.close();
			});
			server.start();
			return fcm;
		}

		String endpoint() {
			return "http://127.0.0.1:" + server.getAddress().getPort();
		}

		@Override
		public void close() {
			server.stop(0);
		}

	}

}
