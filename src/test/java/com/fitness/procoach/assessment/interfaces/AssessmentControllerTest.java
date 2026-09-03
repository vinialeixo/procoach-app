package com.fitness.procoach.assessment.interfaces;

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
 * Testes de integração dos endpoints de avaliação física (US-004):
 * POST de registro (201 + Location + DTO), validação (400) e GET de histórico
 * do atleta em ordem cronológica, sem sobrescrever avaliações anteriores.
 */
@SpringBootTest(classes = ProcoachApplication.class)
@AutoConfigureMockMvc
@Import(PostgresTestcontainersConfiguration.class)
@WithMockUser
class AssessmentControllerTest {

	@Autowired
	private MockMvc mockMvc;

	private String validRequestJson(String athleteId, String assessmentDate) {
		return """
				{
				  "athleteId": "%s",
				  "coachId": "22222222-2222-2222-2222-222222222222",
				  "assessmentDate": "%s",
				  "weightKg": 82.50,
				  "heightCm": 178.00,
				  "bodyFatPercentage": 14.50,
				  "muscleMassKg": 68.00,
				  "waistCm": 82.00,
				  "hipCm": 96.00
				}
				""".formatted(athleteId, assessmentDate);
	}

	@Test
	void registraAvaliacaoComSucesso() throws Exception {
		String athleteId = UUID.randomUUID().toString();

		mockMvc.perform(post("/api/v1/assessments")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRequestJson(athleteId, "2026-09-01")))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location",
						org.hamcrest.Matchers.matchesRegex(".*/api/v1/assessments/[0-9a-f-]{36}")))
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.athleteId").value(athleteId))
				.andExpect(jsonPath("$.coachId").value("22222222-2222-2222-2222-222222222222"))
				.andExpect(jsonPath("$.assessmentDate").value("2026-09-01"))
				.andExpect(jsonPath("$.weightKg").value(82.50))
				.andExpect(jsonPath("$.heightCm").value(178.00))
				.andExpect(jsonPath("$.bodyFatPercentage").value(14.50));
	}

	@Test
	void rejeitaRequisicaoSemDataObrigatoria() throws Exception {
		String invalid = """
				{
				  "athleteId": "%s",
				  "coachId": "22222222-2222-2222-2222-222222222222",
				  "weightKg": 82.50,
				  "heightCm": 178.00
				}
				""".formatted(UUID.randomUUID().toString());

		mockMvc.perform(post("/api/v1/assessments")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(invalid))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fields.assessmentDate").exists());
	}

	@Test
	void rejeitaMedidasInvalidas() throws Exception {
		String invalid = """
				{
				  "athleteId": "%s",
				  "coachId": "22222222-2222-2222-2222-222222222222",
				  "assessmentDate": "2026-09-01",
				  "weightKg": -5,
				  "heightCm": 0
				}
				""".formatted(UUID.randomUUID().toString());

		mockMvc.perform(post("/api/v1/assessments")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(invalid))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fields.weightKg").exists())
				.andExpect(jsonPath("$.fields.heightCm").exists());
	}

	@Test
	void retornaHistoricoEmOrdemCronologicaSemSobrescrever() throws Exception {
		String athleteId = UUID.randomUUID().toString();

		mockMvc.perform(post("/api/v1/assessments").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRequestJson(athleteId, "2026-09-01")))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/v1/assessments").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRequestJson(athleteId, "2026-08-01")))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/v1/assessments").with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRequestJson(athleteId, "2026-07-01")))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/assessments").param("athleteId", athleteId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(3))
				.andExpect(jsonPath("$[0].assessmentDate").value("2026-07-01"))
				.andExpect(jsonPath("$[1].assessmentDate").value("2026-08-01"))
				.andExpect(jsonPath("$[2].assessmentDate").value("2026-09-01"));
	}

	@Test
	void retorna404ParaAvaliacaoInexistente() throws Exception {
		mockMvc.perform(get("/api/v1/assessments/{id}", UUID.randomUUID()))
				.andExpect(status().isNotFound());
	}
}
