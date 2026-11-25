package com.fitness.procoach.security;

import static com.fitness.procoach.CoachAthleteTestSupport.bearer;
import static com.fitness.procoach.CoachAthleteTestSupport.registerAthlete;
import static com.fitness.procoach.CoachAthleteTestSupport.registerCoach;
import static com.fitness.procoach.CoachAthleteTestSupport.uniqueEmail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitness.procoach.ProcoachApplication;
import com.fitness.procoach.TestcontainersConfiguration;
import com.fitness.procoach.security.infrastructure.AthleteInviteJpaEntity;
import com.fitness.procoach.security.infrastructure.AthleteInviteJpaRepository;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Aceite independente da T-064 (issue #79), derivado dos critérios e das regras
 * inegociáveis — não do código de produção:
 *
 * <ul>
 * <li>link de uso único com validade de 7 dias e apenas o hash do token
 * persistido;</li>
 * <li>reenvio gera novo link e invalida o anterior;</li>
 * <li>auditoria append-only: {@code ATHLETE_INVITED} permanece e
 * {@code ATHLETE_INVITE_RESENT} é acrescentado;</li>
 * <li>{@code coachId} do cliente é ignorado (vem do JWT);</li>
 * <li>fallback: falha de SMTP não invalida o convite e devolve
 * {@code emailSent=false} com token/link.</li>
 * </ul>
 */
@SpringBootTest(classes = ProcoachApplication.class,
		properties = { "procoach.security.invite.email.enabled=true",
				"procoach.security.invite.base-url=https://app.procoach.test" })
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, AthleteInviteEmailAcceptanceTest.MailConfig.class })
class AthleteInviteEmailAcceptanceTest {

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

	@Autowired
	private AthleteInviteJpaRepository invites;

	@BeforeEach
	void resetMailSender() {
		reset(mailSender);
	}

	private MvcResult createInvite(String coachToken, String athleteId, String email) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/invites")
						.header("Authorization", bearer(coachToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"athleteId\": \"%s\", \"email\": \"%s\" }".formatted(athleteId, email)))
				.andExpect(status().isCreated())
				.andReturn();
	}

	private static String sha256Hex(String value) throws Exception {
		return HexFormat.of().formatHex(
				MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	void convitePersisteApenasOHashDoTokenEExpiraEmSeteDias() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		Instant before = Instant.now();

		MvcResult result = createInvite(coach, athleteId, uniqueEmail("atleta"));
		Instant after = Instant.now();

		String body = result.getResponse().getContentAsString();
		String token = JsonPath.read(body, "$.token");
		String expiresAtRaw = JsonPath.read(body, "$.expiresAt");
		Instant expiresAt = Instant.parse(expiresAtRaw);

		// Validade de 7 dias (critério 3).
		assertThat(expiresAt).isAfter(before.plus(Duration.ofDays(7)).minusSeconds(2));
		assertThat(expiresAt).isBefore(after.plus(Duration.ofDays(7)).plusSeconds(2));

		// Apenas o hash do token é persistido (critério 3): a linha guarda
		// SHA-256(token), nunca o token em claro.
		List<AthleteInviteJpaEntity> rows = invites.findByAthleteId(UUID.fromString(athleteId));
		assertThat(rows).hasSize(1);
		AthleteInviteJpaEntity row = rows.get(0);
		assertThat(row.getTokenHash()).isNotEqualTo(token);
		assertThat(row.getTokenHash()).isEqualTo(sha256Hex(token));
		assertThat(row.getTokenHash()).hasSize(64);
		// Postgres guarda microssegundos; a resposta serializa nanossegundos. A
		// diferença de precisão não pode passar de 1 ms.
		assertThat(Duration.between(row.getExpiresAt(), expiresAt).abs())
				.isLessThan(Duration.ofMillis(1));

		// O token em claro continua funcionando para consultar o convite (busca por hash).
		mockMvc.perform(get("/api/v1/auth/invites/{token}", token)).andExpect(status().isOk());
	}

	@Test
	void reenvioInvalidaOLinkAnteriorEConservaOAnteriorNoHistoricoAppendOnly() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);

		String first = JsonPath.read(createInvite(coach, athleteId, uniqueEmail("atleta"))
				.getResponse().getContentAsString(), "$.token");

		MvcResult resent = mockMvc.perform(post("/api/v1/athletes/{athleteId}/invites/resend", athleteId)
						.header("Authorization", bearer(coach)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.emailSent").value(true))
				.andReturn();
		String second = JsonPath.read(resent.getResponse().getContentAsString(), "$.token");

		// Novo link é diferente e o anterior deixa de ser utilizável (critério 2).
		assertThat(second).isNotEqualTo(first);
		mockMvc.perform(get("/api/v1/auth/invites/{token}", first)).andExpect(status().isNotFound());
		mockMvc.perform(get("/api/v1/auth/invites/{token}", second)).andExpect(status().isOk());

		// Append-only: o convite anterior não é sobrescrito/apagado; ele aparece
		// como REVOKED ao lado do novo PENDING.
		mockMvc.perform(get("/api/v1/athletes/{athleteId}/invites", athleteId)
						.header("Authorization", bearer(coach)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[?(@.status == 'REVOKED')]").isNotEmpty())
				.andExpect(jsonPath("$[?(@.status == 'PENDING')]").isNotEmpty());

		// Auditoria append-only (critério 5): o evento de criação continua no
		// histórico e o reenvio acrescenta ATHLETE_INVITE_RESENT.
		mockMvc.perform(get("/api/v1/athletes/{athleteId}/history", athleteId)
						.header("Authorization", bearer(coach)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.eventType == 'ATHLETE_INVITED')]").isNotEmpty())
				.andExpect(jsonPath("$[?(@.eventType == 'ATHLETE_INVITE_RESENT')]").isNotEmpty());
	}

	@Test
	void coachIdEnviadoPeloClienteEhIgnoradoEmFavorDoJwt() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		UUID forgedCoachId = UUID.randomUUID();

		mockMvc.perform(post("/api/v1/auth/invites")
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"athleteId\": \"%s\", \"email\": \"%s\", \"coachId\": \"%s\" }"
								.formatted(athleteId, uniqueEmail("atleta"), forgedCoachId)))
				.andExpect(status().isCreated());

		List<AthleteInviteJpaEntity> rows = invites.findByAthleteId(UUID.fromString(athleteId));
		assertThat(rows).hasSize(1);
		assertThat(rows.get(0).getCoachId()).isNotNull().isNotEqualTo(forgedCoachId);
	}

	@Test
	void reenvioDeAtletaDeOutroCoachOuInexistenteRetorna404() throws Exception {
		String coachA = registerCoach(mockMvc, uniqueEmail("coach-a"));
		String coachB = registerCoach(mockMvc, uniqueEmail("coach-b"));
		String athleteOfA = registerAthlete(mockMvc, coachA);
		createInvite(coachA, athleteOfA, uniqueEmail("atleta"));

		// Anti-IDOR (critério 4): atleta de outra carteira → 404 genérico.
		mockMvc.perform(post("/api/v1/athletes/{athleteId}/invites/resend", athleteOfA)
						.header("Authorization", bearer(coachB)))
				.andExpect(status().isNotFound());

		// Atleta inexistente → 404 (não 500/409).
		mockMvc.perform(post("/api/v1/athletes/{athleteId}/invites/resend", UUID.randomUUID())
						.header("Authorization", bearer(coachA)))
				.andExpect(status().isNotFound());

		// Sem autenticação → 401.
		mockMvc.perform(post("/api/v1/athletes/{athleteId}/invites/resend", athleteOfA))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void falhaDeSmtpMantemOConviteValidoEDevolveOLinkComEmailSentFalse() throws Exception {
		doThrow(new MailSendException("SMTP indisponível"))
				.when(mailSender).send(any(SimpleMailMessage.class));

		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);

		MvcResult result = createInvite(coach, athleteId, uniqueEmail("atleta"));
		String body = result.getResponse().getContentAsString();

		// Fallback (critério 6): o convite foi criado, o envio falhou, mas o token
		// e o caminho do link voltam na resposta para o coach repassar.
		assertThat((Boolean) JsonPath.read(body, "$.emailSent")).isFalse();
		String token = JsonPath.read(body, "$.token");
		assertThat(token).isNotBlank();
		assertThat((String) JsonPath.read(body, "$.invitePath")).isEqualTo("/convite/" + token);
		verify(mailSender).send(any(SimpleMailMessage.class));

		// O convite segue válido e aceitável.
		mockMvc.perform(get("/api/v1/auth/invites/{token}", token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").isNotEmpty());
		mockMvc.perform(post("/api/v1/auth/invites/{token}/accept", token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"password\": \"senha-Forte-123\" }"))
				.andExpect(status().isOk());
	}
}
