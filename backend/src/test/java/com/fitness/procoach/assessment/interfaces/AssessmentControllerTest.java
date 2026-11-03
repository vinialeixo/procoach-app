package com.fitness.procoach.assessment.interfaces;

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
 * Testes de integração dos endpoints de avaliação física (US-004, T-058):
 * POST de registro (201 + Location + DTO), validação (400), GET de histórico e
 * autorização — um coach não grava/lê avaliação de atleta de outro coach.
 *
 * <p>Usa token JWT real (nunca {@code @WithMockUser}) para exercitar a
 * resolução server-side do coachId a partir do subject do JWT.</p>
 */
@SpringBootTest(classes = ProcoachApplication.class)
@AutoConfigureMockMvc
@Import(PostgresTestcontainersConfiguration.class)
class AssessmentControllerTest {

	@Autowired
	private MockMvc mockMvc;

	private String validRequestJson(String athleteId, String assessmentDate) {
		return """
				{
				  "athleteId": "%s",
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
		String coachToken = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coachToken);

		mockMvc.perform(post("/api/v1/assessments")
						.header("Authorization", bearer(coachToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRequestJson(athleteId, "2026-09-01")))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location",
						org.hamcrest.Matchers.matchesRegex(".*/api/v1/assessments/[0-9a-f-]{36}")))
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.athleteId").value(athleteId))
				.andExpect(jsonPath("$.coachId").isNotEmpty())
				.andExpect(jsonPath("$.assessmentDate").value("2026-09-01"))
				.andExpect(jsonPath("$.weightKg").value(82.50))
				.andExpect(jsonPath("$.heightCm").value(178.00))
				.andExpect(jsonPath("$.bodyFatPercentage").value(14.50))
				// T-025: métricas derivadas calculadas pelo núcleo.
				.andExpect(jsonPath("$.metrics.imc").isNotEmpty())
				.andExpect(jsonPath("$.metrics.tmbKcal").isNotEmpty())
				.andExpect(jsonPath("$.metrics.getKcal").isNotEmpty());
	}

	@Test
	void rejeitaRequisicaoSemDataObrigatoria() throws Exception {
		String coachToken = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coachToken);
		String invalid = """
				{
				  "athleteId": "%s",
				  "weightKg": 82.50,
				  "heightCm": 178.00
				}
				""".formatted(athleteId);

		mockMvc.perform(post("/api/v1/assessments")
						.header("Authorization", bearer(coachToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(invalid))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fields.assessmentDate").exists());
	}

	@Test
	void rejeitaMedidasInvalidas() throws Exception {
		String coachToken = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coachToken);
		String invalid = """
				{
				  "athleteId": "%s",
				  "assessmentDate": "2026-09-01",
				  "weightKg": -5,
				  "heightCm": 0
				}
				""".formatted(athleteId);

		mockMvc.perform(post("/api/v1/assessments")
						.header("Authorization", bearer(coachToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(invalid))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fields.weightKg").exists())
				.andExpect(jsonPath("$.fields.heightCm").exists());
	}

	@Test
	void retornaHistoricoEmOrdemCronologicaSemSobrescrever() throws Exception {
		String coachToken = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coachToken);

		mockMvc.perform(post("/api/v1/assessments").header("Authorization", bearer(coachToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRequestJson(athleteId, "2026-09-01")))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/v1/assessments").header("Authorization", bearer(coachToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRequestJson(athleteId, "2026-08-01")))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/v1/assessments").header("Authorization", bearer(coachToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRequestJson(athleteId, "2026-07-01")))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/assessments").param("athleteId", athleteId)
						.header("Authorization", bearer(coachToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(3))
				.andExpect(jsonPath("$[0].assessmentDate").value("2026-07-01"))
				.andExpect(jsonPath("$[1].assessmentDate").value("2026-08-01"))
				.andExpect(jsonPath("$[2].assessmentDate").value("2026-09-01"));
	}

	@Test
	void retorna404ParaAvaliacaoInexistente() throws Exception {
		String coachToken = registerCoach(mockMvc, uniqueEmail("coach"));
		mockMvc.perform(get("/api/v1/assessments/{id}", UUID.randomUUID())
						.header("Authorization", bearer(coachToken)))
				.andExpect(status().isNotFound());
	}

	// ---- T-058 item 2: autorização ----

	@Test
	void coachNaoGravaAvaliacaoParaAtletaDeOutroCoach() throws Exception {
		String coachA = registerCoach(mockMvc, uniqueEmail("coach-a"));
		String coachB = registerCoach(mockMvc, uniqueEmail("coach-b"));
		String athleteOfA = registerAthlete(mockMvc, coachA);

		// Coach B tenta registrar avaliação para um atleta do coach A -> 404.
		mockMvc.perform(post("/api/v1/assessments")
						.header("Authorization", bearer(coachB))
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRequestJson(athleteOfA, "2026-09-01")))
				.andExpect(status().isNotFound());
	}

	@Test
	void coachNaoLeHistoricoDeAtletaDeOutroCoach() throws Exception {
		String coachA = registerCoach(mockMvc, uniqueEmail("coach-a"));
		String coachB = registerCoach(mockMvc, uniqueEmail("coach-b"));
		String athleteOfA = registerAthlete(mockMvc, coachA);

		// Coach B tenta ler o histórico de um atleta do coach A -> 404.
		mockMvc.perform(get("/api/v1/assessments").param("athleteId", athleteOfA)
						.header("Authorization", bearer(coachB)))
				.andExpect(status().isNotFound());
	}

	@Test
	void coachIdForjadoNoPayloadNaoConcedeAcessoAAtletaDeOutroCoach() throws Exception {
		String coachA = registerCoach(mockMvc, uniqueEmail("coach-a"));
		String coachB = registerCoach(mockMvc, uniqueEmail("coach-b"));
		String athleteOfA = registerAthlete(mockMvc, coachA);

		// Coach B forja o coachId do coach A no body, mas o coachId é ignorado
		// (resolvido do JWT = coach B) e o atleta não pertence a B -> 404.
		String forged = validRequestJson(athleteOfA, "2026-09-01")
				.replace("{", "{\"coachId\": \"%s\",".formatted(UUID.randomUUID().toString()));
		mockMvc.perform(post("/api/v1/assessments")
						.header("Authorization", bearer(coachB))
						.contentType(MediaType.APPLICATION_JSON)
						.content(forged))
				.andExpect(status().isNotFound());
	}

	// ---- T-058 item 3 (revisão): anti-IDOR em endpoints por {id} ----

	@Test
	void coachNaoLeAvaliacaoDeOutroCoachPorId() throws Exception {
		String coachA = registerCoach(mockMvc, uniqueEmail("coach-a"));
		String coachB = registerCoach(mockMvc, uniqueEmail("coach-b"));
		String athleteOfA = registerAthlete(mockMvc, coachA);

		// Coach A cria uma avaliação para o próprio atleta e captura o id.
		MvcResult created = mockMvc.perform(post("/api/v1/assessments")
						.header("Authorization", bearer(coachA))
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRequestJson(athleteOfA, "2026-09-01")))
				.andExpect(status().isCreated())
				.andReturn();
		String assessmentId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		// Coach B tenta ler a avaliação do atleta do coach A pelo id -> 404
		// (mesmo status de "não existe": não vaza a existência do recurso).
		mockMvc.perform(get("/api/v1/assessments/{id}", assessmentId)
						.header("Authorization", bearer(coachB)))
				.andExpect(status().isNotFound());
	}
}
