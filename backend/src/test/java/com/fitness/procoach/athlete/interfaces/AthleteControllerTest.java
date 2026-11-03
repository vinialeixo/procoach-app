package com.fitness.procoach.athlete.interfaces;

import static com.fitness.procoach.CoachAthleteTestSupport.bearer;
import static com.fitness.procoach.CoachAthleteTestSupport.registerAthlete;
import static com.fitness.procoach.CoachAthleteTestSupport.registerCoach;
import static com.fitness.procoach.CoachAthleteTestSupport.uniqueEmail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitness.procoach.ProcoachApplication;
import com.fitness.procoach.athlete.domain.AthleteId;
import com.fitness.procoach.athlete.domain.AthleteRepository;
import com.fitness.procoach.athlete.infrastructure.PostgresTestcontainersConfiguration;
import java.time.LocalDate;
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
 * Testes de integração do endpoint de cadastro/remoção de atleta (T-018/US-001,
 * T-058): sucesso (201 + Location + DTO), validação (400), soft-delete (204) e
 * autorização — um coach não remove atleta de outro coach mesmo forçando ids.
 *
 * <p>Usa token JWT real (nunca {@code @WithMockUser}) para exercitar a
 * resolução server-side do coachId a partir do subject do JWT.</p>
 */
@SpringBootTest(classes = ProcoachApplication.class)
@AutoConfigureMockMvc
@Import(PostgresTestcontainersConfiguration.class)
class AthleteControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AthleteRepository repository;

	private String validRequestJson() {
		return """
				{
				  "name": "Ana Souza",
				  "sex": "FEMALE",
				  "birthDate": "1995-07-20",
				  "height": 165.50,
				  "weight": 60.20,
				  "goal": "HYPERTROPHY"
				}
				""";
	}

	@Test
	void cadastraAtletaComSucesso() throws Exception {
		String coachToken = registerCoach(mockMvc, uniqueEmail("coach"));

		MvcResult result = mockMvc.perform(post("/api/v1/athletes")
						.header("Authorization", bearer(coachToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRequestJson()))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location",
						org.hamcrest.Matchers.matchesRegex(".*/api/v1/athletes/[0-9a-f-]{36}")))
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.coachId").isNotEmpty())
				.andExpect(jsonPath("$.name").value("Ana Souza"))
				.andExpect(jsonPath("$.sex").value("FEMALE"))
				.andExpect(jsonPath("$.birthDate").value("1995-07-20"))
				.andExpect(jsonPath("$.height").value(165.50))
				.andExpect(jsonPath("$.weight").value(60.20))
				.andExpect(jsonPath("$.goal").value("HYPERTROPHY"))
				.andExpect(jsonPath("$.status").value("ACTIVE"))
				.andReturn();

		String location = result.getResponse().getHeader("Location");
		UUID id = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
		assertThat(repository.existsById(AthleteId.of(id))).isTrue();
	}

	@Test
	void rejeitaRequisicaoComCamposObrigatoriosAusentes() throws Exception {
		String coachToken = registerCoach(mockMvc, uniqueEmail("coach"));

		mockMvc.perform(post("/api/v1/athletes")
						.header("Authorization", bearer(coachToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fields.name").exists())
				.andExpect(jsonPath("$.fields.sex").exists())
				.andExpect(jsonPath("$.fields.birthDate").exists())
				.andExpect(jsonPath("$.fields.height").exists())
				.andExpect(jsonPath("$.fields.weight").exists())
				.andExpect(jsonPath("$.fields.goal").exists());
	}

	@Test
	void rejeitaCamposInvalidos() throws Exception {
		String coachToken = registerCoach(mockMvc, uniqueEmail("coach"));
		String invalid = """
				{
				  "name": "   ",
				  "sex": "MALE",
				  "birthDate": "%s",
				  "height": -10,
				  "weight": 0,
				  "goal": "CUTTING"
				}
				""".formatted(LocalDate.now().plusDays(1));

		mockMvc.perform(post("/api/v1/athletes")
						.header("Authorization", bearer(coachToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(invalid))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fields.name").exists())
				.andExpect(jsonPath("$.fields.birthDate").exists())
				.andExpect(jsonPath("$.fields.height").exists())
				.andExpect(jsonPath("$.fields.weight").exists());
	}

	@Test
	void rejeitaObjetivoInexistente() throws Exception {
		String coachToken = registerCoach(mockMvc, uniqueEmail("coach"));
		String invalid = validRequestJson().replace("\"HYPERTROPHY\"", "\"OBJETIVO_INVALIDO\"");

		mockMvc.perform(post("/api/v1/athletes")
						.header("Authorization", bearer(coachToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content(invalid))
				.andExpect(status().isBadRequest());
	}

	@Test
	void removeAtletaComSoftDelete() throws Exception {
		String coachToken = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coachToken);

		mockMvc.perform(delete("/api/v1/athletes/{id}", athleteId)
						.header("Authorization", bearer(coachToken)))
				.andExpect(status().isNoContent());

		// Soft-delete: o registro continua no banco, apenas marcado como excluído.
		assertThat(repository.findById(AthleteId.of(UUID.fromString(athleteId)))).isPresent();
		assertThat(repository.findById(AthleteId.of(UUID.fromString(athleteId))).get().isDeleted()).isTrue();
	}

	// ---- T-058 item 2: autorização ----

	@Test
	void coachNaoRemoveAtletaDeOutroCoach() throws Exception {
		String coachA = registerCoach(mockMvc, uniqueEmail("coach-a"));
		String coachB = registerCoach(mockMvc, uniqueEmail("coach-b"));
		String athleteOfA = registerAthlete(mockMvc, coachA);

		// Coach B tenta remover um atleta da carteira do coach A -> 404.
		mockMvc.perform(delete("/api/v1/athletes/{id}", athleteOfA)
						.header("Authorization", bearer(coachB)))
				.andExpect(status().isNotFound());

		// O atleta continua ativo (não foi removido).
		assertThat(repository.findById(AthleteId.of(UUID.fromString(athleteOfA))).get().isDeleted()).isFalse();
	}

	@Test
	void coachIdForjadoNoPayloadEhIgnorado() throws Exception {
		String coachA = registerCoach(mockMvc, uniqueEmail("coach-a"));
		String forgedCoachId = UUID.randomUUID().toString();

		// O coachId no body é ignorado (não existe mais no DTO): o atleta é criado
		// para o coach autenticado (A), nunca para o coachId forjado.
		MvcResult result = mockMvc.perform(post("/api/v1/athletes")
						.header("Authorization", bearer(coachA))
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRequestJson().replace("{", "{\"coachId\": \"%s\",".formatted(forgedCoachId))))
				.andExpect(status().isCreated())
				.andReturn();

		String location = result.getResponse().getHeader("Location");
		UUID id = UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
		// O atleta pertence ao coach A (autenticado), não ao coachId forjado.
		assertThat(repository.findById(AthleteId.of(id)).get().coachId().toString()).isNotEqualTo(forgedCoachId);
	}
}
