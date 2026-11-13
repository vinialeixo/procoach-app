package com.fitness.procoach.security;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fitness.procoach.ProcoachApplication;
import com.fitness.procoach.TestcontainersConfiguration;
import com.fitness.procoach.security.application.AuthService;
import com.fitness.procoach.security.application.TotpService;
import com.jayway.jsonpath.JsonPath;

/**
 * Testes de integração HTTP do módulo de segurança (US-20, T-008/T-009/T-010):
 * bloqueio 401 sem token, bloqueio 403 de atleta acessando dados de outro
 * atleta, fluxo completo de login em duas etapas com MFA (TOTP) e refresh.
 */
@SpringBootTest(classes = ProcoachApplication.class)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AuthService authService;

	@Autowired
	private TotpService totpService;

	private static String uniqueEmail(String prefix) {
		return prefix + "-" + UUID.randomUUID() + "@test.com";
	}

	private static String bearer(String token) {
		return "Bearer " + token;
	}

	private String registerCoachViaHttp(String email) throws Exception {
		MvcResult result = mockMvc
				.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Coach Teste", "email": "%s", "password": "senha123!"}
								""".formatted(email)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.userId").isString())
				.andExpect(jsonPath("$.accessToken").isString())
				.andExpect(jsonPath("$.refreshToken").isString())
				.andExpect(jsonPath("$.role").value("COACH"))
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
	}

	@Test
	void blocksProtectedRouteWithoutToken() throws Exception {
		mockMvc.perform(get("/api/v1/assessments").param("athleteId", UUID.randomUUID().toString()))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/v1/athletes").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void registerCreatesCoachAndTokenGrantsAccess() throws Exception {
		String coachToken = registerCoachViaHttp(uniqueEmail("coach"));

		// O coach autenticado consegue ler a própria carteira (atleta criado por ele).
		String athleteId = registerAthleteViaHttp(coachToken);
		mockMvc.perform(get("/api/v1/assessments")
				.param("athleteId", athleteId)
				.header("Authorization", bearer(coachToken)))
				.andExpect(status().isOk());
	}

	private String registerAthleteViaHttp(String coachToken) throws Exception {
		MvcResult result = mockMvc
				.perform(post("/api/v1/athletes").header("Authorization", bearer(coachToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Ana Souza", "sex": "FEMALE", "birthDate": "1995-07-20",
								 "height": 165.50, "weight": 60.20, "goal": "HYPERTROPHY"}
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	@Test
	void loginWithoutMfaReturnsTokensAndWrongPasswordReturns401() throws Exception {
		String email = uniqueEmail("login");
		registerCoachViaHttp(email);

		mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email": "%s", "password": "errada"}
						""".formatted(email)))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email": "%s", "password": "senha123!"}
						""".formatted(email)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.mfaRequired").value(false))
				.andExpect(jsonPath("$.accessToken").isString())
				.andExpect(jsonPath("$.refreshToken").isString())
				.andExpect(jsonPath("$.role").value("COACH"));
	}

	@Test
	void mfaSetupRequiresAuthentication() throws Exception {
		mockMvc.perform(post("/api/v1/auth/mfa/setup")).andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/v1/auth/mfa/setup/confirm").contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\": \"123456\"}")).andExpect(status().isUnauthorized());
	}

	@Test
	void twoStepLoginWithMfaOverHttp() throws Exception {
		String email = uniqueEmail("mfa");
		String coachToken = registerCoachViaHttp(email);

		// Ativa o MFA: setup gera o segredo, confirm valida o primeiro código.
		MvcResult setup = mockMvc
				.perform(post("/api/v1/auth/mfa/setup").header("Authorization", bearer(coachToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.secret").isString())
				.andExpect(jsonPath("$.otpauthUrl").isString())
				.andReturn();
		String secret = JsonPath.read(setup.getResponse().getContentAsString(), "$.secret");

		mockMvc.perform(post("/api/v1/auth/mfa/setup/confirm")
				.header("Authorization", bearer(coachToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\": \"%s\"}".formatted(totpService.currentCode(secret))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.confirmed").value(true));

		// Login passa a exigir a segunda etapa: sem tokens, só o desafio.
		MvcResult login = mockMvc
				.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email": "%s", "password": "senha123!"}
								""".formatted(email)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.mfaRequired").value(true))
				.andExpect(jsonPath("$.challengeId").isString())
				.andExpect(jsonPath("$.accessToken").doesNotExist())
				.andReturn();
		String challengeId = JsonPath.read(login.getResponse().getContentAsString(), "$.challengeId");

		// O token de desafio NÃO é uma sessão: o resource server só aceita token de
		// acesso (type=access), então ele é recusado como Bearer (401).
		mockMvc.perform(get("/api/v1/assessments")
				.param("athleteId", UUID.randomUUID().toString())
				.header("Authorization", bearer(challengeId)))
				.andExpect(status().isUnauthorized());

		// Código errado -> 400; código certo -> sessão completa.
		mockMvc.perform(post("/api/v1/auth/mfa/verify").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"challengeId": "%s", "code": "000000"}
						""".formatted(challengeId)))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post("/api/v1/auth/mfa/verify").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"challengeId": "%s", "code": "%s"}
						""".formatted(challengeId, totpService.currentCode(secret))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isString())
				.andExpect(jsonPath("$.refreshToken").isString())
				.andExpect(jsonPath("$.role").value("COACH"));
	}

	/**
	 * Disparo simultâneo de /mfa/setup (duplo clique, retry de rede, efeito duplo do
	 * front) é uma corrida real de Optimistic Locking na linha do usuário: o perdedor
	 * recarrega a conta e repete dentro do AuthService. Sem o retry, uma das
	 * chamadas responderia 500 com ObjectOptimisticLockingFailureException.
	 */
	@Test
	void concurrentMfaSetupCallsNeverFailWithServerError() throws Exception {
		String coachToken = registerCoachViaHttp(uniqueEmail("mfa-concorrente"));
		int clients = 4;
		ExecutorService executor = Executors.newFixedThreadPool(clients);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<Integer>> statuses = new ArrayList<>();
		try {
			for (int i = 0; i < clients; i++) {
				statuses.add(executor.submit(() -> {
					start.await();
					return mockMvc.perform(post("/api/v1/auth/mfa/setup")
							.header("Authorization", bearer(coachToken)))
							.andReturn().getResponse().getStatus();
				}));
			}
			start.countDown();
			for (Future<Integer> future : statuses) {
				int status = future.get(30, TimeUnit.SECONDS);
				assertTrue(status == HttpStatus.OK.value() || status == HttpStatus.CONFLICT.value(),
						"status inesperado: " + status);
			}
		} catch (ExecutionException ex) {
			fail("requisição concorrente falhou: " + ex.getCause());
		} finally {
			executor.shutdownNow();
		}
	}

	@Test
	void refreshRotatesTokensAndRejectsAccessTokenAsRefresh() throws Exception {
		String email = uniqueEmail("refresh");
		registerCoachViaHttp(email);

		MvcResult login = mockMvc
				.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email": "%s", "password": "senha123!"}
								""".formatted(email)))
				.andExpect(status().isOk())
				.andReturn();
		String refreshToken = JsonPath.read(login.getResponse().getContentAsString(), "$.refreshToken");
		String accessToken = JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");

		mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"refreshToken": "%s"}
						""".formatted(refreshToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isString())
				.andExpect(jsonPath("$.refreshToken").isString());

		mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"refreshToken": "%s"}
						""".formatted(accessToken)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void athleteRoleCannotWriteCoachEndpoints() throws Exception {
		UUID athleteId = UUID.randomUUID();
		String athleteToken = authService
				.registerAthleteCredential(uniqueEmail("athlete"), "senha123!", athleteId)
				.tokens()
				.accessToken();

		mockMvc.perform(post("/api/v1/athletes")
				.header("Authorization", bearer(athleteToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name": "Atleta X", "sex": "MALE", "birthDate": "2000-01-01",
						 "height": 175.0, "weight": 70.0, "goal": "MAINTENANCE"}
						"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void athleteCanOnlyAccessOwnData() throws Exception {
		UUID athleteA = UUID.randomUUID();
		UUID athleteB = UUID.randomUUID();
		String tokenA = authService.registerAthleteCredential(uniqueEmail("athlete-a"), "senha123!", athleteA)
				.tokens()
				.accessToken();
		String coachToken = registerCoachViaHttp(uniqueEmail("coach"));

		// Atleta A tentando ler dados do atleta B -> 403 (T-010/US-038).
		mockMvc.perform(get("/api/v1/assessments")
				.param("athleteId", athleteB.toString())
				.header("Authorization", bearer(tokenA)))
				.andExpect(status().isForbidden());

		// Atleta A lendo os próprios dados -> 200.
		mockMvc.perform(get("/api/v1/assessments")
				.param("athleteId", athleteA.toString())
				.header("Authorization", bearer(tokenA)))
				.andExpect(status().isOk());

		// T-058: coach só lê atletas da própria carteira. athleteB não pertence ao
		// coach autenticado (nunca foi criado na carteira dele) -> 403 (guard valida posse).
		mockMvc.perform(get("/api/v1/assessments")
				.param("athleteId", athleteB.toString())
				.header("Authorization", bearer(coachToken)))
				.andExpect(status().isForbidden());
	}
}
