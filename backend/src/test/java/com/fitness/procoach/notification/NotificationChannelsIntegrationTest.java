package com.fitness.procoach.notification;

import static com.fitness.procoach.CoachAthleteTestSupport.bearer;
import static com.fitness.procoach.CoachAthleteTestSupport.registerAthlete;
import static com.fitness.procoach.CoachAthleteTestSupport.registerCoach;
import static com.fitness.procoach.CoachAthleteTestSupport.uniqueEmail;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

import com.fitness.procoach.ProcoachApplication;
import com.fitness.procoach.TestcontainersConfiguration;
import com.fitness.procoach.notification.application.NotificationChannel;
import com.fitness.procoach.notification.application.NotificationDispatcher;
import com.fitness.procoach.notification.domain.Delivery;
import com.fitness.procoach.notification.domain.DeliveryRepository;
import com.fitness.procoach.notification.domain.DeliveryStatus;
import com.fitness.procoach.notification.domain.DeviceTokenRepository;
import com.fitness.procoach.notification.domain.Notification;
import com.fitness.procoach.notification.domain.NotificationChannelType;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/**
 * T-071: o aviso do atleta também sai por e-mail e push, respeitando a preferência dele; falha de
 * canal não derruba a ação do coach e a entrega é repetida. Canais de teste registram o que enviam.
 */
@SpringBootTest(classes = ProcoachApplication.class)
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, NotificationChannelsIntegrationTest.RecordingChannels.class })
class NotificationChannelsIntegrationTest {

	/** Canal de teste: disponível, registra o que recebe e pode ser forçado a falhar. */
	static final class RecordingChannel implements NotificationChannel {
		private final NotificationChannelType type;
		final List<String> messages = new CopyOnWriteArrayList<>();
		volatile int failures;

		RecordingChannel(NotificationChannelType type) {
			this.type = type;
		}

		@Override
		public NotificationChannelType type() {
			return type;
		}

		@Override
		public boolean available() {
			return true;
		}

		@Override
		public Result send(Notification notification) {
			if (failures > 0) {
				failures--;
				throw new IllegalStateException("provedor fora do ar");
			}
			messages.add(notification.getMessage());
			return Result.sent();
		}

		void reset() {
			messages.clear();
			failures = 0;
		}
	}

	@TestConfiguration
	static class RecordingChannels {
		@Bean
		RecordingChannel recordingEmail() {
			return new RecordingChannel(NotificationChannelType.EMAIL);
		}

		@Bean
		RecordingChannel recordingPush() {
			return new RecordingChannel(NotificationChannelType.PUSH);
		}
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private com.fitness.procoach.security.application.AuthService authService;

	@Autowired
	private RecordingChannel recordingEmail;

	@Autowired
	private RecordingChannel recordingPush;

	@Autowired
	private DeliveryRepository deliveries;

	@Autowired
	private DeviceTokenRepository devices;

	@Autowired
	private NotificationDispatcher dispatcher;

	@BeforeEach
	void resetChannels() {
		recordingEmail.reset();
		recordingPush.reset();
	}

	// ---- helpers ----

	private String id(MvcResult result) throws Exception {
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private String athleteToken(String athleteId) {
		return authService.registerAthleteCredential(uniqueEmail("atleta"), "senha-Forte-123", UUID.fromString(athleteId))
				.tokens().accessToken();
	}

	/** Treino ativo com um exercício; devolve {trainingId, prescriptionId}. */
	private String[] activeTraining(String coach, String athleteId) throws Exception {
		String periodizationId = id(mockMvc.perform(post("/api/training/periodizations")
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"athleteId\": \"%s\", \"name\": \"Macrociclo\", \"startDate\": \"2026-09-07\", \"endDate\": \"2026-11-29\" }"
								.formatted(athleteId)))
				.andExpect(status().isCreated())
				.andReturn());
		String planId = id(mockMvc.perform(post("/api/training/plans")
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"athleteId\": \"%s\", \"periodizationId\": \"%s\", \"name\": \"Plano\" }"
								.formatted(athleteId, periodizationId)))
				.andExpect(status().isCreated())
				.andReturn());
		String trainingId = id(mockMvc.perform(post("/api/training/trainings")
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "periodizationId": "%s", "planId": "%s", "athleteId": "%s", "name": "Treino A",
								  "weekNumber": 1, "dayOfWeek": 1 }
								""".formatted(periodizationId, planId, athleteId)))
				.andExpect(status().isCreated())
				.andReturn());
		MvcResult added = mockMvc.perform(post("/api/training/trainings/{id}/exercises", trainingId)
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "exerciseName": "Supino", "customExercise": true, "orderIndex": 0, "sets": 4,
								  "repetitions": 10, "load": 60.0, "intensityType": "RPE", "intensityValue": 8.0,
								  "restSeconds": 90 }
								"""))
				.andExpect(status().isOk())
				.andReturn();
		String prescriptionId = JsonPath.read(added.getResponse().getContentAsString(), "$.exercises[0].id");
		for (String target : List.of("IN_REVIEW", "ACTIVE")) {
			mockMvc.perform(post("/api/training/trainings/{id}/transition", trainingId)
							.header("Authorization", bearer(coach))
							.contentType(MediaType.APPLICATION_JSON)
							.content("{ \"targetStatus\": \"%s\" }".formatted(target)))
					.andExpect(status().isOk());
		}
		return new String[] { trainingId, prescriptionId };
	}

	private ResultActions edit(String coach, String[] training, int sets) throws Exception {
		return mockMvc.perform(put("/api/training/trainings/{id}/exercises/{pid}", training[0], training[1])
				.header("Authorization", bearer(coach))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "exerciseName": "Supino", "sets": %d, "repetitions": 10, "load": 60.0,
						  "intensityType": "RPE", "intensityValue": 8.0, "restSeconds": 90 }
						""".formatted(sets)));
	}

	private ResultActions setPreferences(String athleteToken, boolean email, boolean push) throws Exception {
		return mockMvc.perform(put("/api/v1/notifications/preferences")
				.header("Authorization", bearer(athleteToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"emailEnabled\": %s, \"pushEnabled\": %s }".formatted(email, push)));
	}

	private String latestNotificationId(String athleteToken, String athleteId) throws Exception {
		MvcResult result = mockMvc.perform(get("/api/v1/notifications").param("athleteId", athleteId)
						.header("Authorization", bearer(athleteToken)))
				.andExpect(status().isOk())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$[0].id");
	}

	// ---- entrega ----

	@Test
	void editarTreinoAtivoEntregaOAvisoPorEmailEPushEGravaARastroDeEntrega() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String athlete = athleteToken(athleteId);
		String[] training = activeTraining(coach, athleteId);
		recordingEmail.reset();
		recordingPush.reset();

		edit(coach, training, 5).andExpect(status().isOk());

		assertThat(recordingEmail.messages).hasSize(1);
		assertThat(recordingEmail.messages.get(0)).contains("séries 4 → 5");
		assertThat(recordingPush.messages).hasSize(1);
		UUID notificationId = UUID.fromString(latestNotificationId(athlete, athleteId));
		assertThat(deliveries.find(notificationId, NotificationChannelType.EMAIL))
				.get().extracting(Delivery::getStatus).isEqualTo(DeliveryStatus.SENT);
		assertThat(deliveries.find(notificationId, NotificationChannelType.PUSH))
				.get().extracting(Delivery::getStatus).isEqualTo(DeliveryStatus.SENT);
	}

	@Test
	void dietaEmRascunhoOuRevisaoNaoGeraEntregaExterna() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String dietId = id(mockMvc.perform(post("/api/v1/diets")
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "athleteId": "%s", "objective": "CUTTING", "calories": 1500, "protein": 120,
								  "carbohydrates": 150, "fat": 50 }
								""".formatted(athleteId)))
				.andExpect(status().isCreated())
				.andReturn());
		mockMvc.perform(post("/api/v1/diets/{id}/status", dietId)
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"status\": \"IN_REVIEW\" }"))
				.andExpect(status().isOk());

		// O atleta só enxerga a dieta aprovada, então rascunho e revisão não saem por canal nenhum.
		assertThat(recordingEmail.messages).isEmpty();
		assertThat(recordingPush.messages).isEmpty();
	}

	@Test
	void aprovarDietaTambemSaiPorEmailEPush() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String dietId = id(mockMvc.perform(post("/api/v1/diets")
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "athleteId": "%s", "objective": "CUTTING", "calories": 1500, "protein": 120,
								  "carbohydrates": 150, "fat": 50 }
								""".formatted(athleteId)))
				.andExpect(status().isCreated())
				.andReturn());
		for (String target : List.of("IN_REVIEW", "APPROVED")) {
			mockMvc.perform(post("/api/v1/diets/{id}/status", dietId)
							.header("Authorization", bearer(coach))
							.contentType(MediaType.APPLICATION_JSON)
							.content("{ \"status\": \"%s\" }".formatted(target)))
					.andExpect(status().isOk());
		}

		assertThat(recordingEmail.messages).containsExactly("Sua dieta está disponível.");
		assertThat(recordingPush.messages).containsExactly("Sua dieta está disponível.");
	}

	// ---- preferências ----

	@Test
	void preferenciaPadraoTemTudoLigadoEPodeSerAlterada() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athlete = athleteToken(registerAthlete(mockMvc, coach));

		mockMvc.perform(get("/api/v1/notifications/preferences").header("Authorization", bearer(athlete)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.emailEnabled").value(true))
				.andExpect(jsonPath("$.pushEnabled").value(true));

		setPreferences(athlete, false, true)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.emailEnabled").value(false))
				.andExpect(jsonPath("$.pushEnabled").value(true));
		mockMvc.perform(get("/api/v1/notifications/preferences").header("Authorization", bearer(athlete)))
				.andExpect(jsonPath("$.emailEnabled").value(false));
	}

	@Test
	void atletaQueDesligouOEmailSoRecebePushMasOAvisoNoAppContinua() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String athlete = athleteToken(athleteId);
		String[] training = activeTraining(coach, athleteId);
		setPreferences(athlete, false, true).andExpect(status().isOk());
		recordingEmail.reset();
		recordingPush.reset();

		edit(coach, training, 5).andExpect(status().isOk());

		assertThat(recordingEmail.messages).isEmpty();
		assertThat(recordingPush.messages).hasSize(1);
		mockMvc.perform(get("/api/v1/notifications").param("athleteId", athleteId).param("unreadOnly", "true")
						.header("Authorization", bearer(athlete)))
				.andExpect(jsonPath("$[0].message").value(containsString("séries 4 → 5")));
	}

	@Test
	void atletaQueDesligouTudoSoTemOAvisoNoApp() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String athlete = athleteToken(athleteId);
		String[] training = activeTraining(coach, athleteId);
		setPreferences(athlete, false, false).andExpect(status().isOk());
		recordingEmail.reset();
		recordingPush.reset();

		edit(coach, training, 5).andExpect(status().isOk());

		assertThat(recordingEmail.messages).isEmpty();
		assertThat(recordingPush.messages).isEmpty();
		mockMvc.perform(get("/api/v1/notifications").param("athleteId", athleteId)
						.header("Authorization", bearer(athlete)))
				.andExpect(jsonPath("$[0].type").value("TRAINING_CHANGED"));
	}

	@Test
	void preferenciasValidamOCorpoEExigemOAtleta() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athlete = athleteToken(registerAthlete(mockMvc, coach));

		mockMvc.perform(put("/api/v1/notifications/preferences")
						.header("Authorization", bearer(athlete))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"emailEnabled\": true }"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/v1/notifications/preferences").header("Authorization", bearer(coach)))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/v1/notifications/preferences")).andExpect(status().isUnauthorized());
	}

	// ---- dispositivos ----

	@Test
	void atletaRegistraETiraDispositivo() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String athlete = athleteToken(athleteId);
		String token = "fcm-" + UUID.randomUUID();

		mockMvc.perform(post("/api/v1/notifications/devices")
						.header("Authorization", bearer(athlete))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"token\": \"%s\", \"platform\": \"ANDROID\" }".formatted(token)))
				.andExpect(status().isNoContent());
		assertThat(devices.findByAthleteId(UUID.fromString(athleteId))).hasSize(1);

		// Registrar de novo não duplica.
		mockMvc.perform(post("/api/v1/notifications/devices")
						.header("Authorization", bearer(athlete))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"token\": \"%s\", \"platform\": \"IOS\" }".formatted(token)))
				.andExpect(status().isNoContent());
		assertThat(devices.findByAthleteId(UUID.fromString(athleteId))).hasSize(1);

		mockMvc.perform(delete("/api/v1/notifications/devices").param("token", token)
						.header("Authorization", bearer(athlete)))
				.andExpect(status().isNoContent());
		assertThat(devices.findByAthleteId(UUID.fromString(athleteId))).isEmpty();
	}

	@Test
	void mesmoAparelhoEmOutraContaMudaDeDonoENaoPodeSerRemovidoPorOutro() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteA = registerAthlete(mockMvc, coach);
		String athleteB = registerAthlete(mockMvc, coach);
		String tokenA = athleteToken(athleteA);
		String tokenB = athleteToken(athleteB);
		String device = "fcm-" + UUID.randomUUID();
		String body = "{ \"token\": \"%s\", \"platform\": \"WEB\" }".formatted(device);

		mockMvc.perform(post("/api/v1/notifications/devices").header("Authorization", bearer(tokenA))
				.contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isNoContent());
		mockMvc.perform(post("/api/v1/notifications/devices").header("Authorization", bearer(tokenB))
				.contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isNoContent());

		assertThat(devices.findByAthleteId(UUID.fromString(athleteA))).isEmpty();
		assertThat(devices.findByAthleteId(UUID.fromString(athleteB))).hasSize(1);
		// A não consegue remover o aparelho que agora é de B.
		mockMvc.perform(delete("/api/v1/notifications/devices").param("token", device)
						.header("Authorization", bearer(tokenA)))
				.andExpect(status().isNoContent());
		assertThat(devices.findByAthleteId(UUID.fromString(athleteB))).hasSize(1);
	}

	@Test
	void dispositivoValidaOCorpo() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athlete = athleteToken(registerAthlete(mockMvc, coach));

		mockMvc.perform(post("/api/v1/notifications/devices").header("Authorization", bearer(athlete))
				.contentType(MediaType.APPLICATION_JSON).content("{ \"token\": \"\", \"platform\": \"ANDROID\" }"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post("/api/v1/notifications/devices").header("Authorization", bearer(athlete))
				.contentType(MediaType.APPLICATION_JSON).content("{ \"token\": \"abc\", \"platform\": \"WINDOWS\" }"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post("/api/v1/notifications/devices").header("Authorization", bearer(coach))
				.contentType(MediaType.APPLICATION_JSON).content("{ \"token\": \"abc\", \"platform\": \"WEB\" }"))
				.andExpect(status().isForbidden());
	}

	// ---- falha e repetição ----

	@Test
	void falhaDeUmCanalNaoDerrubaAEdicaoEARepeticaoEntregaDepois() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String athlete = athleteToken(athleteId);
		String[] training = activeTraining(coach, athleteId);
		recordingEmail.reset();
		recordingPush.reset();
		recordingPush.failures = 1;

		// O push falha, mas a edição do coach dá certo e o e-mail sai.
		edit(coach, training, 5).andExpect(status().isOk());

		assertThat(recordingEmail.messages).hasSize(1);
		assertThat(recordingPush.messages).isEmpty();
		UUID notificationId = UUID.fromString(latestNotificationId(athlete, athleteId));
		Delivery failed = deliveries.find(notificationId, NotificationChannelType.PUSH).orElseThrow();
		assertThat(failed.getStatus()).isEqualTo(DeliveryStatus.FAILED);
		assertThat(failed.getAttempts()).isEqualTo(1);
		assertThat(failed.getLastError()).contains("provedor fora do ar");

		// A repetição entrega o que falhou, sem reenviar o e-mail.
		assertThat(dispatcher.retryFailed()).isEqualTo(1);

		assertThat(recordingPush.messages).hasSize(1);
		assertThat(recordingEmail.messages).hasSize(1);
		Delivery recovered = deliveries.find(notificationId, NotificationChannelType.PUSH).orElseThrow();
		assertThat(recovered.getStatus()).isEqualTo(DeliveryStatus.SENT);
		assertThat(recovered.getAttempts()).isEqualTo(2);
	}
}
