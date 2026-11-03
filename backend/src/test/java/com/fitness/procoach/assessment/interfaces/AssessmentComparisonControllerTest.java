package com.fitness.procoach.assessment.interfaces;

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
import com.fitness.procoach.security.application.AuthService;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** GET /api/v1/assessments/compare (T-026): valores do núcleo, autorização e erros. */
@SpringBootTest(classes = ProcoachApplication.class)
@AutoConfigureMockMvc
@Import(PostgresTestcontainersConfiguration.class)
class AssessmentComparisonControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AuthService authService;

	private String register(String coach, String athleteId, String date, String weight, String bodyFat)
			throws Exception {
		String body = """
				{"athleteId": "%s", "assessmentDate": "%s", "weightKg": %s, "heightCm": 178, "bodyFatPercentage": %s}
				""".formatted(athleteId, date, weight, bodyFat);
		String json = mockMvc.perform(post("/api/v1/assessments").header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(json, "$.id");
	}

	private String athleteToken(String athleteId) {
		return authService.registerAthleteCredential(uniqueEmail("atleta"), "senha-Forte-123",
				UUID.fromString(athleteId)).tokens().accessToken();
	}

	@Test
	void comparaDuasAvaliacoesComVariacaoDoNucleo() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String first = register(coach, athleteId, "2026-07-01", "90.00", "20.00");
		String second = register(coach, athleteId, "2026-08-26", "84.60", "16.00");

		mockMvc.perform(get("/api/v1/assessments/compare").param("athleteId", athleteId)
						.param("from", first).param("to", second).header("Authorization", bearer(coach)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.from.id").value(first))
				.andExpect(jsonPath("$.to.id").value(second))
				.andExpect(jsonPath("$.daysBetween").value(56))
				.andExpect(jsonPath("$.weightKg.change").value(-5.40))
				.andExpect(jsonPath("$.weightKg.changePercent").value(-6.00))
				.andExpect(jsonPath("$.bodyFatPercentage.change").value(-4.00))
				.andExpect(jsonPath("$.imc.change").isNotEmpty())
				.andExpect(jsonPath("$.tmbKcal.change").isNotEmpty());
	}

	@Test
	void rejeitaCompararUmaAvaliacaoComElaMesma() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String only = register(coach, athleteId, "2026-07-01", "90", "20");

		mockMvc.perform(get("/api/v1/assessments/compare").param("athleteId", athleteId)
						.param("from", only).param("to", only).header("Authorization", bearer(coach)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void avaliacaoDeOutroAtletaDevolve404() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteA = registerAthlete(mockMvc, coach);
		String athleteB = registerAthlete(mockMvc, coach);
		String ofA = register(coach, athleteA, "2026-07-01", "90", "20");
		String ofB = register(coach, athleteB, "2026-08-01", "80", "15");

		mockMvc.perform(get("/api/v1/assessments/compare").param("athleteId", athleteA)
						.param("from", ofA).param("to", ofB).header("Authorization", bearer(coach)))
				.andExpect(status().isNotFound());
	}

	@Test
	void coachNaoCompararAtletaDeOutroCoach() throws Exception {
		String coachA = registerCoach(mockMvc, uniqueEmail("coach"));
		String coachB = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coachA);
		String first = register(coachA, athleteId, "2026-07-01", "90", "20");
		String second = register(coachA, athleteId, "2026-08-01", "88", "18");

		mockMvc.perform(get("/api/v1/assessments/compare").param("athleteId", athleteId)
						.param("from", first).param("to", second).header("Authorization", bearer(coachB)))
				.andExpect(status().isNotFound());
	}

	@Test
	void atletaComparaAsProprias() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String first = register(coach, athleteId, "2026-07-01", "90", "20");
		String second = register(coach, athleteId, "2026-08-01", "88", "18");

		mockMvc.perform(get("/api/v1/assessments/compare").param("athleteId", athleteId)
						.param("from", first).param("to", second)
						.header("Authorization", bearer(athleteToken(athleteId))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.weightKg.change").value(-2.00));
	}

	@Test
	void atletaNaoCompararAvaliacoesDeOutroAtleta() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteA = registerAthlete(mockMvc, coach);
		String athleteB = registerAthlete(mockMvc, coach);
		String first = register(coach, athleteB, "2026-07-01", "90", "20");
		String second = register(coach, athleteB, "2026-08-01", "88", "18");

		mockMvc.perform(get("/api/v1/assessments/compare").param("athleteId", athleteB)
						.param("from", first).param("to", second)
						.header("Authorization", bearer(athleteToken(athleteA))))
				.andExpect(status().is4xxClientError());
	}
}
