package com.fitness.procoach.training.interfaces.rest;

import static com.fitness.procoach.CoachAthleteTestSupport.bearer;
import static com.fitness.procoach.CoachAthleteTestSupport.registerAthlete;
import static com.fitness.procoach.CoachAthleteTestSupport.registerCoach;
import static com.fitness.procoach.CoachAthleteTestSupport.uniqueEmail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitness.procoach.ProcoachApplication;
import com.fitness.procoach.athlete.infrastructure.PostgresTestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Testes de autorização do módulo training (T-058 item 2): um coach autenticado
 * NÃO consegue gravar/lê periodização (e derivados) de atleta de outro coach,
 * mesmo forçando coachId no payload. Usa token JWT real.
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
}
