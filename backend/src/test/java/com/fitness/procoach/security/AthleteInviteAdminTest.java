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
 * T-077/T-079: listagem e revogação de convites do atleta (anti-IDOR, auditoria
 * append-only) e 409 acionáveis distintos na criação de convite
 * (ATHLETE_HAS_CREDENTIAL vs EMAIL_IN_USE).
 */
@SpringBootTest(classes = ProcoachApplication.class)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AthleteInviteAdminTest {

	@Autowired
	private MockMvc mockMvc;

	private String createInvite(String coachToken, String athleteId, String email) throws Exception {
		return JsonPath.read(mockMvc
				.perform(post("/api/v1/auth/invites")
						.header("Authorization", bearer(coachToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"athleteId\": \"%s\", \"email\": \"%s\" }".formatted(athleteId, email)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString(), "$.token");
	}

	private String accept(String token, String password) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/invites/{token}/accept", token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"password\": \"%s\" }".formatted(password)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
	}

	@Test
	void listagemDeConvitesNaoExpoeTokenEMostraStatus() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String email = uniqueEmail("atleta");
		String token = createInvite(coach, athleteId, email);

		MvcResult result = mockMvc.perform(get("/api/v1/athletes/{athleteId}/invites", athleteId)
						.header("Authorization", bearer(coach)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").isString())
				.andExpect(jsonPath("$[0].athleteId").value(athleteId))
				.andExpect(jsonPath("$[0].email").value(email))
				.andExpect(jsonPath("$[0].expiresAt").isNotEmpty())
				.andExpect(jsonPath("$[0].createdAt").isNotEmpty())
				.andExpect(jsonPath("$[0].status").value("PENDING"))
				.andReturn();

		String body = result.getResponse().getContentAsString();
		org.assertj.core.api.Assertions.assertThat(body).doesNotContain("token");

		// Aceite do convite muda o status para ACCEPTED na listagem.
		accept(token, "senha-Forte-123");
		mockMvc.perform(get("/api/v1/athletes/{athleteId}/invites", athleteId)
						.header("Authorization", bearer(coach)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].status").value("ACCEPTED"));
	}

	@Test
	void revogaConvitePendenteERegistraAuditoria() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		createInvite(coach, athleteId, uniqueEmail("atleta"));

		String inviteId = JsonPath.read(mockMvc
				.perform(get("/api/v1/athletes/{athleteId}/invites", athleteId)
						.header("Authorization", bearer(coach)))
				.andReturn().getResponse().getContentAsString(), "$[0].id");

		mockMvc.perform(post("/api/v1/athletes/{athleteId}/invites/{inviteId}/revoke", athleteId, inviteId)
						.header("Authorization", bearer(coach)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(inviteId))
				.andExpect(jsonPath("$.status").value("REVOKED"));

		// Listagem reflete a revogação...
		mockMvc.perform(get("/api/v1/athletes/{athleteId}/invites", athleteId)
						.header("Authorization", bearer(coach)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].status").value("REVOKED"));

		// ... e a revogação não derruba o convite: 409 ao tentar de novo.
		mockMvc.perform(post("/api/v1/athletes/{athleteId}/invites/{inviteId}/revoke", athleteId, inviteId)
						.header("Authorization", bearer(coach)))
				.andExpect(status().isConflict());

		// Auditoria append-only: ATHLETE_INVITE_REVOKED no histórico do atleta.
		mockMvc.perform(get("/api/v1/athletes/{id}/history", athleteId)
						.header("Authorization", bearer(coach)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.eventType == 'ATHLETE_INVITE_REVOKED')]").isNotEmpty());
	}

	@Test
	void coachDeOutroCarteiraRecebe404NaListagemERevogacao() throws Exception {
		String coachA = registerCoach(mockMvc, uniqueEmail("coach-a"));
		String coachB = registerCoach(mockMvc, uniqueEmail("coach-b"));
		String athleteOfA = registerAthlete(mockMvc, coachA);
		createInvite(coachA, athleteOfA, uniqueEmail("atleta"));

		String inviteId = JsonPath.read(mockMvc
				.perform(get("/api/v1/athletes/{athleteId}/invites", athleteOfA)
						.header("Authorization", bearer(coachA)))
				.andReturn().getResponse().getContentAsString(), "$[0].id");

		// Anti-IDOR: 404 genérico, não vaza existência do recurso.
		mockMvc.perform(get("/api/v1/athletes/{athleteId}/invites", athleteOfA)
						.header("Authorization", bearer(coachB)))
				.andExpect(status().isNotFound());
		mockMvc.perform(post("/api/v1/athletes/{athleteId}/invites/{inviteId}/revoke", athleteOfA, inviteId)
						.header("Authorization", bearer(coachB)))
				.andExpect(status().isNotFound());
	}

	@Test
	void conviteAceitoRetorna409AthleteHasCredential() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		accept(createInvite(coach, athleteId, uniqueEmail("atleta")), "senha-Forte-123");

		mockMvc.perform(post("/api/v1/auth/invites")
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"athleteId\": \"%s\", \"email\": \"%s\" }"
								.formatted(athleteId, uniqueEmail("outro"))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("ATHLETE_HAS_CREDENTIAL"))
				.andExpect(jsonPath("$.message").isNotEmpty());
	}

	@Test
	void conviteComEmailDeOutroUsuarioRetorna409EmailInUse() throws Exception {
		String coachA = registerCoach(mockMvc, uniqueEmail("coach-a"));
		String coachBEmail = uniqueEmail("coach-b");
		String coachB = registerCoach(mockMvc, coachBEmail);
		String athleteId = registerAthlete(mockMvc, coachA);

		// E-mail do coach B (User existente de outro usuário) — EMAIL_IN_USE.
		mockMvc.perform(post("/api/v1/auth/invites")
						.header("Authorization", bearer(coachA))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"athleteId\": \"%s\", \"email\": \"%s\" }".formatted(athleteId, coachBEmail)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("EMAIL_IN_USE"))
				.andExpect(jsonPath("$.message").isNotEmpty());
	}
}
