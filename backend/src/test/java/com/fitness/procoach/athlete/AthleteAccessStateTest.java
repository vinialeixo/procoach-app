package com.fitness.procoach.athlete;

import static com.fitness.procoach.CoachAthleteTestSupport.bearer;
import static com.fitness.procoach.CoachAthleteTestSupport.registerCoach;
import static com.fitness.procoach.CoachAthleteTestSupport.uniqueEmail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitness.procoach.ProcoachApplication;
import com.fitness.procoach.TestcontainersConfiguration;
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
 * T-077/T-078 (US-20/US-01): estado de acesso do atleta nas respostas de
 * listagem/ficha ({@code hasCredential}, {@code pendingInvite}) e e-mail
 * opcional no cadastro com dedupe acionável (409 EMAIL_ALREADY_REGISTERED).
 */
@SpringBootTest(classes = ProcoachApplication.class)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AthleteAccessStateTest {

	@Autowired
	private MockMvc mockMvc;

	private String registerAthleteWithEmail(String coachToken, String email) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/athletes")
						.header("Authorization", bearer(coachToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Ana Souza",
								  "sex": "FEMALE",
								  "birthDate": "1995-07-20",
								  "height": 165.50,
								  "weight": 60.20,
								  "goal": "HYPERTROPHY",
								  "email": "%s"
								}
								""".formatted(email)))
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private String createInvite(String coachToken, String athleteId, String email) throws Exception {
		return JsonPath.read(mockMvc
				.perform(post("/api/v1/auth/invites")
						.header("Authorization", bearer(coachToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"athleteId\": \"%s\", \"email\": \"%s\" }".formatted(athleteId, email)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString(), "$.token");
	}

	private void accept(String token, String password) throws Exception {
		mockMvc.perform(post("/api/v1/auth/invites/{token}/accept", token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"password\": \"%s\" }".formatted(password)))
				.andExpect(status().isOk());
	}

	@Test
	void listEFichaTrazemEstadoDeAcessoSemConvite() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthleteWithEmail(coach, uniqueEmail("atleta"));

		mockMvc.perform(get("/api/v1/athletes").header("Authorization", bearer(coach)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].hasCredential").value(false))
				.andExpect(jsonPath("$[0].pendingInvite").value(org.hamcrest.Matchers.nullValue()));

		mockMvc.perform(get("/api/v1/athletes/{id}", athleteId).header("Authorization", bearer(coach)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.hasCredential").value(false))
				.andExpect(jsonPath("$.pendingInvite").value(org.hamcrest.Matchers.nullValue()));
	}

	@Test
	void convitePendenteApareceNaFichaESaiDepoisDoAceite() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String email = uniqueEmail("atleta");
		String athleteId = registerAthleteWithEmail(coach, email);
		createInvite(coach, athleteId, email);

		// Convite pendente com e-mail e expiração, sem token exposto.
		mockMvc.perform(get("/api/v1/athletes/{id}", athleteId).header("Authorization", bearer(coach)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.hasCredential").value(false))
				.andExpect(jsonPath("$.pendingInvite.id").isString())
				.andExpect(jsonPath("$.pendingInvite.email").value(email))
				.andExpect(jsonPath("$.pendingInvite.expiresAt").isNotEmpty());

		accept(createInvite(coach, athleteId, uniqueEmail("atleta2")), "senha-Forte-123");

		// Aceite cria a credencial e consome o convite.
		mockMvc.perform(get("/api/v1/athletes/{id}", athleteId).header("Authorization", bearer(coach)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.hasCredential").value(true))
				.andExpect(jsonPath("$.pendingInvite").value(org.hamcrest.Matchers.nullValue()));
	}

	@Test
	void cadastroComEmailDeUsuarioExistenteRetorna409EmailAlreadyRegistered() throws Exception {
		String coachEmail = uniqueEmail("coach");
		String coach = registerCoach(mockMvc, coachEmail);

		// E-mail do próprio coach (já é User) — 409 acionável.
		mockMvc.perform(post("/api/v1/athletes")
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Ana Souza",
								  "sex": "FEMALE",
								  "birthDate": "1995-07-20",
								  "height": 165.50,
								  "weight": 60.20,
								  "goal": "HYPERTROPHY",
								  "email": "%s"
								}
								""".formatted(coachEmail)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"))
				.andExpect(jsonPath("$.error").isNotEmpty());

		// Sem e-mail continua funcionando (compatibilidade).
		String athleteId = com.fitness.procoach.CoachAthleteTestSupport.registerAthlete(mockMvc, coach);
		mockMvc.perform(get("/api/v1/athletes/{id}", athleteId).header("Authorization", bearer(coach)))
				.andExpect(status().isOk());

		// E-mail livre é aceito.
		registerAthleteWithEmail(coach, uniqueEmail("atleta-livre"));
		mockMvc.perform(get("/api/v1/athletes").header("Authorization", bearer(coach)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	void cadastroComEmailDeAtletaComAcessoTambemRetorna409() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String email = uniqueEmail("atleta");
		String athleteId = registerAthleteWithEmail(coach, email);
		accept(createInvite(coach, athleteId, email), "senha-Forte-123");

		mockMvc.perform(post("/api/v1/athletes")
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Outro Atleta",
								  "sex": "MALE",
								  "birthDate": "1990-01-01",
								  "height": 180.00,
								  "weight": 80.00,
								  "goal": "CUTTING",
								  "email": "%s"
								}
								""".formatted(email)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
	}
}
