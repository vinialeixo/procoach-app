package com.fitness.procoach.audit;

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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Trilha de auditoria (T-013/T-015): cadastro, mudança de status (com motivo),
 * avaliação, dieta, convite e login ficam registrados e o coach dono do atleta
 * consulta o histórico — mais recente primeiro.
 */
@SpringBootTest(classes = ProcoachApplication.class)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuditTrailIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private com.fitness.procoach.security.application.AuthService authService;

	private void changeStatus(String coach, String athleteId, String status, String reason) throws Exception {
		mockMvc.perform(post("/api/v1/athletes/{id}/status", athleteId)
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"status\": \"%s\", \"reason\": \"%s\" }".formatted(status, reason)))
				.andExpect(status().isOk());
	}

	private org.springframework.test.web.servlet.ResultActions history(String token, String athleteId)
			throws Exception {
		return mockMvc.perform(get("/api/v1/athletes/{id}/history", athleteId)
				.header("Authorization", bearer(token)));
	}

	@Test
	void cadastroEMudancaDeStatusFicamNoHistoricoComOMotivo() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		changeStatus(coach, athleteId, "PAUSED", "Viagem de duas semanas");

		history(coach, athleteId)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].eventType").value(hasItems("ATHLETE_REGISTERED", "ATHLETE_STATUS_CHANGED")))
				.andExpect(jsonPath("$[?(@.eventType=='ATHLETE_STATUS_CHANGED')].data")
						.value(hasItem(containsString("Viagem de duas semanas"))))
				.andExpect(jsonPath("$[?(@.eventType=='ATHLETE_STATUS_CHANGED')].data")
						.value(hasItem(containsString("\"from\":\"ACTIVE\""))));
	}

	@Test
	void historicoVemDoMaisRecenteParaOMaisAntigo() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		changeStatus(coach, athleteId, "PAUSED", "pausa");

		history(coach, athleteId)
				.andExpect(jsonPath("$[0].eventType").value("ATHLETE_STATUS_CHANGED"))
				.andExpect(jsonPath("$[1].eventType").value("ATHLETE_REGISTERED"));
	}

	@Test
	void avaliacaoEDietaSaoRegistradas() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);

		mockMvc.perform(post("/api/v1/assessments")
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "athleteId": "%s", "assessmentDate": "2026-09-01", "weightKg": 60, "heightCm": 165 }
								""".formatted(athleteId)))
				.andExpect(status().isCreated());
		MvcResult diet = mockMvc.perform(post("/api/v1/diets")
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "athleteId": "%s", "objective": "CUTTING", "calories": 1500, "protein": 120,
								  "carbohydrates": 150, "fat": 50 }
								""".formatted(athleteId)))
				.andExpect(status().isCreated())
				.andReturn();
		String dietId = JsonPath.read(diet.getResponse().getContentAsString(), "$.id");
		mockMvc.perform(post("/api/v1/diets/{id}/status", dietId)
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"status\": \"IN_REVIEW\" }"))
				.andExpect(status().isOk());

		history(coach, athleteId)
				.andExpect(jsonPath("$[*].eventType")
						.value(hasItems("ASSESSMENT_REGISTERED", "DIET_CREATED", "DIET_STATUS_CHANGED")));
	}

	@Test
	void conviteEPrimeiroAcessoDoAtletaSaoRegistrados() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String email = uniqueEmail("atleta");
		MvcResult invite = mockMvc.perform(post("/api/v1/auth/invites")
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"athleteId\": \"%s\", \"email\": \"%s\" }".formatted(athleteId, email)))
				.andExpect(status().isCreated())
				.andReturn();
		String token = JsonPath.read(invite.getResponse().getContentAsString(), "$.token");
		mockMvc.perform(post("/api/v1/auth/invites/{token}/accept", token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"password\": \"senha-Forte-123\" }"))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"email\": \"%s\", \"password\": \"senha-Forte-123\" }".formatted(email)))
				.andExpect(status().isOk());

		history(coach, athleteId)
				.andExpect(jsonPath("$[*].eventType").value(hasItems("ATHLETE_INVITED", "ATHLETE_INVITE_ACCEPTED",
						"ATHLETE_CREDENTIAL_CREATED", "USER_LOGGED_IN")));
	}

	@Test
	void outroCoachNaoLeOHistorico() throws Exception {
		String coachA = registerCoach(mockMvc, uniqueEmail("coach-a"));
		String coachB = registerCoach(mockMvc, uniqueEmail("coach-b"));
		String athleteOfA = registerAthlete(mockMvc, coachA);

		history(coachB, athleteOfA).andExpect(status().isNotFound());
	}

	@Test
	void atletaNaoLeOHistoricoDeAuditoria() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String athleteToken = authService.registerAthleteCredential(uniqueEmail("atleta"), "senha-Forte-123",
				java.util.UUID.fromString(athleteId)).tokens().accessToken();

		history(athleteToken, athleteId).andExpect(status().isForbidden());
	}

	@Test
	void exerciciosDoTreinoSaoRegistradosComAntesEDepoisDaOrdem() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		MvcResult periodization = mockMvc.perform(post("/api/training/periodizations")
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"athleteId\": \"%s\", \"name\": \"Macrociclo\", \"startDate\": \"2026-09-07\", \"endDate\": \"2026-11-29\" }"
								.formatted(athleteId)))
				.andExpect(status().isCreated())
				.andReturn();
		String periodizationId = JsonPath.read(periodization.getResponse().getContentAsString(), "$.id");
		MvcResult plan = mockMvc.perform(post("/api/training/plans")
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"athleteId\": \"%s\", \"periodizationId\": \"%s\", \"name\": \"Plano 1\" }"
								.formatted(athleteId, periodizationId)))
				.andExpect(status().isCreated())
				.andReturn();
		String planId = JsonPath.read(plan.getResponse().getContentAsString(), "$.id");
		MvcResult training = mockMvc.perform(post("/api/training/trainings")
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "periodizationId": "%s", "planId": "%s", "athleteId": "%s", "name": "Treino A", "weekNumber": 1, "dayOfWeek": 1 }
								""".formatted(periodizationId, planId, athleteId)))
				.andExpect(status().isCreated())
				.andReturn();
		String trainingId = JsonPath.read(training.getResponse().getContentAsString(), "$.id");

		String exercise = """
				{ "exerciseName": "%s", "customExercise": true, "orderIndex": %d, "sets": 4, "repetitions": 10,
				  "load": 60.0, "intensityType": "RPE", "intensityValue": 8.0, "restSeconds": 90,
				  "notes": "texto livre que não deve ir para o histórico" }
				""";
		mockMvc.perform(post("/api/training/trainings/{id}/exercises", trainingId)
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content(exercise.formatted("Supino", 0)))
				.andExpect(status().isOk());
		MvcResult withTwo = mockMvc.perform(post("/api/training/trainings/{id}/exercises", trainingId)
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content(exercise.formatted("Agachamento", 1)))
				.andExpect(status().isOk())
				.andReturn();
		java.util.List<String> ids = JsonPath.read(withTwo.getResponse().getContentAsString(), "$.exercises[*].id");
		mockMvc.perform(put("/api/training/trainings/{id}/exercises/reorder", trainingId)
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"orderedPrescriptionIds\": [\"%s\", \"%s\"] }".formatted(ids.get(1), ids.get(0))))
				.andExpect(status().isOk());

		history(coach, athleteId)
				.andExpect(jsonPath("$[*].eventType").value(hasItems("TRAINING_EXERCISE_ADDED",
						"TRAINING_EXERCISES_REORDERED")))
				.andExpect(jsonPath("$[?(@.eventType==\"TRAINING_EXERCISES_REORDERED\")].data")
						.value(hasItem(containsString(ids.get(0) + "," + ids.get(1)))))
				.andExpect(jsonPath("$[?(@.eventType==\"TRAINING_EXERCISE_ADDED\")].data")
						.value(org.hamcrest.Matchers.not(hasItem(containsString("texto livre")))));
	}
}
