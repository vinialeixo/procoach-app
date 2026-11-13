package com.fitness.procoach.nutrition.interfaces;

import static com.fitness.procoach.CoachAthleteTestSupport.bearer;
import static com.fitness.procoach.CoachAthleteTestSupport.registerCoach;
import static com.fitness.procoach.CoachAthleteTestSupport.uniqueEmail;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitness.procoach.ProcoachApplication;
import com.fitness.procoach.athlete.infrastructure.PostgresTestcontainersConfiguration;
import com.fitness.procoach.audit.application.AuditService;
import com.fitness.procoach.audit.domain.AuditLog;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
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
 * Testes de integração do catálogo de alimentos TACO (T-086): busca paginada
 * com macros por 100 g, seed coberto pela migração, criação de alimento CUSTOM
 * vinculado ao coach do JWT, auditoria e isolamento entre coaches.
 *
 * <p>Usa token JWT real (nunca {@code @WithMockUser}) para exercitar a
 * resolução server-side do coachId a partir do subject do JWT.</p>
 */
@SpringBootTest(classes = ProcoachApplication.class)
@AutoConfigureMockMvc
@Import(PostgresTestcontainersConfiguration.class)
class FoodCatalogIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AuditService auditService;

	private String customRequestJson(String name) {
		return """
				{
				  "name": "%s",
				  "category": "Suplementos",
				  "calories": 380,
				  "protein": 80,
				  "carbohydrates": 5,
				  "fat": 4,
				  "ediblePortionPercent": 100,
				  "householdMeasure": "1 scoop",
				  "householdMeasureGrams": 30,
				  "stateDefault": "AS_SERVED"
				}
				""".formatted(name);
	}

	private String createCustom(String coachToken, String name) throws Exception {
		MvcResult result = mockMvc
				.perform(post("/api/v1/foods")
						.header("Authorization", bearer(coachToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(customRequestJson(name)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	@Test
	void buscaPorNomeRetornaAlimentoTacoComMacrosPor100g() throws Exception {
		String coachToken = registerCoach(mockMvc, uniqueEmail("coach"));

		mockMvc.perform(get("/api/v1/foods")
						.header("Authorization", bearer(coachToken))
						.param("query", "frango")
						.param("page", "0")
						.param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].name").value("Peito de frango, sem pele, cru"))
				.andExpect(jsonPath("$.content[0].source").value("TACO"))
				.andExpect(jsonPath("$.content[0].stateDefault").value("RAW"))
				.andExpect(jsonPath("$.content[0].macrosPer100g.calories").value(119.0))
				.andExpect(jsonPath("$.content[0].macrosPer100g.protein").value(21.5))
				.andExpect(jsonPath("$.content[0].macrosPer100g.carbohydrates").value(0.0))
				.andExpect(jsonPath("$.content[0].macrosPer100g.fat").value(3.0))
				.andExpect(jsonPath("$.content[0].ediblePortionPercent").value(100.0))
				.andExpect(jsonPath("$.content[0].householdMeasure").value("1 filé médio"))
				.andExpect(jsonPath("$.content[0].householdMeasureGrams").value(100.0))
				.andExpect(jsonPath("$.content[0].tacoId").value("taco:frango-peito-sem-pele-cru"))
				.andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(1)));
	}

	@Test
	void buscaFiltraPorCategoria() throws Exception {
		String coachToken = registerCoach(mockMvc, uniqueEmail("coach"));

		mockMvc.perform(get("/api/v1/foods")
						.header("Authorization", bearer(coachToken))
						.param("category", "Ovos"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].name").value("Ovo, de galinha, inteiro, cru"))
				.andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(1)));
	}

	@Test
	void criaCustomVinculadoAoCoachDoJwtEEAudita() throws Exception {
		String coachToken = registerCoach(mockMvc, uniqueEmail("coach"));

		MvcResult result = mockMvc
				.perform(post("/api/v1/foods")
						.header("Authorization", bearer(coachToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(customRequestJson("Whey do coach autenticado")))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location",
						org.hamcrest.Matchers.matchesRegex(".*/api/v1/foods/[0-9a-f-]{36}")))
				.andExpect(jsonPath("$.source").value("CUSTOM"))
				.andExpect(jsonPath("$.coachApproved").value(true))
				.andExpect(jsonPath("$.coachId").isNotEmpty())
				.andExpect(jsonPath("$.stateDefault").value("AS_SERVED"))
				.andExpect(jsonPath("$.householdMeasure").value("1 scoop"))
				.andReturn();

		String id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
		List<AuditLog> audit = auditService.findByAggregate(UUID.fromString(id));
		assertTrue(audit.stream().anyMatch(log -> "FOOD_CUSTOM_CREATED".equals(log.getEventType())
				&& "Food".equals(log.getAggregateType())));
	}

	@Test
	void customDeUmCoachNaoApareceParaOutro() throws Exception {
		String coachA = registerCoach(mockMvc, uniqueEmail("coach-a"));
		String coachB = registerCoach(mockMvc, uniqueEmail("coach-b"));
		String uniqueName = "Whey exclusivo " + UUID.randomUUID();
		String foodId = createCustom(coachA, uniqueName);

		mockMvc.perform(get("/api/v1/foods")
						.header("Authorization", bearer(coachA))
						.param("query", uniqueName))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].id").value(foodId));

		mockMvc.perform(get("/api/v1/foods")
						.header("Authorization", bearer(coachB))
						.param("query", uniqueName))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(0));

		mockMvc.perform(get("/api/v1/foods/{id}", foodId)
						.header("Authorization", bearer(coachB)))
				.andExpect(status().isNotFound());
	}

	@Test
	void buscaEhPaginada() throws Exception {
		String coachToken = registerCoach(mockMvc, uniqueEmail("coach"));
		String prefix = "Custom paginado " + UUID.randomUUID();
		createCustom(coachToken, prefix + " 1");
		createCustom(coachToken, prefix + " 2");
		createCustom(coachToken, prefix + " 3");

		mockMvc.perform(get("/api/v1/foods")
						.header("Authorization", bearer(coachToken))
						.param("query", prefix)
						.param("page", "0")
						.param("size", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(2))
				.andExpect(jsonPath("$.size").value(2))
				.andExpect(jsonPath("$.totalElements").value(3))
				.andExpect(jsonPath("$.totalPages").value(2));
	}

	@Test
	void rejeitaCustomSemNome() throws Exception {
		String coachToken = registerCoach(mockMvc, uniqueEmail("coach"));
		String invalid = """
				{
				  "calories": 380,
				  "protein": 80,
				  "carbohydrates": 5,
				  "fat": 4,
				  "stateDefault": "AS_SERVED"
				}
				""";

		mockMvc.perform(post("/api/v1/foods")
						.header("Authorization", bearer(coachToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(invalid))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fields.name").exists());
	}

	@Test
	void rejeitaMacroNegativo() throws Exception {
		String coachToken = registerCoach(mockMvc, uniqueEmail("coach"));
		String invalid = customRequestJson("Whey invalido").replace("\"fat\": 4", "\"fat\": -1");

		mockMvc.perform(post("/api/v1/foods")
						.header("Authorization", bearer(coachToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(invalid))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fields.fat").exists());
	}

	@Test
	void seedDaTacoTemOsAlimentosPrioritarios() throws Exception {
		String coachToken = registerCoach(mockMvc, uniqueEmail("coach"));

		for (String query : new String[] { "Peito de frango", "Arroz, tipo 1", "Ovo, de galinha",
				"Aveia, flocos", "Batata-doce" }) {
			MvcResult result = mockMvc.perform(get("/api/v1/foods")
							.header("Authorization", bearer(coachToken))
							.param("query", query))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(1)))
					.andReturn();
			Number total = JsonPath.read(result.getResponse().getContentAsString(), "$.totalElements");
			assertEquals(1, total.intValue(), "seed duplicado para " + query);
		}
	}
}
