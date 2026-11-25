package com.fitness.procoach.security;

import static com.fitness.procoach.CoachAthleteTestSupport.bearer;
import static com.fitness.procoach.CoachAthleteTestSupport.registerAthlete;
import static com.fitness.procoach.CoachAthleteTestSupport.registerCoach;
import static com.fitness.procoach.CoachAthleteTestSupport.uniqueEmail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitness.procoach.ProcoachApplication;
import com.fitness.procoach.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * T-064: convite do atleta enviado por e-mail automaticamente e reenvio
 * (novo link de uso único de 7 dias, revogando o anterior, com auditoria).
 */
@SpringBootTest(classes = ProcoachApplication.class,
		properties = { "procoach.security.invite.email.enabled=true",
				"procoach.security.invite.base-url=https://app.procoach.test" })
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, AthleteInviteEmailTest.MailConfig.class })
class AthleteInviteEmailTest {

	@TestConfiguration(proxyBeanMethods = false)
	static class MailConfig {

		@Bean
		JavaMailSender javaMailSender() {
			return mock(JavaMailSender.class);
		}
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JavaMailSender mailSender;

	@BeforeEach
	void clearMail() {
		clearInvocations(mailSender);
	}

	private MvcResult createInvite(String coachToken, String athleteId, String email) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/invites")
						.header("Authorization", bearer(coachToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"athleteId\": \"%s\", \"email\": \"%s\" }".formatted(athleteId, email)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.emailSent").value(true))
				.andReturn();
	}

	@Test
	void conviteEhEnviadoPorEmailComLinkDeUsoUnico() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String email = uniqueEmail("atleta");

		MvcResult result = createInvite(coach, athleteId, email);
		String token = JsonPath.read(result.getResponse().getContentAsString(), "$.token");

		ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(mailSender).send(captor.capture());
		SimpleMailMessage message = captor.getValue();
		assertThat(message.getTo()).containsExactly(email);
		assertThat(message.getSubject()).contains("convite");
		assertThat(message.getText()).contains("https://app.procoach.test/convite/" + token);

		// O link enviado por e-mail é o mesmo convite de uso único e validade de 7 dias.
		mockMvc.perform(get("/api/v1/auth/invites/{token}", token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(email));
	}

	@Test
	void reenvioRevogaOLinkAnteriorEEnviaUmNovo() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String first = JsonPath.read(createInvite(coach, athleteId, uniqueEmail("atleta"))
				.getResponse().getContentAsString(), "$.token");

		MvcResult resent = mockMvc.perform(post("/api/v1/athletes/{athleteId}/invites/resend", athleteId)
						.header("Authorization", bearer(coach)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.emailSent").value(true))
				.andExpect(jsonPath("$.invitePath").isNotEmpty())
				.andReturn();
		String second = JsonPath.read(resent.getResponse().getContentAsString(), "$.token");

		assertThat(second).isNotEqualTo(first);
		mockMvc.perform(get("/api/v1/auth/invites/{token}", first)).andExpect(status().isNotFound());
		mockMvc.perform(get("/api/v1/auth/invites/{token}", second)).andExpect(status().isOk());

		ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(mailSender, times(2)).send(captor.capture());
		List<SimpleMailMessage> messages = captor.getAllValues();
		assertThat(messages.get(1).getText()).contains("https://app.procoach.test/convite/" + second);

		// Auditoria append-only do reenvio no histórico do atleta.
		mockMvc.perform(get("/api/v1/athletes/{id}/history", athleteId)
						.header("Authorization", bearer(coach)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.eventType == 'ATHLETE_INVITE_RESENT')]").isNotEmpty());
	}

	@Test
	void reenvioDeAtletaDeOutroCoachRetorna404() throws Exception {
		String coachA = registerCoach(mockMvc, uniqueEmail("coach-a"));
		String coachB = registerCoach(mockMvc, uniqueEmail("coach-b"));
		String athleteOfA = registerAthlete(mockMvc, coachA);
		createInvite(coachA, athleteOfA, uniqueEmail("atleta"));

		mockMvc.perform(post("/api/v1/athletes/{athleteId}/invites/resend", athleteOfA)
						.header("Authorization", bearer(coachB)))
				.andExpect(status().isNotFound());
	}

	@Test
	void reenvioSemConviteAnteriorRetorna409Acionavel() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);

		mockMvc.perform(post("/api/v1/athletes/{athleteId}/invites/resend", athleteId)
						.header("Authorization", bearer(coach)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").isNotEmpty());
	}

	@Test
	void reenvioDeAtletaQueJaAceitouRetorna409() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String token = JsonPath.read(createInvite(coach, athleteId, uniqueEmail("atleta"))
				.getResponse().getContentAsString(), "$.token");
		mockMvc.perform(post("/api/v1/auth/invites/{token}/accept", token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"password\": \"senha-Forte-123\" }"))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/athletes/{athleteId}/invites/resend", athleteId)
						.header("Authorization", bearer(coach)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("ATHLETE_HAS_CREDENTIAL"));
	}
}
