package com.fitness.procoach.library;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/** Contrato HTTP da biblioteca consumido pelo workspace de treino T-040. */
@SpringBootTest(classes = ProcoachApplication.class)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("h2")
@Transactional
class ExerciseLibraryApiIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	private String id(MvcResult result) throws Exception {
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private String createExercise(String coach, String name) throws Exception {
		return id(mockMvc.perform(post("/api/library/exercises")
					.header("Authorization", bearer(coach))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "name": "%s", "muscleGroups": ["PEITO", "TRICEPS"], "equipment": "BARRA_LIVRE",
							  "execution": "Desça a barra até o peito e empurre.", "commonErrors": ["Arquear a lombar"],
							  "progressions": ["Aumentar carga"], "regressions": ["Reduzir carga"],
							  "contraindications": [], "references": [] }
							""".formatted(name)))
				.andExpect(status().isCreated())
				.andReturn());
	}

	@Test
	void buscaExercicioPeloNomeEGrupoMuscularComCamposDaBiblioteca() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("training-library"));
		String exerciseId = createExercise(coach, "Supino reto com barra");

		mockMvc.perform(get("/api/library/exercises")
					.header("Authorization", bearer(coach))
					.param("name", "supino")
					.param("muscleGroup", "PEITO"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(exerciseId))
				.andExpect(jsonPath("$[0].name").value("Supino reto com barra"))
				.andExpect(jsonPath("$[0].muscleGroups[0]").value("PEITO"))
				.andExpect(jsonPath("$[0].equipment").value("BARRA_LIVRE"))
				.andExpect(jsonPath("$[0].execution").isNotEmpty())
				.andExpect(jsonPath("$[0].commonErrors[0]").value("Arquear a lombar"))
				.andExpect(jsonPath("$[0].version").value(1))
				.andExpect(jsonPath("$[0].superseded").value(false));
	}
}
