package com.fitness.procoach.athlete;

import static com.fitness.procoach.CoachAthleteTestSupport.bearer;
import static com.fitness.procoach.CoachAthleteTestSupport.registerCoach;
import static com.fitness.procoach.CoachAthleteTestSupport.uniqueEmail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitness.procoach.ProcoachApplication;
import com.fitness.procoach.TestcontainersConfiguration;
import com.fitness.procoach.athlete.domain.AthleteId;
import com.fitness.procoach.athlete.domain.AthleteRepository;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * QA independente da T-096 (E1/E9, US-001/US-038): persistencia e exposicao do
 * e-mail opcional do atleta, compatibilidade sem e-mail, dedupe contra User,
 * RBAC/isolamento multi-tenant e migracao aditiva/anulavel (V808).
 *
 * <p>Testes derivados dos criterios de aceite, nao do codigo existente. Usa JWT
 * real (nunca @WithMockUser) e PostgreSQL real via Testcontainers, para que o
 * Flyway rode as migracoes de verdade.</p>
 */
@SpringBootTest(classes = ProcoachApplication.class)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class T096AthleteEmailAcceptanceTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AthleteRepository repository;

	@Autowired
	private JdbcTemplate jdbc;

	private static final String BASE_BODY = """
			{
			  "name": "Ana Souza",
			  "sex": "FEMALE",
			  "birthDate": "1995-07-20",
			  "height": 165.50,
			  "weight": 60.20,
			  "goal": "HYPERTROPHY"
			}
			""";

	private String bodyWithEmail(String email) {
		return BASE_BODY.replace("{", "{\n\t\t\t  \"email\": \"%s\",".formatted(email));
	}

	private String createAthlete(String coachToken, String body) throws Exception {
		return JsonPath.read(mockMvc.perform(post("/api/v1/athletes")
						.header("Authorization", bearer(coachToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString(), "$.id");
	}

	/** Token real de atleta (ROLE_ATHLETE) via convite + aceite. */
	private String athleteToken(String coachToken, String athleteId) throws Exception {
		String invite = JsonPath.read(mockMvc.perform(post("/api/v1/auth/invites")
						.header("Authorization", bearer(coachToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"athleteId\": \"%s\", \"email\": \"%s\" }"
								.formatted(athleteId, uniqueEmail("atleta-invite"))))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString(), "$.token");

		return JsonPath.read(mockMvc.perform(post("/api/v1/auth/invites/{t}/accept", invite)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"password\": \"senha-Forte-123\" }"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString(), "$.accessToken");
	}

	/** Procura o atleta na listagem e devolve o e-mail exposto (ou null se nao achou). */
	private Object listEmailFor(String coachToken, String athleteId) throws Exception {
		String body = mockMvc.perform(get("/api/v1/athletes").header("Authorization", bearer(coachToken)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		List<String> emails = JsonPath.read(body, "$[?(@.id == '%s')].email".formatted(athleteId));
		assertThat(emails).as("atleta %s deveria aparecer na listagem", athleteId).hasSize(1);
		return emails.get(0);
	}

	// ---- Criterio 1: cadastro com e-mail persiste e expoe ----

	@Test
	void cadastroComEmailRetornaEmailNaCriacaoNaFichaENaListagem() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String email = uniqueEmail("atleta");

		MvcResult created = mockMvc.perform(post("/api/v1/athletes")
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content(bodyWithEmail(email)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value(email))
				.andReturn();
		String athleteId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(get("/api/v1/athletes/{id}", athleteId).header("Authorization", bearer(coach)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(email));

		assertThat(listEmailFor(coach, athleteId)).isEqualTo(email);

		// Persistido de fato (nao apenas ecoado do request).
		assertThat(repository.findById(AthleteId.of(UUID.fromString(athleteId))))
				.get().extracting(a -> a.email()).isEqualTo(email);
	}

	@Test
	void emailPersisteEmMinusculasConformeEnviado() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String email = "QA.MixedCase-" + UUID.randomUUID() + "@Test.COM";

		String athleteId = createAthlete(coach, bodyWithEmail(email));

		mockMvc.perform(get("/api/v1/athletes/{id}", athleteId).header("Authorization", bearer(coach)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(email));
	}

	// ---- Criterio 2: compatibilidade sem e-mail ----

	@Test
	void cadastroSemCampoEmailContinua201ComEmailNulo() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));

		MvcResult created = mockMvc.perform(post("/api/v1/athletes")
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content(BASE_BODY))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value(org.hamcrest.Matchers.nullValue()))
				.andReturn();
		String athleteId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(get("/api/v1/athletes/{id}", athleteId).header("Authorization", bearer(coach)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(org.hamcrest.Matchers.nullValue()));

		assertThat(listEmailFor(coach, athleteId)).isNull();
	}

	@Test
	void cadastroComEmailNuloExplicitoContinua201ComEmailNulo() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String body = BASE_BODY.replace("{", "{\n\t\t\t  \"email\": null,");

		mockMvc.perform(post("/api/v1/athletes")
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value(org.hamcrest.Matchers.nullValue()));
	}

	@Test
	void cadastroComEmailEmBrancoNormalizaParaNulo() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));

		// O dominio declara "blank -> null"; a API nao pode rejeitar como 400 nem
		// persistir string em branco.
		MvcResult created = mockMvc.perform(post("/api/v1/athletes")
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content(bodyWithEmail("   ")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value(org.hamcrest.Matchers.nullValue()))
				.andReturn();
		String athleteId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
		assertThat(repository.findById(AthleteId.of(UUID.fromString(athleteId))))
				.get().extracting(a -> a.email()).isNull();
	}

	@Test
	void cadastroComEmailComEspacosNasPontasNormalizaComTrim() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String raw = uniqueEmail("atleta-padded");

		String athleteId = createAthlete(coach, bodyWithEmail("  " + raw + "  "));

		mockMvc.perform(get("/api/v1/athletes/{id}", athleteId).header("Authorization", bearer(coach)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(raw));
	}

	// ---- Criterio 3: e-mail de User existente continua 409 ----

	@Test
	void emailDeUserExistenteRetorna409SemCriarAtleta() throws Exception {
		String coachEmail = uniqueEmail("coach");
		String coach = registerCoach(mockMvc, coachEmail);

		mockMvc.perform(post("/api/v1/athletes")
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content(bodyWithEmail(coachEmail)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));

		// Nada criado apesar do 409.
		mockMvc.perform(get("/api/v1/athletes").header("Authorization", bearer(coach)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void emailDeUserExistenteEmOutroCaixaRetorna409() throws Exception {
		String coachEmail = uniqueEmail("coach").toLowerCase();
		String coach = registerCoach(mockMvc, coachEmail);

		// E-mails sao case-insensitive por convencao: a variante em maiusculas
		// ainda e o mesmo User e deve ser recusada.
		mockMvc.perform(post("/api/v1/athletes")
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content(bodyWithEmail(coachEmail.toUpperCase())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
	}

	// ---- Regra inegociavel: toda escrita gera AuditLog ----

	@Test
	void cadastroComEmailGeraEventoDeAuditoria() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = createAthlete(coach, bodyWithEmail(uniqueEmail("atleta")));

		mockMvc.perform(get("/api/v1/athletes/{id}/history", athleteId)
						.header("Authorization", bearer(coach)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].eventType")
						.value(org.hamcrest.Matchers.hasItem("ATHLETE_REGISTERED")));
	}

	// ---- Criterio 4: RBAC e isolamento multi-tenant ----

	@Test
	void semTokenNaoLeNemCadastraAtletas() throws Exception {
		mockMvc.perform(post("/api/v1/athletes")
						.contentType(MediaType.APPLICATION_JSON)
						.content(bodyWithEmail(uniqueEmail("atleta"))))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/v1/athletes")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/v1/athletes/{id}", UUID.randomUUID())).andExpect(status().isUnauthorized());
	}

	@Test
	void atletaNaoPodeCadastrarNemListarAtletas() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = createAthlete(coach, bodyWithEmail(uniqueEmail("atleta")));
		String athlete = athleteToken(coach, athleteId);

		mockMvc.perform(post("/api/v1/athletes")
						.header("Authorization", bearer(athlete))
						.contentType(MediaType.APPLICATION_JSON)
						.content(bodyWithEmail(uniqueEmail("invasor"))))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/v1/athletes").header("Authorization", bearer(athlete)))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/v1/athletes/{id}", athleteId).header("Authorization", bearer(athlete)))
				.andExpect(status().isForbidden());
	}

	@Test
	void coachDeOutroTenantNaoVeEmailDoAtleta() throws Exception {
		String coachA = registerCoach(mockMvc, uniqueEmail("coach-a"));
		String coachB = registerCoach(mockMvc, uniqueEmail("coach-b"));
		String email = uniqueEmail("atleta");
		String athleteOfA = createAthlete(coachA, bodyWithEmail(email));

		mockMvc.perform(get("/api/v1/athletes/{id}", athleteOfA).header("Authorization", bearer(coachB)))
				.andExpect(status().isNotFound());

		String listOfB = mockMvc.perform(get("/api/v1/athletes").header("Authorization", bearer(coachB)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		assertThat(listOfB).doesNotContain(athleteOfA).doesNotContain(email);
	}

	@Test
	void coachIdForjadoNoBodyEhIgnoradoMesmoComEmail() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String forged = UUID.randomUUID().toString();

		MvcResult created = mockMvc.perform(post("/api/v1/athletes")
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content(bodyWithEmail(uniqueEmail("atleta"))
								.replace("{", "{\n\t\t\t  \"coachId\": \"%s\",".formatted(forged))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.coachId").value(org.hamcrest.Matchers.not(forged)))
				.andReturn();
		String athleteId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		// O dono real e o coach autenticado; o e-mail continua na ficha dele.
		mockMvc.perform(get("/api/v1/athletes/{id}", athleteId).header("Authorization", bearer(coach)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.coachId").value(org.hamcrest.Matchers.not(forged)));
		assertThat(listEmailFor(coach, athleteId)).isNotNull();
	}

	@Test
	void atletaRemovidoNaoExpoeMaisEmail() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String email = uniqueEmail("atleta");
		String athleteId = createAthlete(coach, bodyWithEmail(email));

		mockMvc.perform(delete("/api/v1/athletes/{id}", athleteId).header("Authorization", bearer(coach)))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/athletes/{id}", athleteId).header("Authorization", bearer(coach)))
				.andExpect(status().isNotFound());

		String list = mockMvc.perform(get("/api/v1/athletes").header("Authorization", bearer(coach)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		assertThat(list).doesNotContain(athleteId).doesNotContain(email);

		// Soft-delete: a linha (com o email) continua no banco, apenas marcada.
		assertThat(repository.findById(AthleteId.of(UUID.fromString(athleteId))))
				.get().extracting(a -> a.isDeleted()).isEqualTo(true);
	}

	// ---- Criterio 5: migracao V808 aditiva e anulavel ----

	@Test
	void migracaoV808AdicionaColunaEmailAnulavelDe255() {
		Map<String, Object> column = jdbc.queryForMap("""
				SELECT is_nullable, data_type, character_maximum_length, column_default
				FROM information_schema.columns
				WHERE table_name = 'athletes' AND column_name = 'email'
				""");

		assertThat(column.get("is_nullable")).isEqualTo("YES");
		assertThat(column.get("data_type")).isEqualTo("character varying");
		assertThat(((Number) column.get("character_maximum_length")).intValue()).isEqualTo(255);
		assertThat(column.get("column_default")).isNull();
	}

	@Test
	void migracaoV808FoiAplicada() {
		Integer applied = jdbc.queryForObject("""
				SELECT count(*) FROM flyway_schema_history WHERE version = '808' AND success = TRUE
				""", Integer.class);
		assertThat(applied).isEqualTo(1);
	}

	@Test
	void linhaLegadaSemEmailContinuaValidaELegivelComEmailNulo() {
		UUID legacyId = UUID.randomUUID();

		// INSERT no formato anterior a V808: sem a coluna email.
		jdbc.update("""
				INSERT INTO athletes (id, coach_id, name, sex, birth_date, height, weight, goal,
				                      status, created_at, updated_at)
				VALUES (?, ?, 'Legado Sem Email', 'MALE', DATE '1990-01-01', 180.00, 80.00,
				        'CUTTING', 'ACTIVE', now(), now())
				""", legacyId, UUID.randomUUID());

		Map<String, Object> row = jdbc.queryForMap("SELECT email FROM athletes WHERE id = ?", legacyId);
		assertThat(row.get("email")).isNull();

		assertThat(repository.findById(AthleteId.of(legacyId)))
				.get().extracting(a -> a.email()).isNull();
	}

	// ---- Borda: e-mail acima do limite da coluna nao pode virar 500 ----

	@Test
	void emailMaiorQueOLimiteDaColunaRetorna400ENao500() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String tooLong = "a".repeat(250) + "@test.com"; // 259 > VARCHAR(255)

		mockMvc.perform(post("/api/v1/athletes")
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content(bodyWithEmail(tooLong)))
				.andExpect(status().isBadRequest());
	}
}
