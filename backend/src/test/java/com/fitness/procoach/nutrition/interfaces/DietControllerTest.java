package com.fitness.procoach.nutrition.interfaces;

import static com.fitness.procoach.CoachAthleteTestSupport.bearer;
import static com.fitness.procoach.CoachAthleteTestSupport.registerAthlete;
import static com.fitness.procoach.CoachAthleteTestSupport.registerCoach;
import static com.fitness.procoach.CoachAthleteTestSupport.uniqueEmail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitness.procoach.ProcoachApplication;
import com.fitness.procoach.athlete.infrastructure.PostgresTestcontainersConfiguration;
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

/**
 * Testes de integração dos endpoints de dieta (US-007, T-058): POST de criação
 * da primeira versão (201 + Location + DTO), validação (400), GET da versão
 * mais recente / histórico e autorização — um coach não grava/lê dieta de
 * atleta de outro coach.
 *
 * <p>Usa token JWT real (nunca {@code @WithMockUser}) para exercitar a
 * resolução server-side do coachId a partir do subject do JWT.</p>
 */
@SpringBootTest(classes = ProcoachApplication.class)
@AutoConfigureMockMvc
@Import(PostgresTestcontainersConfiguration.class)
class DietControllerTest {

	@Autowired
	private MockMvc mockMvc;

	private String validRequestJson(String athleteId) {
		return """
				{
				  "athleteId": "%s",
				  "objective": "HYPERTROPHY",
				  "calories": 2500,
				  "protein": 180,
				  "carbohydrates": 250,
				  "fat": 80
				}
				""".formatted(athleteId);
	}

	@Test
	void criaPrimeiraVersaoComSucesso() throws Exception {
		String coachToken = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coachToken);

		mockMvc.perform(post("/api/v1/diets")
						.header("Authorization", bearer(coachToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRequestJson(athleteId)))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location",
						org.hamcrest.Matchers.matchesRegex(".*/api/v1/diets/[0-9a-f-]{36}")))
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.athleteId").value(athleteId))
				.andExpect(jsonPath("$.coachId").isNotEmpty())
				.andExpect(jsonPath("$.objective").value("HYPERTROPHY"))
				.andExpect(jsonPath("$.calories").value(2500.0))
				.andExpect(jsonPath("$.protein").value(180.0))
				.andExpect(jsonPath("$.carbohydrates").value(250.0))
				.andExpect(jsonPath("$.fat").value(80.0))
				.andExpect(jsonPath("$.status").value("DRAFT"))
				.andExpect(jsonPath("$.version").value(1))
				.andExpect(jsonPath("$.readOnly").value(false))
				.andExpect(jsonPath("$.deleted").value(false));
	}

	@Test
	void rejeitaRequisicaoSemObjetivo() throws Exception {
		String coachToken = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coachToken);
		String invalid = """
				{
				  "athleteId": "%s",
				  "calories": 2500,
				  "protein": 180,
				  "carbohydrates": 250,
				  "fat": 80
				}
				""".formatted(athleteId);

		mockMvc.perform(post("/api/v1/diets")
						.header("Authorization", bearer(coachToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(invalid))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fields.objective").exists());
	}

	@Test
	void rejeitaMacronutrientesInvalidos() throws Exception {
		String coachToken = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coachToken);
		String invalid = """
				{
				  "athleteId": "%s",
				  "objective": "HYPERTROPHY",
				  "calories": -100,
				  "protein": 0,
				  "carbohydrates": 250,
				  "fat": 80
				}
				""".formatted(athleteId);

		mockMvc.perform(post("/api/v1/diets")
						.header("Authorization", bearer(coachToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(invalid))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fields.calories").exists())
				.andExpect(jsonPath("$.fields.protein").exists());
	}

	@Test
	void retornaVersaoMaisRecenteDoAtleta() throws Exception {
		String coachToken = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coachToken);

		mockMvc.perform(post("/api/v1/diets").header("Authorization", bearer(coachToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRequestJson(athleteId)))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/diets").param("athleteId", athleteId)
						.header("Authorization", bearer(coachToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.athleteId").value(athleteId))
				.andExpect(jsonPath("$.version").value(1));
	}

	@Test
	void retornaHistoricoDeVersoesDoAtleta() throws Exception {
		String coachToken = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coachToken);

		mockMvc.perform(post("/api/v1/diets").header("Authorization", bearer(coachToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRequestJson(athleteId)))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/diets/history").param("athleteId", athleteId)
						.header("Authorization", bearer(coachToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].version").value(1));
	}

	@Test
	void retorna404ParaDietaInexistente() throws Exception {
		String coachToken = registerCoach(mockMvc, uniqueEmail("coach"));
		mockMvc.perform(get("/api/v1/diets/{id}", UUID.randomUUID())
						.header("Authorization", bearer(coachToken)))
				.andExpect(status().isNotFound());
	}

	// ---- T-058 item 2: autorização ----

	@Test
	void coachNaoGravaDietaParaAtletaDeOutroCoach() throws Exception {
		String coachA = registerCoach(mockMvc, uniqueEmail("coach-a"));
		String coachB = registerCoach(mockMvc, uniqueEmail("coach-b"));
		String athleteOfA = registerAthlete(mockMvc, coachA);

		// Coach B tenta criar dieta para um atleta do coach A -> 404.
		mockMvc.perform(post("/api/v1/diets")
						.header("Authorization", bearer(coachB))
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRequestJson(athleteOfA)))
				.andExpect(status().isNotFound());
	}

	@Test
	void coachNaoLeDietaDeAtletaDeOutroCoach() throws Exception {
		String coachA = registerCoach(mockMvc, uniqueEmail("coach-a"));
		String coachB = registerCoach(mockMvc, uniqueEmail("coach-b"));
		String athleteOfA = registerAthlete(mockMvc, coachA);

		// Coach B tenta ler a dieta de um atleta do coach A -> 404.
		mockMvc.perform(get("/api/v1/diets").param("athleteId", athleteOfA)
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
		String forged = validRequestJson(athleteOfA)
				.replace("{", "{\"coachId\": \"%s\",".formatted(UUID.randomUUID().toString()));
		mockMvc.perform(post("/api/v1/diets")
						.header("Authorization", bearer(coachB))
						.contentType(MediaType.APPLICATION_JSON)
						.content(forged))
				.andExpect(status().isNotFound());
	}

	// ---- T-058 item 3 (revisão): anti-IDOR em endpoints por {id} ----

	@Test
	void coachNaoLeDietaDeOutroCoachPorId() throws Exception {
		String coachA = registerCoach(mockMvc, uniqueEmail("coach-a"));
		String coachB = registerCoach(mockMvc, uniqueEmail("coach-b"));
		String athleteOfA = registerAthlete(mockMvc, coachA);

		// Coach A cria uma dieta para o próprio atleta e captura o id.
		MvcResult created = mockMvc.perform(post("/api/v1/diets")
						.header("Authorization", bearer(coachA))
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRequestJson(athleteOfA)))
				.andExpect(status().isCreated())
				.andReturn();
		String dietId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		// Coach B tenta ler a dieta do atleta do coach A pelo id -> 404
		// (mesmo status de "não existe": não vaza a existência do recurso).
		mockMvc.perform(get("/api/v1/diets/{id}", dietId)
						.header("Authorization", bearer(coachB)))
				.andExpect(status().isNotFound());
	}

	// ---- T-030: balanço energético da dieta ----

	private String createDiet(String coachToken, String athleteId) throws Exception {
		MvcResult created = mockMvc.perform(post("/api/v1/diets")
						.header("Authorization", bearer(coachToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRequestJson(athleteId)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(created.getResponse().getContentAsString(), "$.id");
	}

	@Test
	void balancoEnergeticoSemAvaliacaoRetorna409() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String dietId = createDiet(coach, athleteId);

		mockMvc.perform(get("/api/v1/diets/{id}/energy-balance", dietId)
						.header("Authorization", bearer(coach)))
				.andExpect(status().isConflict());
	}

	@Test
	void balancoEnergeticoUsaOGetDaAvaliacaoMaisRecente() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String dietId = createDiet(coach, athleteId);

		// Atleta: mulher nascida em 1995-07-20 -> 31 anos em 2026-09-01.
		// TMB = 10·60 + 6,25·165 − 5·31 − 161 = 1315; GET = 1315,25 × 1,2 -> 1578.
		mockMvc.perform(post("/api/v1/assessments")
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "athleteId": "%s",
								  "assessmentDate": "2026-09-01",
								  "weightKg": 60,
								  "heightCm": 165
								}
								""".formatted(athleteId)))
				.andExpect(status().isCreated());

		// Dieta de 2500 kcal contra GET 1578: superávit de 922 kcal.
		mockMvc.perform(get("/api/v1/diets/{id}/energy-balance", dietId)
						.header("Authorization", bearer(coach)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dietId").value(dietId))
				.andExpect(jsonPath("$.tmbKcal").value(1315))
				.andExpect(jsonPath("$.getKcal").value(1578))
				.andExpect(jsonPath("$.balanceKcal").value(922))
				.andExpect(jsonPath("$.classification").value("SURPLUS"))
				.andExpect(jsonPath("$.basedOnAssessmentDate").value("2026-09-01"));
	}

	@Test
	void coachNaoVeBalancoDeDietaDeOutroCoach() throws Exception {
		String coachA = registerCoach(mockMvc, uniqueEmail("coach-a"));
		String coachB = registerCoach(mockMvc, uniqueEmail("coach-b"));
		String dietId = createDiet(coachA, registerAthlete(mockMvc, coachA));

		mockMvc.perform(get("/api/v1/diets/{id}/energy-balance", dietId)
						.header("Authorization", bearer(coachB)))
				.andExpect(status().isNotFound());
	}

	// ---- T-030: visão do atleta ----

	@Autowired
	private com.fitness.procoach.security.application.AuthService authService;

	private String athleteToken(String athleteId) {
		return authService.registerAthleteCredential(uniqueEmail("atleta"), "senha-Forte-123", java.util.UUID.fromString(athleteId))
				.tokens().accessToken();
	}

	private void registerAssessment(String coach, String athleteId) throws Exception {
		mockMvc.perform(post("/api/v1/assessments")
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "athleteId": "%s",
								  "assessmentDate": "2026-09-01",
								  "weightKg": 60,
								  "heightCm": 165
								}
								""".formatted(athleteId)))
				.andExpect(status().isCreated());
	}

	@Test
	void atletaVeOProprioBalancoEnergetico() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		// O atleta só vê o balanço da dieta que o coach aprovou.
		approve(coach, createDiet(coach, athleteId));
		registerAssessment(coach, athleteId);

		mockMvc.perform(get("/api/v1/diets/energy-balance").param("athleteId", athleteId)
						.header("Authorization", bearer(athleteToken(athleteId))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.getKcal").value(1578))
				.andExpect(jsonPath("$.balanceKcal").value(922))
				.andExpect(jsonPath("$.classification").value("SURPLUS"));
	}

	@Test
	void atletaNaoVeBalancoDeOutroAtleta() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteA = registerAthlete(mockMvc, coach);
		String athleteB = registerAthlete(mockMvc, coach);
		createDiet(coach, athleteB);
		registerAssessment(coach, athleteB);

		mockMvc.perform(get("/api/v1/diets/energy-balance").param("athleteId", athleteB)
						.header("Authorization", bearer(athleteToken(athleteA))))
				.andExpect(status().isForbidden());
	}

	@Test
	void coachVeBalancoDoAtletaDaPropriaCarteiraEOutroCoachNao() throws Exception {
		String coachA = registerCoach(mockMvc, uniqueEmail("coach-a"));
		String coachB = registerCoach(mockMvc, uniqueEmail("coach-b"));
		String athleteId = registerAthlete(mockMvc, coachA);
		createDiet(coachA, athleteId);
		registerAssessment(coachA, athleteId);

		mockMvc.perform(get("/api/v1/diets/energy-balance").param("athleteId", athleteId)
						.header("Authorization", bearer(coachA)))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/diets/energy-balance").param("athleteId", athleteId)
						.header("Authorization", bearer(coachB)))
				.andExpect(status().isNotFound());
	}

	// ---- visão do atleta: só a dieta que o coach aprovou ----

	private void changeDietStatus(String coach, String dietId, String status) throws Exception {
		mockMvc.perform(post("/api/v1/diets/{id}/status", dietId)
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"status\": \"%s\" }".formatted(status)))
				.andExpect(status().isOk());
	}

	private void approve(String coach, String dietId) throws Exception {
		changeDietStatus(coach, dietId, "IN_REVIEW");
		changeDietStatus(coach, dietId, "APPROVED");
	}

	@Test
	void atletaSoVeADietaDepoisDeAprovada() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String athleteToken = athleteToken(athleteId);
		String dietId = createDiet(coach, athleteId);

		// Rascunho: o coach vê, o atleta não.
		mockMvc.perform(get("/api/v1/diets").param("athleteId", athleteId).header("Authorization", bearer(coach)))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/diets").param("athleteId", athleteId)
						.header("Authorization", bearer(athleteToken)))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/v1/diets/history").param("athleteId", athleteId)
						.header("Authorization", bearer(athleteToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isEmpty());

		approve(coach, dietId);

		mockMvc.perform(get("/api/v1/diets").param("athleteId", athleteId)
						.header("Authorization", bearer(athleteToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("APPROVED"));
		mockMvc.perform(get("/api/v1/diets/history").param("athleteId", athleteId)
						.header("Authorization", bearer(athleteToken)))
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void atletaContinuaVendoAAprovadaEnquantoOCoachEditaNovaVersao() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String athleteToken = athleteToken(athleteId);
		String v1 = createDiet(coach, athleteId);
		approve(coach, v1);

		mockMvc.perform(post("/api/v1/diets/{id}/versions", v1)
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "objective": "HYPERTROPHY", "calories": 2600, "protein": 180,
								  "carbohydrates": 260, "fat": 80, "changeReason": "ajuste" }
								"""))
				.andExpect(status().isCreated());

		// Coach vê a v2 em rascunho; o atleta segue com a v1 aprovada.
		mockMvc.perform(get("/api/v1/diets").param("athleteId", athleteId).header("Authorization", bearer(coach)))
				.andExpect(jsonPath("$.version").value(2));
		mockMvc.perform(get("/api/v1/diets").param("athleteId", athleteId)
						.header("Authorization", bearer(athleteToken)))
				.andExpect(jsonPath("$.version").value(1));
	}

	@Test
	void balancoDoAtletaIgnoraDietaEmRascunho() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String athleteToken = athleteToken(athleteId);
		String dietId = createDiet(coach, athleteId);
		registerAssessment(coach, athleteId);

		mockMvc.perform(get("/api/v1/diets/energy-balance").param("athleteId", athleteId)
						.header("Authorization", bearer(athleteToken)))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/v1/diets/energy-balance").param("athleteId", athleteId)
						.header("Authorization", bearer(coach)))
				.andExpect(status().isOk());

		approve(coach, dietId);

		mockMvc.perform(get("/api/v1/diets/energy-balance").param("athleteId", athleteId)
						.header("Authorization", bearer(athleteToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.classification").value("SURPLUS"));
	}
}
