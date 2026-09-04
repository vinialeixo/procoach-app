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
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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
				  "fat": 80,
				  "createdBy": "33333333-3333-3333-3333-333333333333"
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
				  "fat": 80,
				  "createdBy": "33333333-3333-3333-3333-333333333333"
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
				  "fat": 80,
				  "createdBy": "33333333-3333-3333-3333-333333333333"
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
}
