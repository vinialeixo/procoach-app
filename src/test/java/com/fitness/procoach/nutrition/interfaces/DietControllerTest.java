package com.fitness.procoach.nutrition.interfaces;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Testes de integração dos endpoints de dieta (US-007): POST de criação da
 * primeira versão (201 + Location + DTO), validação (400) e GET da versão mais
 * recente / histórico do atleta.
 */
@SpringBootTest(classes = ProcoachApplication.class)
@AutoConfigureMockMvc
@Import(PostgresTestcontainersConfiguration.class)
@WithMockUser
class DietControllerTest {

	@Autowired
	private MockMvc mockMvc;

	private String validRequestJson(String athleteId) {
		return """
				{
				  "coachId": "22222222-2222-2222-2222-222222222222",
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
		String athleteId = UUID.randomUUID().toString();

		mockMvc.perform(post("/api/v1/diets")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRequestJson(athleteId)))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location",
						org.hamcrest.Matchers.matchesRegex(".*/api/v1/diets/[0-9a-f-]{36}")))
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.athleteId").value(athleteId))
				.andExpect(jsonPath("$.coachId").value("22222222-2222-2222-2222-222222222222"))
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
		String invalid = """
				{
				  "coachId": "22222222-2222-2222-2222-222222222222",
				  "athleteId": "%s",
				  "calories": 2500,
				  "protein": 180,
				  "carbohydrates": 250,
				  "fat": 80,
				  "createdBy": "33333333-3333-3333-3333-333333333333"
				}
				""".formatted(UUID.randomUUID().toString());

		mockMvc.perform(post("/api/v1/diets")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(invalid))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fields.objective").exists());
	}

	@Test
	void rejeitaMacronutrientesInvalidos() throws Exception {
		String invalid = """
				{
				  "coachId": "22222222-2222-2222-2222-222222222222",
				  "athleteId": "%s",
				  "objective": "HYPERTROPHY",
				  "calories": -100,
				  "protein": 0,
				  "carbohydrates": 250,
				  "fat": 80,
				  "createdBy": "33333333-3333-3333-3333-333333333333"
				}
				""".formatted(UUID.randomUUID().toString());

		mockMvc.perform(post("/api/v1/diets")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(invalid))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fields.calories").exists())
				.andExpect(jsonPath("$.fields.protein").exists());
	}

	@Test
	void retornaVersaoMaisRecenteDoAtleta() throws Exception {
		String athleteId = UUID.randomUUID().toString();

		mockMvc.perform(post("/api/v1/diets").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRequestJson(athleteId)))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/diets").param("athleteId", athleteId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.athleteId").value(athleteId))
				.andExpect(jsonPath("$.version").value(1));
	}

	@Test
	void retornaHistoricoDeVersoesDoAtleta() throws Exception {
		String athleteId = UUID.randomUUID().toString();

		mockMvc.perform(post("/api/v1/diets").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRequestJson(athleteId)))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/diets/history").param("athleteId", athleteId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].version").value(1));
	}

	@Test
	void retorna404ParaDietaInexistente() throws Exception {
		mockMvc.perform(get("/api/v1/diets/{id}", UUID.randomUUID()))
				.andExpect(status().isNotFound());
	}
}
