package com.fitness.procoach.nutrition.interfaces;

import static com.fitness.procoach.CoachAthleteTestSupport.bearer;
import static com.fitness.procoach.CoachAthleteTestSupport.registerAthlete;
import static com.fitness.procoach.CoachAthleteTestSupport.registerCoach;
import static com.fitness.procoach.CoachAthleteTestSupport.uniqueEmail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
 * Testes de integração para proposta de plano alimentar gerado por IA (T-088).
 * Cobre: criação de proposta (persiste como DRAFT), validação dry-run,
 * auditoria AI_PLAN_PROPOSED, regras de aprovação do coach.
 */
@SpringBootTest(classes = ProcoachApplication.class)
@AutoConfigureMockMvc
@Import(PostgresTestcontainersConfiguration.class)
class AiPlanProposalControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void proposeAiPlan_primeiraDieta_criaDietaEmDraftComAuditoria() throws Exception {
		String coachToken = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coachToken);

		// Registra avaliação para balanço energético
		registerAssessment(coachToken, athleteId);

		String request = aiPlanProposalRequest(athleteId, null);

		MvcResult result = mockMvc.perform(post("/api/v1/diets/ai-proposal")
						.header("Authorization", bearer(coachToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(request))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.dietId").exists())
				.andExpect(jsonPath("$.version").value(1))
				.andExpect(jsonPath("$.status").value("DRAFT"))
				.andExpect(jsonPath("$.requiresCoachApproval").value(true))
				.andExpect(jsonPath("$.objective").value("CUTTING"))
				.andExpect(jsonPath("$.meals").isArray())
				.andExpect(jsonPath("$.meals.length()").value(1))
				.andExpect(jsonPath("$.warnings").isArray())
				.andReturn();

		String dietId = JsonPath.read(result.getResponse().getContentAsString(), "$.dietId");

		// Verifica auditoria AI_PLAN_PROPOSED via endpoint do atleta (GET)
		mockMvc.perform(get("/api/v1/athletes/{id}/history", athleteId)
						.header("Authorization", bearer(coachToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.eventType=='AI_PLAN_PROPOSED')]").exists());
	}

	@Test
	void proposeAiPlan_novaVersao_encadeiaVersaoAnterior() throws Exception {
		String coachToken = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coachToken);

		// Cria dieta inicial
		String v1Id = createDiet(coachToken, athleteId);

		// Registra avaliação
		registerAssessment(coachToken, athleteId);

		// Propõe nova versão via IA
		String request = aiPlanProposalRequest(athleteId, v1Id);

		MvcResult result = mockMvc.perform(post("/api/v1/diets/ai-proposal")
						.header("Authorization", bearer(coachToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(request))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.version").value(2))
				.andExpect(jsonPath("$.previousVersionId").value(v1Id))
				.andExpect(jsonPath("$.status").value("DRAFT"))
				.andExpect(jsonPath("$.requiresCoachApproval").value(true))
				.andReturn();

		String v2Id = JsonPath.read(result.getResponse().getContentAsString(), "$.dietId");

		// V1 deve estar readOnly
		mockMvc.perform(post("/api/v1/diets/{id}/meals", v1Id).header("Authorization", bearer(coachToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(""" 
							{ "name": "Teste", "order": 1, "time": "08:00" }
							"""))
				.andExpect(status().isConflict()); // 409 - read-only

		// Verifica auditoria (GET) - data é uma string JSON, verificamos se contém o baseDietVersionId
		mockMvc.perform(get("/api/v1/athletes/{id}/history", athleteId)
						.header("Authorization", bearer(coachToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.eventType=='AI_PLAN_PROPOSED')].data").value(
						org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.containsString(v1Id))));
	}

	@Test
	void validateAiPlan_dryRun_retornaPropostaEnriquecidaSemPersistir() throws Exception {
		String coachToken = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coachToken);

		// Registra avaliação para balanço
		registerAssessment(coachToken, athleteId);

		String request = aiPlanProposalRequest(athleteId, null);

		mockMvc.perform(post("/api/v1/diets/ai-proposal/validate")
						.header("Authorization", bearer(coachToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(request))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dietId").doesNotExist()) // não persistido
				.andExpect(jsonPath("$.version").value(1))
				.andExpect(jsonPath("$.status").value("DRAFT"))
				.andExpect(jsonPath("$.requiresCoachApproval").value(true))
				.andExpect(jsonPath("$.warnings").isArray())
				.andExpect(jsonPath("$.derived").exists())
				.andExpect(jsonPath("$.derived.proteinGPerKg").exists())
				.andExpect(jsonPath("$.derived.isConsistent").exists());

		// Verifica que NÃO foi persistido (sem dieta no banco) - usa GET
		mockMvc.perform(get("/api/v1/diets").param("athleteId", athleteId)
						.header("Authorization", bearer(coachToken)))
				.andExpect(status().isNotFound()); // 404 pois não há dieta
	}

	@Test
	void proposeAiPlan_funcionaSemAvaliacaoPoisNaoCalculaBalanco() throws Exception {
		String coachToken = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coachToken);

		// SEM registrar avaliação - deve funcionar pois proposeAiPlan não calcula balanço
		String request = aiPlanProposalRequest(athleteId, null);

		mockMvc.perform(post("/api/v1/diets/ai-proposal")
						.header("Authorization", bearer(coachToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(request))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("DRAFT"));
	}

	@Test
	void proposeAiPlan_rejeitaAtletaJaTemDietaSemBaseVersion() throws Exception {
		String coachToken = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coachToken);

		// Cria dieta inicial
		createDiet(coachToken, athleteId);
		registerAssessment(coachToken, athleteId);

		// Tenta criar outra sem baseDietVersionId
		String request = aiPlanProposalRequest(athleteId, null);

		mockMvc.perform(post("/api/v1/diets/ai-proposal")
						.header("Authorization", bearer(coachToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(request))
				.andExpect(status().isConflict()); // 409 - já possui dieta
	}

	@Test
	void proposeAiPlan_coachNaoPodeProporParaAtletaDeOutroCoach() throws Exception {
		String coachA = registerCoach(mockMvc, uniqueEmail("coach-a"));
		String coachB = registerCoach(mockMvc, uniqueEmail("coach-b"));
		String athleteOfA = registerAthlete(mockMvc, coachA);
		registerAssessment(coachA, athleteOfA);

		// Coach B tenta propor para atleta do Coach A
		String request = aiPlanProposalRequest(athleteOfA, null);

		mockMvc.perform(post("/api/v1/diets/ai-proposal")
						.header("Authorization", bearer(coachB))
						.contentType(MediaType.APPLICATION_JSON)
						.content(request))
				.andExpect(status().isNotFound()); // 404 - não vaza existência
	}

	@Test
	void validateAiPlan_calculaWarningsEConsistencia() throws Exception {
		String coachToken = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coachToken);
		registerAssessment(coachToken, athleteId);

		// Proposta com carboidrato zero no item - deve gerar warning ZERO_CARBOHYDRATE
		String request = aiPlanProposalRequestZeroCarb(athleteId);

		mockMvc.perform(post("/api/v1/diets/ai-proposal/validate")
						.header("Authorization", bearer(coachToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(request))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.warnings[?(@.code=='ZERO_CARBOHYDRATE')]").exists())
				.andExpect(jsonPath("$.derived.isConsistent").value(true));
	}

	// Helpers

	private String createDiet(String coachToken, String athleteId) throws Exception {
		MvcResult created = mockMvc.perform(post("/api/v1/diets")
						.header("Authorization", bearer(coachToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(""" 
							{ "athleteId": "%s", "objective": "HYPERTROPHY", "calories": 2500,
							  "protein": 180, "carbohydrates": 250, "fat": 80 }
							""".formatted(athleteId)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(created.getResponse().getContentAsString(), "$.id");
	}

	private void registerAssessment(String coachToken, String athleteId) throws Exception {
		mockMvc.perform(post("/api/v1/assessments")
						.header("Authorization", bearer(coachToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(""" 
							{ "athleteId": "%s", "assessmentDate": "2026-09-01",
							  "weightKg": 75, "heightCm": 180 }
							""".formatted(athleteId)))
				.andExpect(status().isCreated());
	}

	private String aiPlanProposalRequest(String athleteId, String baseDietVersionId) {
		return aiPlanProposalRequest(athleteId, baseDietVersionId, 2500, 180, 250, 80);
	}

	private String aiPlanProposalRequest(String athleteId, String baseDietVersionId,
			double calories, double protein, double carbs, double fat) {
		String baseVersionJson = baseDietVersionId != null
				? "\"baseDietVersionId\": \"%s\"".formatted(baseDietVersionId)
				: "";

		return """ 
			{
			  "schemaVersion": "1.0",
			  "meta": {
			    "generatedAt": "2026-11-11T12:00:00Z",
			    "source": "AI_ASSISTANT",
			    "requiresCoachApproval": true
			    %s
			  },
			  "athlete": {
			    "athleteId": "%s",
			    "name": "Atleta Teste",
			    "sex": "MALE",
			    "age": 25,
			    "heightCm": 180,
			    "weightKg": 75.0,
			    "bodyFatPercent": 15.0
			  },
			  "strategy": {
			    "objective": "CUTTING",
			    "phase": "PRE_CONTEST",
			    "trainingDay": true,
			    "notes": "Deficit moderado com alta proteina"
			  },
			  "targets": {
			    "calories": %.0f,
			    "protein": %.0f,
			    "carbohydrates": %.0f,
			    "fat": %.0f,
			    "fiber": 30,
			    "waterMl": 3500
			  },
			  "derived": {
			    "tmbKcal": 1800,
			    "getKcal": 2400,
			    "balanceKcal": 100,
			    "balancePercent": 4.2,
			    "classification": "SURPLUS",
			    "proteinGPerKg": 2.4,
			    "carbohydratesGPerKg": 3.3,
			    "fatGPerKg": 1.1,
			    "proteinPctKcal": 28.8,
			    "carbohydratesPctKcal": 40.0,
			    "fatPctKcal": 28.8
			  },
			  "consistency": {
			    "calculatedCalories": 2500,
			    "declaredCalories": 2500,
			    "deltaKcal": 0,
			    "deltaPercent": 0,
			    "isConsistent": true
			  },
			  "planMode": "MEAL_PLAN",
			  "meals": [
			    {
			      "mealId": "meal-1",
			      "name": "Cafe da manha",
			      "order": 1,
			      "time": "07:00",
			      "notes": "Pos-jejum",
			      "target": { "calories": 600, "protein": 40, "carbohydrates": 60, "fat": 20 },
			      "items": [
			        {
			          "foodId": "taco:ovo-de-galinha-inteiro",
			          "name": "Ovo, de galinha, inteiro",
			          "source": "TACO",
			          "kind": "FOOD",
			          "state": "RAW",
			          "quantityG": 100,
			          "householdMeasure": "2 unidades",
			          "preparation": "cozido",
			          "macros": { "calories": 143, "protein": 13.0, "carbohydrates": 1.6, "fat": 9.5 }
			        }
			      ],
			      "totals": { "calories": 143, "protein": 13.0, "carbohydrates": 1.6, "fat": 9.5 }
			    }
			  ],
			  "supplements": [
			    { "name": "Creatina monoidratada", "dose": "5 g", "timing": "qualquer horario", "countsTowardMacros": false }
			  ],
			  "warnings": [],
			  "totals": { "calories": 2500, "protein": 180, "carbohydrates": 250, "fat": 80 },
			  "disclaimer": "Plano consultivo. A decisao final e a aprovacao sao do coach."
			}
			""".formatted(baseVersionJson.isEmpty() ? "" : ", " + baseVersionJson, athleteId, calories, protein, carbs, fat);
	} 
	
	/**
	 * Request com item de zero carboidrato (ex.: apenas proteína/gordura).
	 */
	private String aiPlanProposalRequestZeroCarb(String athleteId) {
		return """ 
			{
			  "schemaVersion": "1.0",
			  "meta": {
			    "generatedAt": "2026-11-11T12:00:00Z",
			    "source": "AI_ASSISTANT",
			    "requiresCoachApproval": true
			  },
			  "athlete": {
			    "athleteId": "%s",
			    "name": "Atleta Teste",
			    "sex": "MALE",
			    "age": 25,
			    "heightCm": 180,
			    "weightKg": 75.0,
			    "bodyFatPercent": 15.0
			  },
			  "strategy": {
			    "objective": "CUTTING",
			    "phase": "PRE_CONTEST",
			    "trainingDay": true,
			    "notes": "Cetogenica - zero carboidrato"
			  },
			  "targets": {
			    "calories": 2000,
			    "protein": 150,
			    "carbohydrates": 0,
			    "fat": 150,
			    "fiber": 10,
			    "waterMl": 3500
			  },
			  "derived": {
			    "tmbKcal": 1800,
			    "getKcal": 2400,
			    "balanceKcal": -400,
			    "balancePercent": -16.7,
			    "classification": "DEFICIT",
			    "proteinGPerKg": 2.0,
			    "carbohydratesGPerKg": 0.0,
			    "fatGPerKg": 2.0,
			    "proteinPctKcal": 30.0,
			    "carbohydratesPctKcal": 0.0,
			    "fatPctKcal": 70.0
			  },
			  "consistency": {
			    "calculatedCalories": 2000,
			    "declaredCalories": 2000,
			    "deltaKcal": 0,
			    "deltaPercent": 0,
			    "isConsistent": true
			  },
			  "planMode": "MEAL_PLAN",
			  "meals": [
			    {
			      "mealId": "meal-1",
			      "name": "Cafe da manha",
			      "order": 1,
			      "time": "07:00",
			      "notes": "Cetogenico",
			      "target": { "calories": 500, "protein": 40, "carbohydrates": 0, "fat": 40 },
			      "items": [
			        {
			          "foodId": "taco:ovo-de-galinha-inteiro",
			          "name": "Ovo, de galinha, inteiro",
			          "source": "TACO",
			          "kind": "FOOD",
			          "state": "RAW",
			          "quantityG": 100,
			          "householdMeasure": "2 unidades",
			          "preparation": "cozido",
			          "macros": { "calories": 143, "protein": 13.0, "carbohydrates": 0.0, "fat": 9.5 }
			        },
			        {
			          "foodId": "taco:abacate",
			          "name": "Abacate",
			          "source": "TACO",
			          "kind": "FOOD",
			          "state": "AS_SERVED",
			          "quantityG": 100,
			          "householdMeasure": "1/2 unidade",
			          "preparation": null,
			          "macros": { "calories": 160, "protein": 2.0, "carbohydrates": 0.0, "fat": 15.0 }
			        }
			      ],
			      "totals": { "calories": 303, "protein": 15.0, "carbohydrates": 0.0, "fat": 24.5 }
			    }
			  ],
			  "supplements": [],
			  "warnings": [],
			  "totals": { "calories": 2000, "protein": 150, "carbohydrates": 0, "fat": 150 },
			  "disclaimer": "Plano consultivo. A decisao final e a aprovacao sao do coach."
			}
			""".formatted(athleteId);
	}
}