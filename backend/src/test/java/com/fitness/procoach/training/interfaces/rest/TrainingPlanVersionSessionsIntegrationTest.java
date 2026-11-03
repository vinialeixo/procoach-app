package com.fitness.procoach.training.interfaces.rest;

import static com.fitness.procoach.CoachAthleteTestSupport.bearer;
import static com.fitness.procoach.CoachAthleteTestSupport.registerAthlete;
import static com.fitness.procoach.CoachAthleteTestSupport.registerCoach;
import static com.fitness.procoach.CoachAthleteTestSupport.uniqueEmail;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
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
 * T-067: cada sessão pertence a uma versão do plano. Criar a v2 copia as sessões da v1 (que ficam
 * intactas); ativar a v2 publica as sessões novas, encerra a v1 como histórico e avisa o atleta.
 */
@SpringBootTest(classes = ProcoachApplication.class)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TrainingPlanVersionSessionsIntegrationTest {

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

	private String createPeriodization(String coach, String athleteId) throws Exception {
		return id(mockMvc.perform(post("/api/training/periodizations")
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"athleteId\": \"%s\", \"name\": \"Macrociclo\", \"startDate\": \"2026-09-07\", \"endDate\": \"2026-11-29\" }"
								.formatted(athleteId)))
				.andExpect(status().isCreated())
				.andReturn());
	}

	private String createPlan(String coach, String athleteId, String periodizationId) throws Exception {
		return id(mockMvc.perform(post("/api/training/plans")
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"athleteId\": \"%s\", \"periodizationId\": \"%s\", \"name\": \"Plano de hipertrofia\" }"
								.formatted(athleteId, periodizationId)))
				.andExpect(status().isCreated())
				.andReturn());
	}

	private ResultActions postTraining(String coach, String athleteId, String periodizationId, String planId,
			String name, int dayOfWeek) throws Exception {
		return mockMvc.perform(post("/api/training/trainings")
				.header("Authorization", bearer(coach))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "periodizationId": "%s", "planId": "%s", "athleteId": "%s", "name": "%s", "weekNumber": 1,
						  "dayOfWeek": %d }
						""".formatted(periodizationId, planId, athleteId, name, dayOfWeek)));
	}

	private String createTraining(String coach, String athleteId, String periodizationId, String planId, String name,
			int dayOfWeek) throws Exception {
		return id(postTraining(coach, athleteId, periodizationId, planId, name, dayOfWeek)
				.andExpect(status().isCreated())
				.andReturn());
	}

	private void addExercise(String coach, String trainingId, String name, int orderIndex) throws Exception {
		mockMvc.perform(post("/api/training/trainings/{id}/exercises", trainingId)
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "exerciseName": "%s", "customExercise": true, "orderIndex": %d, "sets": 4,
								  "repetitions": 10, "load": 60.0, "intensityType": "RPE", "intensityValue": 8.0,
								  "restSeconds": 90 }
								""".formatted(name, orderIndex)))
				.andExpect(status().isOk());
	}

	private String newVersion(String coach, String planId) throws Exception {
		return id(mockMvc.perform(post("/api/training/plans/{id}/versions", planId)
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"newName\": \"Plano de hipertrofia - bloco 2\", \"newDescription\": \"ajuste\" }"))
				.andExpect(status().isCreated())
				.andReturn());
	}

	private void transitionPlan(String coach, String planId, String target) throws Exception {
		mockMvc.perform(post("/api/training/plans/{id}/transition", planId)
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"targetStatus\": \"%s\" }".formatted(target)))
				.andExpect(status().isOk());
	}

	private void activatePlan(String coach, String planId) throws Exception {
		transitionPlan(coach, planId, "IN_REVIEW");
		transitionPlan(coach, planId, "ACTIVE");
	}

	private ResultActions sessions(String token, String athleteId, String planId) throws Exception {
		return mockMvc.perform(get("/api/training/trainings/athlete/{id}", athleteId).param("planId", planId)
				.header("Authorization", bearer(token)));
	}

	private ResultActions notifications(String token, String athleteId) throws Exception {
		return mockMvc.perform(get("/api/v1/notifications").param("athleteId", athleteId)
				.header("Authorization", bearer(token)));
	}

	// ---- cenários ----

	@Test
	void sessaoNasceLigadaAVersaoDoPlano() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String periodizationId = createPeriodization(coach, athleteId);
		String plan = createPlan(coach, athleteId, periodizationId);

		String trainingId = createTraining(coach, athleteId, periodizationId, plan, "Treino A", 1);

		mockMvc.perform(get("/api/training/trainings/{id}", trainingId).header("Authorization", bearer(coach)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.planId").value(plan));
	}

	@Test
	void novaVersaoDoPlanoCopiaAsSessoesEDeixaAsAntigasIntactas() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String periodizationId = createPeriodization(coach, athleteId);
		String v1 = createPlan(coach, athleteId, periodizationId);
		String a = createTraining(coach, athleteId, periodizationId, v1, "Treino A", 1);
		String b = createTraining(coach, athleteId, periodizationId, v1, "Treino B", 3);
		addExercise(coach, a, "Supino", 0);
		addExercise(coach, a, "Agachamento", 1);
		addExercise(coach, b, "Remada", 0);

		String v2 = newVersion(coach, v1);

		// A v2 nasce com cópias das duas sessões, em rascunho e com as mesmas prescrições.
		sessions(coach, athleteId, v2)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[*].name").value(hasItems("Treino A", "Treino B")))
				.andExpect(jsonPath("$[*].status").value(hasItem("DRAFT")))
				.andExpect(jsonPath("$[?(@.name=='Treino A')].exercises.length()").value(hasItem(2)))
				.andExpect(jsonPath("$[?(@.name=='Treino A')].planId").value(hasItem(v2)));
		// A v1 continua com as mesmas duas sessões e as mesmas prescrições.
		sessions(coach, athleteId, v1)
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[?(@.name=='Treino A')].exercises.length()").value(hasItem(2)))
				.andExpect(jsonPath("$[*].id").value(hasItems(a, b)));
	}

	@Test
	void editarAsSessoesDaV2NaoMudaAsDaV1() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String periodizationId = createPeriodization(coach, athleteId);
		String v1 = createPlan(coach, athleteId, periodizationId);
		String a = createTraining(coach, athleteId, periodizationId, v1, "Treino A", 1);
		addExercise(coach, a, "Supino", 0);
		String v2 = newVersion(coach, v1);
		MvcResult v2Sessions = sessions(coach, athleteId, v2).andExpect(jsonPath("$.length()").value(1)).andReturn();
		String v2Training = JsonPath.read(v2Sessions.getResponse().getContentAsString(), "$[0].id");
		String v2Prescription = JsonPath.read(v2Sessions.getResponse().getContentAsString(), "$[0].exercises[0].id");

		mockMvc.perform(put("/api/training/trainings/{id}/exercises/{pid}", v2Training, v2Prescription)
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "exerciseName": "Supino", "sets": 6, "repetitions": 8, "load": 70.0,
								  "intensityType": "RPE", "intensityValue": 9.0, "restSeconds": 120 }
								"""))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/training/trainings/{id}", a).header("Authorization", bearer(coach)))
				.andExpect(jsonPath("$.exercises[0].sets").value(4))
				.andExpect(jsonPath("$.exercises[0].repetitions").value(10));
		mockMvc.perform(get("/api/training/trainings/{id}", v2Training).header("Authorization", bearer(coach)))
				.andExpect(jsonPath("$.exercises[0].sets").value(6));
	}

	@Test
	void ativarOPlanoPublicaAsSessoesEAvisaOAtleta() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String athlete = athleteToken(athleteId);
		String periodizationId = createPeriodization(coach, athleteId);
		String v1 = createPlan(coach, athleteId, periodizationId);
		createTraining(coach, athleteId, periodizationId, v1, "Treino A", 1);
		createTraining(coach, athleteId, periodizationId, v1, "Treino B", 3);

		// Antes de ativar o plano, o atleta não vê nenhuma sessão.
		sessions(athlete, athleteId, v1).andExpect(jsonPath("$").isEmpty());

		activatePlan(coach, v1);

		sessions(athlete, athleteId, v1)
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[*].status").value(hasItem("ACTIVE")));
		notifications(athlete, athleteId)
				.andExpect(jsonPath("$[0].type").value("TRAINING_PLAN_PUBLISHED"))
				.andExpect(jsonPath("$[0].message").value(containsString("está disponível")));
	}

	@Test
	void ativarAV2EncerraAV1ComoHistoricoEPublicaAsSessoesNovas() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String athlete = athleteToken(athleteId);
		String periodizationId = createPeriodization(coach, athleteId);
		String v1 = createPlan(coach, athleteId, periodizationId);
		createTraining(coach, athleteId, periodizationId, v1, "Treino A", 1);
		createTraining(coach, athleteId, periodizationId, v1, "Treino B", 3);
		activatePlan(coach, v1);
		String v2 = newVersion(coach, v1);

		activatePlan(coach, v2);

		// v1 vira histórico (plano e sessões FINISHED); v2 passa a valer (ACTIVE).
		mockMvc.perform(get("/api/training/plans/{id}", v1).header("Authorization", bearer(coach)))
				.andExpect(jsonPath("$.status").value("FINISHED"));
		mockMvc.perform(get("/api/training/plans/{id}", v2).header("Authorization", bearer(coach)))
				.andExpect(jsonPath("$.status").value("ACTIVE"));
		sessions(athlete, athleteId, v1)
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[*].status").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("FINISHED"))));
		sessions(athlete, athleteId, v2)
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[*].status").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("ACTIVE"))));
		// O atleta vê o plano corrente (v2) e foi avisado da nova versão.
		mockMvc.perform(get("/api/training/plans/athlete/{id}/latest", athleteId)
						.header("Authorization", bearer(athlete)))
				.andExpect(jsonPath("$.id").value(v2));
		notifications(athlete, athleteId)
				.andExpect(jsonPath("$[*].message").value(hasItem(containsString("nova versão do plano de treino"))))
				.andExpect(jsonPath("$[*].message").value(hasItem(containsString("(versão 2)"))));
	}

	@Test
	void historicoRegistraACopiaEOEncerramentoDaVersaoAnterior() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String periodizationId = createPeriodization(coach, athleteId);
		String v1 = createPlan(coach, athleteId, periodizationId);
		createTraining(coach, athleteId, periodizationId, v1, "Treino A", 1);
		activatePlan(coach, v1);
		String v2 = newVersion(coach, v1);
		activatePlan(coach, v2);

		mockMvc.perform(get("/api/v1/athletes/{id}/history", athleteId).header("Authorization", bearer(coach)))
				.andExpect(jsonPath("$[?(@.eventType=='TRAINING_PLAN_VERSIONED')].data")
						.value(hasItem(containsString("\"copiedTrainings\":1"))))
				.andExpect(jsonPath("$[?(@.eventType=='TRAINING_PLAN_STATUS_CHANGED')].data")
						.value(hasItem(containsString("\"cause\":\"new_version_published\""))))
				.andExpect(jsonPath("$[?(@.eventType=='TRAINING_STATUS_CHANGED')].data")
						.value(hasItem(containsString("\"cause\":\"plan_published\""))));
	}

	@Test
	void sessaoPrecisaDeUmPlanoDoMesmoAtletaEDaMesmaPeriodizacao() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteA = registerAthlete(mockMvc, coach);
		String athleteB = registerAthlete(mockMvc, coach);
		String periodizationA = createPeriodization(coach, athleteA);
		String periodizationA2 = createPeriodization(coach, athleteA);
		String periodizationB = createPeriodization(coach, athleteB);
		String planA = createPlan(coach, athleteA, periodizationA);
		String planB = createPlan(coach, athleteB, periodizationB);

		// Plano de outro atleta e plano de outra periodização -> 404; plano inexistente -> 404.
		postTraining(coach, athleteA, periodizationA, planB, "Treino A", 1).andExpect(status().isNotFound());
		postTraining(coach, athleteA, periodizationA2, planA, "Treino A", 1).andExpect(status().isNotFound());
		postTraining(coach, athleteA, periodizationA, UUID.randomUUID().toString(), "Treino A", 1)
				.andExpect(status().isNotFound());
		// Sem planId -> 400.
		mockMvc.perform(post("/api/training/trainings")
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "periodizationId": "%s", "athleteId": "%s", "name": "Treino A", "weekNumber": 1,
								  "dayOfWeek": 1 }
								""".formatted(periodizationA, athleteA)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void versaoFinalizadaNaoRecebeSessoesNovas() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String periodizationId = createPeriodization(coach, athleteId);
		String v1 = createPlan(coach, athleteId, periodizationId);
		createTraining(coach, athleteId, periodizationId, v1, "Treino A", 1);
		activatePlan(coach, v1);
		activatePlan(coach, newVersion(coach, v1));

		postTraining(coach, athleteId, periodizationId, v1, "Treino C", 5).andExpect(status().isConflict());
	}

	@Test
	void outroCoachNaoCriaSessaoNemNovaVersaoNoPlanoAlheio() throws Exception {
		String coachA = registerCoach(mockMvc, uniqueEmail("coach-a"));
		String coachB = registerCoach(mockMvc, uniqueEmail("coach-b"));
		String athleteA = registerAthlete(mockMvc, coachA);
		String periodizationA = createPeriodization(coachA, athleteA);
		String planA = createPlan(coachA, athleteA, periodizationA);

		postTraining(coachB, athleteA, periodizationA, planA, "Treino A", 1).andExpect(status().isNotFound());
		mockMvc.perform(post("/api/training/plans/{id}/versions", planA)
						.header("Authorization", bearer(coachB))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"newName\": \"X\" }"))
				.andExpect(status().isNotFound());
	}

	@Test
	void sessoesRemovidasNaoSaoCopiadasParaANovaVersao() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String periodizationId = createPeriodization(coach, athleteId);
		String v1 = createPlan(coach, athleteId, periodizationId);
		createTraining(coach, athleteId, periodizationId, v1, "Treino A", 1);
		String removed = createTraining(coach, athleteId, periodizationId, v1, "Treino B", 3);
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
						.delete("/api/training/trainings/{id}", removed).header("Authorization", bearer(coach)))
				.andExpect(status().isNoContent());

		String v2 = newVersion(coach, v1);

		sessions(coach, athleteId, v2)
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].name").value("Treino A"));
	}
}
