package com.fitness.procoach.training.interfaces.rest;

import static com.fitness.procoach.CoachAthleteTestSupport.bearer;
import static com.fitness.procoach.CoachAthleteTestSupport.registerAthlete;
import static com.fitness.procoach.CoachAthleteTestSupport.registerCoach;
import static com.fitness.procoach.CoachAthleteTestSupport.uniqueEmail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitness.procoach.ProcoachApplication;
import com.fitness.procoach.athlete.infrastructure.PostgresTestcontainersConfiguration;
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
 * Testes de autorização do módulo training (T-058 item 2): um coach autenticado
 * NÃO consegue gravar/lê periodização (e derivados) de atleta de outro coach,
 * mesmo forçando coachId no payload. Usa token JWT real.
 *
 * <p>Item 3 da revisão T-058: anti-IDOR nos endpoints por {@code {id}} — o
 * Coach B consultando/alterando recurso (assessment/diet/periodização/plano/
 * treino) do Coach A pelo id do recurso recebe 404 Not Found uniforme (mesmo
 * status de recurso inexistente, sem vazar a existência).</p>
 */
@SpringBootTest(classes = ProcoachApplication.class)
@AutoConfigureMockMvc
@Import(PostgresTestcontainersConfiguration.class)
class TrainingAuthorizationTest {

	@Autowired
	private MockMvc mockMvc;

	private String periodizationJson(String athleteId) {
		return """
				{
				  "athleteId": "%s",
				  "name": "Macrociclo 2026",
				  "description": "12 semanas de hipertrofia",
				  "startDate": "2026-09-07",
				  "endDate": "2026-11-29"
				}
				""".formatted(athleteId);
	}

	private String planJson(String athleteId, String periodizationId) {
		return """
				{
				  "athleteId": "%s",
				  "periodizationId": "%s",
				  "name": "Plano de Hipertrofia - Bloco 1",
				  "description": "Foco em hipertrofia de membros superiores, 4x/semana."
				}
				""".formatted(athleteId, periodizationId);
	}

	private String trainingJson(String athleteId, String periodizationId) {
		return """
				{
				  "periodizationId": "%s",
				  "athleteId": "%s",
				  "name": "Treino A - Peito e Tríceps",
				  "description": "Ênfase em força",
				  "weekNumber": 1,
				  "dayOfWeek": 1
				}
				""".formatted(periodizationId, athleteId);
	}

	private String exerciseJson() {
		return """
				{
				  "exerciseName": "Supino reto com barra",
				  "customExercise": true,
				  "orderIndex": 1,
				  "sets": 4,
				  "repetitions": 10,
				  "load": 60.0,
				  "intensityType": "RPE",
				  "intensityValue": 8.0,
				  "restSeconds": 90,
				  "notes": "Cadência controlada"
				}
				""";
	}

	// ---- helpers de criação (retornam o id do recurso criado) ----

	private String createPeriodization(String token, String athleteId) throws Exception {
		MvcResult created = mockMvc.perform(post("/api/training/periodizations")
						.header("Authorization", bearer(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content(periodizationJson(athleteId)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(created.getResponse().getContentAsString(), "$.id");
	}

	private String createPlan(String token, String athleteId, String periodizationId) throws Exception {
		MvcResult created = mockMvc.perform(post("/api/training/plans")
						.header("Authorization", bearer(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content(planJson(athleteId, periodizationId)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(created.getResponse().getContentAsString(), "$.id");
	}

	private String createTraining(String token, String athleteId, String periodizationId) throws Exception {
		MvcResult created = mockMvc.perform(post("/api/training/trainings")
						.header("Authorization", bearer(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content(trainingJson(athleteId, periodizationId)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(created.getResponse().getContentAsString(), "$.id");
	}

	@Test
	void coachCriaPeriodizacaoParaProprioAtleta() throws Exception {
		String coachToken = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coachToken);

		mockMvc.perform(post("/api/training/periodizations")
						.header("Authorization", bearer(coachToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(periodizationJson(athleteId)))
				.andExpect(status().isCreated());
	}

	@Test
	void coachNaoGravaPeriodizacaoParaAtletaDeOutroCoach() throws Exception {
		String coachA = registerCoach(mockMvc, uniqueEmail("coach-a"));
		String coachB = registerCoach(mockMvc, uniqueEmail("coach-b"));
		String athleteOfA = registerAthlete(mockMvc, coachA);

		// Coach B tenta criar periodização para um atleta do coach A -> 404.
		mockMvc.perform(post("/api/training/periodizations")
						.header("Authorization", bearer(coachB))
						.contentType(MediaType.APPLICATION_JSON)
						.content(periodizationJson(athleteOfA)))
				.andExpect(status().isNotFound());
	}

	@Test
	void coachNaoLePeriodizacaoDeAtletaDeOutroCoach() throws Exception {
		String coachA = registerCoach(mockMvc, uniqueEmail("coach-a"));
		String coachB = registerCoach(mockMvc, uniqueEmail("coach-b"));
		String athleteOfA = registerAthlete(mockMvc, coachA);

		// Coach B tenta ler as periodizações de um atleta do coach A -> 404.
		mockMvc.perform(get("/api/training/periodizations/athlete/{athleteId}", athleteOfA)
						.header("Authorization", bearer(coachB)))
				.andExpect(status().isNotFound());
	}

	@Test
	void coachIdForjadoNoPayloadNaoConcedeAcessoAAtletaDeOutroCoach() throws Exception {
		String coachA = registerCoach(mockMvc, uniqueEmail("coach-a"));
		String coachB = registerCoach(mockMvc, uniqueEmail("coach-b"));
		String athleteOfA = registerAthlete(mockMvc, coachA);

		// Coach B forja um coachId no body, mas ele é ignorado (resolvido do JWT
		// = coach B) e o atleta não pertence a B -> 404.
		String forged = periodizationJson(athleteOfA)
				.replace("{", "{\"coachId\": \"%s\",".formatted(java.util.UUID.randomUUID().toString()));
		mockMvc.perform(post("/api/training/periodizations")
						.header("Authorization", bearer(coachB))
						.contentType(MediaType.APPLICATION_JSON)
						.content(forged))
				.andExpect(status().isNotFound());
	}

	// ---- T-058 item 3 (revisão): anti-IDOR em endpoints por {id} ----

	@Test
	void coachLePeriodizacaoDoProprioAtletaPorId() throws Exception {
		String coachA = registerCoach(mockMvc, uniqueEmail("coach-a"));
		String athleteOfA = registerAthlete(mockMvc, coachA);
		String periodizationId = createPeriodization(coachA, athleteOfA);

		// Coach A lê a própria periodização pelo id -> 200.
		mockMvc.perform(get("/api/training/periodizations/{id}", periodizationId)
						.header("Authorization", bearer(coachA)))
				.andExpect(status().isOk());
	}

	@Test
	void coachNaoLePeriodizacaoDeOutroCoachPorId() throws Exception {
		String coachA = registerCoach(mockMvc, uniqueEmail("coach-a"));
		String coachB = registerCoach(mockMvc, uniqueEmail("coach-b"));
		String athleteOfA = registerAthlete(mockMvc, coachA);
		String periodizationId = createPeriodization(coachA, athleteOfA);

		// Coach B consulta a periodização do atleta do coach A pelo id -> 404
		// (mesmo status de "não existe": não vaza a existência do recurso).
		mockMvc.perform(get("/api/training/periodizations/{id}", periodizationId)
						.header("Authorization", bearer(coachB)))
				.andExpect(status().isNotFound());
	}

	@Test
	void coachNaoLePlanoDeOutroCoachPorId() throws Exception {
		String coachA = registerCoach(mockMvc, uniqueEmail("coach-a"));
		String coachB = registerCoach(mockMvc, uniqueEmail("coach-b"));
		String athleteOfA = registerAthlete(mockMvc, coachA);
		String periodizationId = createPeriodization(coachA, athleteOfA);
		String planId = createPlan(coachA, athleteOfA, periodizationId);

		// Coach B consulta o plano do atleta do coach A pelo id -> 404.
		mockMvc.perform(get("/api/training/plans/{id}", planId)
						.header("Authorization", bearer(coachB)))
				.andExpect(status().isNotFound());
	}

	@Test
	void coachNaoCriaVersaoDePlanoDeOutroCoachPorId() throws Exception {
		String coachA = registerCoach(mockMvc, uniqueEmail("coach-a"));
		String coachB = registerCoach(mockMvc, uniqueEmail("coach-b"));
		String athleteOfA = registerAthlete(mockMvc, coachA);
		String periodizationId = createPeriodization(coachA, athleteOfA);
		String planId = createPlan(coachA, athleteOfA, periodizationId);

		// Coach B tenta criar versão do plano do coach A pelo id -> 404.
		mockMvc.perform(post("/api/training/plans/{id}/versions", planId)
						.header("Authorization", bearer(coachB))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "newName": "Bloco 2",
								  "newDescription": "Foco em força"
								}
								"""))
				.andExpect(status().isNotFound());
	}

	@Test
	void coachNaoTransicionaPlanoDeOutroCoachPorId() throws Exception {
		String coachA = registerCoach(mockMvc, uniqueEmail("coach-a"));
		String coachB = registerCoach(mockMvc, uniqueEmail("coach-b"));
		String athleteOfA = registerAthlete(mockMvc, coachA);
		String periodizationId = createPeriodization(coachA, athleteOfA);
		String planId = createPlan(coachA, athleteOfA, periodizationId);

		// Coach B tenta transicionar o plano do coach A pelo id -> 404.
		mockMvc.perform(post("/api/training/plans/{id}/transition", planId)
						.header("Authorization", bearer(coachB))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "targetStatus": "IN_REVIEW"
								}
								"""))
				.andExpect(status().isNotFound());
	}

	@Test
	void coachNaoRemovePlanoDeOutroCoachPorId() throws Exception {
		String coachA = registerCoach(mockMvc, uniqueEmail("coach-a"));
		String coachB = registerCoach(mockMvc, uniqueEmail("coach-b"));
		String athleteOfA = registerAthlete(mockMvc, coachA);
		String periodizationId = createPeriodization(coachA, athleteOfA);
		String planId = createPlan(coachA, athleteOfA, periodizationId);

		// Coach B tenta remover o plano do coach A pelo id -> 404.
		mockMvc.perform(delete("/api/training/plans/{id}", planId)
						.header("Authorization", bearer(coachB)))
				.andExpect(status().isNotFound());
	}

	@Test
	void coachNaoLeTreinoDeOutroCoachPorId() throws Exception {
		String coachA = registerCoach(mockMvc, uniqueEmail("coach-a"));
		String coachB = registerCoach(mockMvc, uniqueEmail("coach-b"));
		String athleteOfA = registerAthlete(mockMvc, coachA);
		String periodizationId = createPeriodization(coachA, athleteOfA);
		String trainingId = createTraining(coachA, athleteOfA, periodizationId);

		// Coach B consulta o treino do atleta do coach A pelo id -> 404.
		mockMvc.perform(get("/api/training/trainings/{id}", trainingId)
						.header("Authorization", bearer(coachB)))
				.andExpect(status().isNotFound());
	}

	@Test
	void coachNaoListaTreinosDePeriodizacaoDeOutroCoach() throws Exception {
		String coachA = registerCoach(mockMvc, uniqueEmail("coach-a"));
		String coachB = registerCoach(mockMvc, uniqueEmail("coach-b"));
		String athleteOfA = registerAthlete(mockMvc, coachA);
		String periodizationId = createPeriodization(coachA, athleteOfA);
		createTraining(coachA, athleteOfA, periodizationId);

		// Coach B lista os treinos da periodização do coach A pelo id -> 404.
		mockMvc.perform(get("/api/training/trainings/periodization/{periodizationId}", periodizationId)
						.header("Authorization", bearer(coachB)))
				.andExpect(status().isNotFound());
	}

	@Test
	void coachNaoAdicionaExercicioEmTreinoDeOutroCoachPorId() throws Exception {
		String coachA = registerCoach(mockMvc, uniqueEmail("coach-a"));
		String coachB = registerCoach(mockMvc, uniqueEmail("coach-b"));
		String athleteOfA = registerAthlete(mockMvc, coachA);
		String periodizationId = createPeriodization(coachA, athleteOfA);
		String trainingId = createTraining(coachA, athleteOfA, periodizationId);

		// Coach B tenta prescrever exercício no treino do coach A pelo id -> 404.
		mockMvc.perform(post("/api/training/trainings/{id}/exercises", trainingId)
						.header("Authorization", bearer(coachB))
						.contentType(MediaType.APPLICATION_JSON)
						.content(exerciseJson()))
				.andExpect(status().isNotFound());
	}

	@Test
	void coachNaoReordenaExerciciosDeTreinoDeOutroCoachPorId() throws Exception {
		String coachA = registerCoach(mockMvc, uniqueEmail("coach-a"));
		String coachB = registerCoach(mockMvc, uniqueEmail("coach-b"));
		String athleteOfA = registerAthlete(mockMvc, coachA);
		String periodizationId = createPeriodization(coachA, athleteOfA);
		String trainingId = createTraining(coachA, athleteOfA, periodizationId);

		// Coach B tenta reordenar os exercícios do treino do coach A pelo id -> 404.
		mockMvc.perform(put("/api/training/trainings/{id}/exercises/reorder", trainingId)
						.header("Authorization", bearer(coachB))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "orderedPrescriptionIds": []
								}
								"""))
				.andExpect(status().isNotFound());
	}

	@Test
	void coachNaoTransicionaTreinoDeOutroCoachPorId() throws Exception {
		String coachA = registerCoach(mockMvc, uniqueEmail("coach-a"));
		String coachB = registerCoach(mockMvc, uniqueEmail("coach-b"));
		String athleteOfA = registerAthlete(mockMvc, coachA);
		String periodizationId = createPeriodization(coachA, athleteOfA);
		String trainingId = createTraining(coachA, athleteOfA, periodizationId);

		// Coach B tenta transicionar o treino do coach A pelo id -> 404.
		mockMvc.perform(post("/api/training/trainings/{id}/transition", trainingId)
						.header("Authorization", bearer(coachB))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "targetStatus": "IN_REVIEW"
								}
								"""))
				.andExpect(status().isNotFound());
	}

	@Test
	void coachNaoRemoveTreinoDeOutroCoachPorId() throws Exception {
		String coachA = registerCoach(mockMvc, uniqueEmail("coach-a"));
		String coachB = registerCoach(mockMvc, uniqueEmail("coach-b"));
		String athleteOfA = registerAthlete(mockMvc, coachA);
		String periodizationId = createPeriodization(coachA, athleteOfA);
		String trainingId = createTraining(coachA, athleteOfA, periodizationId);

		// Coach B tenta remover o treino do coach A pelo id -> 404.
		mockMvc.perform(delete("/api/training/trainings/{id}", trainingId)
						.header("Authorization", bearer(coachB)))
				.andExpect(status().isNotFound());
	}
}
