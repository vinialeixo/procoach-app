package com.fitness.procoach.security;

import static com.fitness.procoach.CoachAthleteTestSupport.bearer;
import static com.fitness.procoach.CoachAthleteTestSupport.registerAthlete;
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
 * Convite do atleta (primeira credencial): o coach gera o link, o atleta consulta
 * e aceita sem autenticação e passa a acessar os próprios dados (T-010).
 */
@SpringBootTest(classes = ProcoachApplication.class)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AthleteInviteControllerTest {

	@Autowired
	private MockMvc mockMvc;

	private String inviteJson(String athleteId, String email) {
		return """
				{ "athleteId": "%s", "email": "%s" }
				""".formatted(athleteId, email);
	}

	private String createInvite(String coachToken, String athleteId, String email) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/invites")
						.header("Authorization", bearer(coachToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(inviteJson(athleteId, email)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(jsonPath("$.invitePath").isNotEmpty())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
	}

	private void accept(String token, String password, int expectedStatus) throws Exception {
		mockMvc.perform(post("/api/v1/auth/invites/{token}/accept", token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"password\": \"%s\" }".formatted(password)))
				.andExpect(status().is(expectedStatus));
	}

	@Test
	void atletaAceitaOConviteEPassaAAcessarOsProprioDados() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String email = uniqueEmail("atleta");
		String token = createInvite(coach, athleteId, email);

		// Consulta pública: mostra o nome do atleta e o e-mail do convite.
		mockMvc.perform(get("/api/v1/auth/invites/{token}", token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.athleteName").value("Ana Souza"))
				.andExpect(jsonPath("$.email").value(email));

		// Aceite público: cria a credencial e abre a sessão do atleta.
		MvcResult accepted = mockMvc.perform(post("/api/v1/auth/invites/{token}/accept", token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"password\": \"senha-Forte-123\" }"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.role").value("ATHLETE"))
				.andExpect(jsonPath("$.athleteId").value(athleteId))
				.andReturn();
		String athleteToken = JsonPath.read(accepted.getResponse().getContentAsString(), "$.accessToken");

		// O token do atleta lê os próprios dados...
		mockMvc.perform(get("/api/v1/assessments").param("athleteId", athleteId)
						.header("Authorization", bearer(athleteToken)))
				.andExpect(status().isOk());

		// ... e o login com e-mail e senha também funciona.
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"email\": \"%s\", \"password\": \"senha-Forte-123\" }".formatted(email)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty());
	}

	@Test
	void conviteSoPodeSerUsadoUmaVez() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String token = createInvite(coach, athleteId, uniqueEmail("atleta"));

		accept(token, "senha-Forte-123", 200);
		accept(token, "outra-senha-456", 404);
		mockMvc.perform(get("/api/v1/auth/invites/{token}", token)).andExpect(status().isNotFound());
	}

	@Test
	void novoConviteRevogaOAnterior() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String first = createInvite(coach, athleteId, uniqueEmail("atleta"));
		String second = createInvite(coach, athleteId, uniqueEmail("atleta"));

		mockMvc.perform(get("/api/v1/auth/invites/{token}", first)).andExpect(status().isNotFound());
		mockMvc.perform(get("/api/v1/auth/invites/{token}", second)).andExpect(status().isOk());
	}

	@Test
	void tokenInexistenteRetorna404() throws Exception {
		mockMvc.perform(get("/api/v1/auth/invites/{token}", "nao-existe")).andExpect(status().isNotFound());
		accept("nao-existe", "senha-Forte-123", 404);
	}

	@Test
	void senhaCurtaEhRecusadaESemConsumirOConvite() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String token = createInvite(coach, athleteId, uniqueEmail("atleta"));

		accept(token, "curta", 400);
		accept(token, "senha-Forte-123", 200);
	}

	@Test
	void coachNaoConvidaAtletaDeOutroCoach() throws Exception {
		String coachA = registerCoach(mockMvc, uniqueEmail("coach-a"));
		String coachB = registerCoach(mockMvc, uniqueEmail("coach-b"));
		String athleteOfA = registerAthlete(mockMvc, coachA);

		mockMvc.perform(post("/api/v1/auth/invites")
						.header("Authorization", bearer(coachB))
						.contentType(MediaType.APPLICATION_JSON)
						.content(inviteJson(athleteOfA, uniqueEmail("atleta"))))
				.andExpect(status().isNotFound());
	}

	@Test
	void criarConviteExigeAutenticacaoDeCoach() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);

		mockMvc.perform(post("/api/v1/auth/invites")
						.contentType(MediaType.APPLICATION_JSON)
						.content(inviteJson(athleteId, uniqueEmail("atleta"))))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void atletaQueJaTemAcessoNaoRecebeNovoConvite() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		accept(createInvite(coach, athleteId, uniqueEmail("atleta")), "senha-Forte-123", 200);

		mockMvc.perform(post("/api/v1/auth/invites")
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content(inviteJson(athleteId, uniqueEmail("atleta"))))
				.andExpect(status().isConflict());
	}
}
