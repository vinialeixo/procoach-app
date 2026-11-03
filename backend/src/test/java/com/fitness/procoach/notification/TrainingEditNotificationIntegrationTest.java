package com.fitness.procoach.notification;

import static com.fitness.procoach.CoachAthleteTestSupport.bearer;
import static com.fitness.procoach.CoachAthleteTestSupport.registerAthlete;
import static com.fitness.procoach.CoachAthleteTestSupport.registerCoach;
import static com.fitness.procoach.CoachAthleteTestSupport.uniqueEmail;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitness.procoach.ProcoachApplication;
import com.fitness.procoach.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/**
 * O coach edita o que já foi prescrito (inclusive treino ativo) e o atleta é avisado; dieta
 * aprovada também avisa. O histórico guarda o antes/depois da edição.
 */
@SpringBootTest(classes = ProcoachApplication.class)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TrainingEditNotificationIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private com.fitness.procoach.security.application.AuthService authService;

	// ---- helpers ----

	private String id(MvcResult result) throws Exception {
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private String athleteToken(String athleteId) {
		return authService.registerAthleteCredential(uniqueEmail("atleta"), "senha-Forte-123", UUID.fromString(athleteId))
				.tokens().accessToken();
	}

	private String createTraining(String coach, String athleteId) throws Exception {
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
						.content("{ \"athleteId\": \"%s\", \"periodizationId\": \"%s\", \"name\": \"Plano 1\" }"
								.formatted(athleteId, periodizationId)))
				.andExpect(status().isCreated())
				.andReturn());
		return id(mockMvc.perform(post("/api/training/trainings")
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "periodizationId": "%s", "planId": "%s", "athleteId": "%s", "name": "Treino A", "weekNumber": 1, "dayOfWeek": 1 }
								""".formatted(periodizationId, planId, athleteId)))
				.andExpect(status().isCreated())
				.andReturn());
	}

	private String addCustomExercise(String coach, String trainingId, String name, int orderIndex) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/training/trainings/{id}/exercises", trainingId)
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "exerciseName": "%s", "customExercise": true, "orderIndex": %d, "sets": 4,
								  "repetitions": 10, "load": 60.0, "intensityType": "RPE", "intensityValue": 8.0,
								  "restSeconds": 90 }
								""".formatted(name, orderIndex)))
				.andExpect(status().isOk())
				.andReturn();
		java.util.List<String> ids = JsonPath.read(result.getResponse().getContentAsString(),
				"$.exercises[?(@.exerciseName=='" + name + "')].id");
		return ids.get(0);
	}

	private void transition(String coach, String trainingId, String target) throws Exception {
		mockMvc.perform(post("/api/training/trainings/{id}/transition", trainingId)
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"targetStatus\": \"%s\" }".formatted(target)))
				.andExpect(status().isOk());
	}

	private void activate(String coach, String trainingId) throws Exception {
		transition(coach, trainingId, "IN_REVIEW");
		transition(coach, trainingId, "ACTIVE");
	}

	private ResultActions edit(String coach, String trainingId, String prescriptionId, String name, int sets,
			int repetitions, String load) throws Exception {
		return mockMvc.perform(put("/api/training/trainings/{id}/exercises/{pid}", trainingId, prescriptionId)
				.header("Authorization", bearer(coach))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "exerciseName": "%s", "sets": %d, "repetitions": %d, "load": %s, "intensityType": "RPE",
						  "intensityValue": 8.0, "restSeconds": 90 }
						""".formatted(name, sets, repetitions, load)));
	}

	private ResultActions notifications(String token, String athleteId, boolean unreadOnly) throws Exception {
		return mockMvc.perform(get("/api/v1/notifications").param("athleteId", athleteId)
				.param("unreadOnly", String.valueOf(unreadOnly))
				.header("Authorization", bearer(token)));
	}

	// ---- edição + aviso ----

	@Test
	void editarPrescricaoDeTreinoAtivoAvisaOAtleta() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String athlete = athleteToken(athleteId);
		String trainingId = createTraining(coach, athleteId);
		String prescriptionId = addCustomExercise(coach, trainingId, "Supino", 0);
		activate(coach, trainingId);

		edit(coach, trainingId, prescriptionId, "Supino", 5, 12, "60.0")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.exercises[0].sets").value(5))
				.andExpect(jsonPath("$.exercises[0].repetitions").value(12))
				.andExpect(jsonPath("$.exercises[0].id").value(prescriptionId));

		notifications(athlete, athleteId, true)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].type").value("TRAINING_CHANGED"))
				.andExpect(jsonPath("$[0].read").value(false))
				.andExpect(jsonPath("$[0].message").value(containsString("séries 4 → 5")))
				.andExpect(jsonPath("$[0].message").value(containsString("repetições 10 → 12")))
				.andExpect(jsonPath("$[0].message").value(not(containsString("carga"))));
	}

	@Test
	void editarTreinoEmRascunhoNaoAvisaOAtleta() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String athlete = athleteToken(athleteId);
		String trainingId = createTraining(coach, athleteId);
		String prescriptionId = addCustomExercise(coach, trainingId, "Supino", 0);

		edit(coach, trainingId, prescriptionId, "Supino", 5, 12, "60.0").andExpect(status().isOk());

		notifications(athlete, athleteId, false).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
	}

	@Test
	void editarSemMudarNadaNaoAvisaNemGravaHistorico() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String athlete = athleteToken(athleteId);
		String trainingId = createTraining(coach, athleteId);
		String prescriptionId = addCustomExercise(coach, trainingId, "Supino", 0);
		activate(coach, trainingId);

		// Mesmos valores da prescrição original (60 kg == 60.00).
		edit(coach, trainingId, prescriptionId, "Supino", 4, 10, "60.00").andExpect(status().isOk());

		notifications(athlete, athleteId, false).andExpect(jsonPath("$").isEmpty());
		mockMvc.perform(get("/api/v1/athletes/{id}/history", athleteId).header("Authorization", bearer(coach)))
				.andExpect(jsonPath("$[*].eventType").value(not(hasItem("TRAINING_EXERCISE_UPDATED"))));
	}

	@Test
	void adicionarRemoverEReordenarEmTreinoAtivoAvisamOAtleta() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String athlete = athleteToken(athleteId);
		String trainingId = createTraining(coach, athleteId);
		String first = addCustomExercise(coach, trainingId, "Supino", 0);
		String second = addCustomExercise(coach, trainingId, "Agachamento", 1);
		activate(coach, trainingId);

		addCustomExercise(coach, trainingId, "Remada", 2);
		mockMvc.perform(put("/api/training/trainings/{id}/exercises/reorder", trainingId)
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"orderedPrescriptionIds\": [\"%s\", \"%s\", \"%s\"] }".formatted(second, first,
								findId(coach, trainingId, "Remada"))))
				.andExpect(status().isOk());
		mockMvc.perform(delete("/api/training/trainings/{id}/exercises/{pid}", trainingId, first)
						.header("Authorization", bearer(coach)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.exercises.length()").value(2));

		notifications(athlete, athleteId, false)
				.andExpect(jsonPath("$[*].message").value(hasItems(
						containsString("adicionou \"Remada\""),
						containsString("reorganizou a ordem"),
						containsString("removeu \"Supino\""))));
	}

	private String findId(String coach, String trainingId, String name) throws Exception {
		MvcResult result = mockMvc.perform(get("/api/training/trainings/{id}", trainingId)
						.header("Authorization", bearer(coach)))
				.andExpect(status().isOk())
				.andReturn();
		java.util.List<String> ids = JsonPath.read(result.getResponse().getContentAsString(),
				"$.exercises[?(@.exerciseName=='" + name + "')].id");
		return ids.get(0);
	}

	// ---- avisos: leitura e isolamento ----

	@Test
	void atletaMarcaAvisosComoLidos() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String athlete = athleteToken(athleteId);
		String trainingId = createTraining(coach, athleteId);
		String prescriptionId = addCustomExercise(coach, trainingId, "Supino", 0);
		activate(coach, trainingId);
		edit(coach, trainingId, prescriptionId, "Supino", 5, 10, "60.0").andExpect(status().isOk());
		edit(coach, trainingId, prescriptionId, "Supino", 5, 12, "60.0").andExpect(status().isOk());

		MvcResult list = notifications(athlete, athleteId, true)
				.andExpect(jsonPath("$.length()").value(2))
				.andReturn();
		String firstId = JsonPath.read(list.getResponse().getContentAsString(), "$[0].id");

		mockMvc.perform(post("/api/v1/notifications/{id}/read", firstId).header("Authorization", bearer(athlete)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.read").value(true));
		notifications(athlete, athleteId, true).andExpect(jsonPath("$.length()").value(1));

		mockMvc.perform(post("/api/v1/notifications/read-all").header("Authorization", bearer(athlete)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.marked").value(1));
		notifications(athlete, athleteId, true).andExpect(jsonPath("$").isEmpty());
		notifications(athlete, athleteId, false).andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	void atletaNaoVeNemMarcaAvisosDeOutroAtleta() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteA = registerAthlete(mockMvc, coach);
		String athleteB = registerAthlete(mockMvc, coach);
		String tokenA = athleteToken(athleteA);
		String tokenB = athleteToken(athleteB);
		String trainingId = createTraining(coach, athleteB);
		String prescriptionId = addCustomExercise(coach, trainingId, "Supino", 0);
		activate(coach, trainingId);
		edit(coach, trainingId, prescriptionId, "Supino", 5, 10, "60.0").andExpect(status().isOk());
		MvcResult list = notifications(tokenB, athleteB, false).andReturn();
		String notificationOfB = JsonPath.read(list.getResponse().getContentAsString(), "$[0].id");

		// A não lista os avisos de B (403) e não consegue marcar um aviso de B (404).
		notifications(tokenA, athleteB, false).andExpect(status().isForbidden());
		mockMvc.perform(post("/api/v1/notifications/{id}/read", notificationOfB)
						.header("Authorization", bearer(tokenA)))
				.andExpect(status().isNotFound());
		notifications(tokenB, athleteB, true).andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void coachNaoLeOsAvisosDoAtleta() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);

		notifications(coach, athleteId, false).andExpect(status().isForbidden());
	}

	// ---- regras da edição ----

	@Test
	void outroCoachNaoEditaPrescricaoAlheia() throws Exception {
		String coachA = registerCoach(mockMvc, uniqueEmail("coach-a"));
		String coachB = registerCoach(mockMvc, uniqueEmail("coach-b"));
		String athleteId = registerAthlete(mockMvc, coachA);
		String trainingId = createTraining(coachA, athleteId);
		String prescriptionId = addCustomExercise(coachA, trainingId, "Supino", 0);

		edit(coachB, trainingId, prescriptionId, "Supino", 5, 12, "60.0").andExpect(status().isNotFound());
		mockMvc.perform(delete("/api/training/trainings/{id}/exercises/{pid}", trainingId, prescriptionId)
						.header("Authorization", bearer(coachB)))
				.andExpect(status().isNotFound());
	}

	@Test
	void treinoFinalizadoNaoAceitaEdicao() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String trainingId = createTraining(coach, athleteId);
		String prescriptionId = addCustomExercise(coach, trainingId, "Supino", 0);
		activate(coach, trainingId);
		transition(coach, trainingId, "FINISHED");

		edit(coach, trainingId, prescriptionId, "Supino", 5, 12, "60.0").andExpect(status().isConflict());
	}

	@Test
	void prescricaoInexistenteRetorna404EValoresInvalidosRetornam400() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String trainingId = createTraining(coach, athleteId);
		String prescriptionId = addCustomExercise(coach, trainingId, "Supino", 0);

		edit(coach, trainingId, UUID.randomUUID().toString(), "Supino", 5, 12, "60.0")
				.andExpect(status().isNotFound());
		edit(coach, trainingId, prescriptionId, "Supino", 0, 12, "60.0").andExpect(status().isBadRequest());
	}

	@Test
	void historicoGuardaOAntesEDepoisDaEdicao() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String trainingId = createTraining(coach, athleteId);
		String prescriptionId = addCustomExercise(coach, trainingId, "Supino", 0);
		activate(coach, trainingId);

		edit(coach, trainingId, prescriptionId, "Supino", 5, 12, "62.5").andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/athletes/{id}/history", athleteId).header("Authorization", bearer(coach)))
				.andExpect(jsonPath("$[?(@.eventType=='TRAINING_EXERCISE_UPDATED')].data")
						.value(hasItem(containsString("\"setsBefore\":\"4\""))))
				.andExpect(jsonPath("$[?(@.eventType=='TRAINING_EXERCISE_UPDATED')].data")
						.value(hasItem(containsString("\"setsAfter\":\"5\""))))
				.andExpect(jsonPath("$[?(@.eventType=='TRAINING_EXERCISE_UPDATED')].data")
						.value(hasItem(containsString("\"loadAfter\":\"62.5\""))));
	}

	// ---- trocar pela versão mais recente do exercício da biblioteca ----

	@Test
	void coachTrocaAPrescricaoPelaVersaoMaisRecenteDoExercicio() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String athlete = athleteToken(athleteId);
		String v1 = id(mockMvc.perform(post("/api/library/exercises")
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "Supino reto com barra", "muscleGroups": ["PEITO"], "equipment": "BARRA_LIVRE",
								  "execution": "Desça a barra até o peito.", "commonErrors": [], "progressions": [],
								  "regressions": [], "contraindications": [], "references": [] }
								"""))
				.andExpect(status().isCreated())
				.andReturn());
		String trainingId = createTraining(coach, athleteId);
		MvcResult added = mockMvc.perform(post("/api/training/trainings/{id}/exercises", trainingId)
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "exerciseId": "%s", "exerciseName": "Supino reto com barra", "customExercise": false,
								  "orderIndex": 0, "sets": 4, "repetitions": 10, "load": 60.0, "intensityType": "RPE",
								  "intensityValue": 8.0, "restSeconds": 90 }
								""".formatted(v1)))
				.andExpect(status().isOk())
				.andReturn();
		String prescriptionId = JsonPath.read(added.getResponse().getContentAsString(), "$.exercises[0].id");
		activate(coach, trainingId);
		String v2 = id(mockMvc.perform(put("/api/library/exercises/{id}", v1)
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "Supino reto com halteres", "muscleGroups": ["PEITO"], "equipment": "HALTERES",
								  "execution": "Desça os halteres.", "commonErrors": [], "progressions": [],
								  "regressions": [], "contraindications": [], "references": [] }
								"""))
				.andExpect(status().isOk())
				.andReturn());

		mockMvc.perform(get("/api/training/trainings/{id}", trainingId).header("Authorization", bearer(coach)))
				.andExpect(jsonPath("$.exercises[0].newerVersionAvailable").value(true))
				.andExpect(jsonPath("$.exercises[0].currentExerciseId").value(v2));

		// O coach manda o currentExerciseId e o nome da versão nova.
		mockMvc.perform(put("/api/training/trainings/{id}/exercises/{pid}", trainingId, prescriptionId)
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "exerciseId": "%s", "exerciseName": "Supino reto com halteres", "sets": 4,
								  "repetitions": 10, "load": 60.0, "intensityType": "RPE", "intensityValue": 8.0,
								  "restSeconds": 90 }
								""".formatted(v2)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.exercises[0].exerciseId").value(v2))
				.andExpect(jsonPath("$.exercises[0].exerciseVersion").value(2))
				.andExpect(jsonPath("$.exercises[0].newerVersionAvailable").value(false));

		notifications(athlete, athleteId, false)
				.andExpect(jsonPath("$[0].message").value(containsString("exercício trocado pela versão mais recente")));
	}

	// ---- dieta aprovada ----

	@Test
	void aprovarDietaAvisaOAtleta() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String athlete = athleteToken(athleteId);
		String v1 = id(mockMvc.perform(post("/api/v1/diets")
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "athleteId": "%s", "objective": "CUTTING", "calories": 1500, "protein": 120,
								  "carbohydrates": 150, "fat": 50 }
								""".formatted(athleteId)))
				.andExpect(status().isCreated())
				.andReturn());

		// Rascunho e revisão não avisam; a aprovação sim.
		changeDietStatus(coach, v1, "IN_REVIEW");
		notifications(athlete, athleteId, false).andExpect(jsonPath("$").isEmpty());
		changeDietStatus(coach, v1, "APPROVED");
		notifications(athlete, athleteId, false)
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].type").value("DIET_APPROVED"))
				.andExpect(jsonPath("$[0].message").value("Sua dieta está disponível."));

		String v2 = id(mockMvc.perform(post("/api/v1/diets/{id}/versions", v1)
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "objective": "CUTTING", "calories": 1400, "protein": 120, "carbohydrates": 130,
								  "fat": 45, "changeReason": "ajuste" }
								"""))
				.andExpect(status().isCreated())
				.andReturn());
		changeDietStatus(coach, v2, "IN_REVIEW");
		changeDietStatus(coach, v2, "APPROVED");

		notifications(athlete, athleteId, true)
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[*].message").value(hasItem(containsString("atualizou sua dieta (versão 2)"))));
	}

	private void changeDietStatus(String coach, String dietId, String status) throws Exception {
		mockMvc.perform(post("/api/v1/diets/{id}/status", dietId)
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"status\": \"%s\" }".formatted(status)))
				.andExpect(status().isOk());
	}
}
