package com.fitness.procoach.athlete.interfaces;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitness.procoach.ProcoachApplication;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Testes de integração do endpoint de cadastro de atleta (T-018/US-001):
 * sucesso (201 + Location + DTO) e falha de validação (400 detalhando campos).
 */
@SpringBootTest(classes = ProcoachApplication.class)
@AutoConfigureMockMvc
@Import(PostgresTestcontainersConfiguration.class)
@WithMockUser
class AthleteControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AthleteRepository repository;

	private String validRequestJson() {
		return """
				{
				  "coachId": "22222222-2222-2222-2222-222222222222",
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
		MvcResult result = mockMvc.perform(post("/api/v1/athletes")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(validRequestJson()))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location",
						org.hamcrest.Matchers.matchesRegex(".*/api/v1/athletes/[0-9a-f-]{36}")))
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.coachId").value("22222222-2222-2222-2222-222222222222"))
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
		assertThat(repository.existsById(com.fitness.procoach.athlete.domain.AthleteId.of(id))).isTrue();
	}

	@Test
	void rejeitaRequisicaoComCamposObrigatoriosAusentes() throws Exception {
		mockMvc.perform(post("/api/v1/athletes")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fields.coachId").exists())
				.andExpect(jsonPath("$.fields.name").exists())
				.andExpect(jsonPath("$.fields.sex").exists())
				.andExpect(jsonPath("$.fields.birthDate").exists())
				.andExpect(jsonPath("$.fields.height").exists())
				.andExpect(jsonPath("$.fields.weight").exists())
				.andExpect(jsonPath("$.fields.goal").exists());
	}

	@Test
	void rejeitaCamposInvalidos() throws Exception {
		String invalid = """
				{
				  "coachId": "22222222-2222-2222-2222-222222222222",
				  "name": "   ",
				  "sex": "MALE",
				  "birthDate": "%s",
				  "height": -10,
				  "weight": 0,
				  "goal": "CUTTING"
				}
				""".formatted(LocalDate.now().plusDays(1));

		mockMvc.perform(post("/api/v1/athletes")
						.with(csrf())
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
		String invalid = validRequestJson().replace("\"HYPERTROPHY\"", "\"OBJETIVO_INVALIDO\"");

		mockMvc.perform(post("/api/v1/athletes")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(invalid))
				.andExpect(status().isBadRequest());
	}
}
