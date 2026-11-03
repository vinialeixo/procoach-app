package com.fitness.procoach.training.interfaces.rest;

import static com.fitness.procoach.CoachAthleteTestSupport.bearer;
import static com.fitness.procoach.CoachAthleteTestSupport.registerAthlete;
import static com.fitness.procoach.CoachAthleteTestSupport.registerCoach;
import static com.fitness.procoach.CoachAthleteTestSupport.uniqueEmail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
 * Aviso de versão mais nova (T-066): o treino continua apontando para a versão do
 * exercício que o coach usou e indica quando a biblioteca já tem uma versão corrente.
 */
@SpringBootTest(classes = ProcoachApplication.class)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TrainingExerciseVersionIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	private String id(MvcResult result) throws Exception {
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private String createLibraryExercise(String coach, String name) throws Exception {
		return id(mockMvc.perform(post("/api/library/exercises")
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "%s", "muscleGroups": ["PEITO"], "equipment": "BARRA_LIVRE",
								  "execution": "Desça a barra até o peito e empurre.", "commonErrors": [],
								  "progressions": [], "regressions": [], "contraindications": [], "references": [] }
								""".formatted(name)))
				.andExpect(status().isCreated())
				.andReturn());
	}

	private String editLibraryExercise(String coach, String exerciseId, String name) throws Exception {
		return id(mockMvc.perform(put("/api/library/exercises/{id}", exerciseId)
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "%s", "muscleGroups": ["PEITO"], "equipment": "HALTERES",
								  "execution": "Desça os halteres até a linha do peito.", "commonErrors": [],
								  "progressions": [], "regressions": [], "contraindications": [], "references": [] }
								""".formatted(name)))
				.andExpect(status().isOk())
				.andReturn());
	}

	private String createTraining(String coach, String athleteId) throws Exception {
		String periodizationId = id(mockMvc.perform(post("/api/training/periodizations")
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{ \"athleteId\": \"%s\", \"name\": \"Macrociclo\", \"startDate\": \"2026-09-07\", \"endDate\": \"2026-11-29\" }"
								.formatted(athleteId)))
				.andExpect(status().isCreated())
				.andReturn());
		return id(mockMvc.perform(post("/api/training/trainings")
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "periodizationId": "%s", "athleteId": "%s", "name": "Treino A", "weekNumber": 1, "dayOfWeek": 1 }
								""".formatted(periodizationId, athleteId)))
				.andExpect(status().isCreated())
				.andReturn());
	}

	private void addLibraryExercise(String coach, String trainingId, String exerciseId, int orderIndex)
			throws Exception {
		mockMvc.perform(post("/api/training/trainings/{id}/exercises", trainingId)
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "exerciseId": "%s", "exerciseName": "Supino reto com barra", "customExercise": false, "orderIndex": %d, "sets": 4,
								  "repetitions": 10, "load": 60.0, "intensityType": "RPE", "intensityValue": 8.0,
								  "restSeconds": 90 }
								""".formatted(exerciseId, orderIndex)))
				.andExpect(status().isOk());
	}

	@Test
	void prescricaoAvisaQuandoOExercicioDaBibliotecaGanhaVersaoMaisNova() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String v1 = createLibraryExercise(coach, "Supino reto com barra");
		String trainingId = createTraining(coach, athleteId);
		addLibraryExercise(coach, trainingId, v1, 0);

		// Versão corrente: sem aviso.
		mockMvc.perform(get("/api/training/trainings/{id}", trainingId).header("Authorization", bearer(coach)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.exercises[0].exerciseVersion").value(1))
				.andExpect(jsonPath("$.exercises[0].newerVersionAvailable").value(false))
				.andExpect(jsonPath("$.exercises[0].currentExerciseId").doesNotExist());

		String v2 = editLibraryExercise(coach, v1, "Supino reto com halteres");

		// A prescrição segue na v1, mas indica a versão corrente.
		mockMvc.perform(get("/api/training/trainings/{id}", trainingId).header("Authorization", bearer(coach)))
				.andExpect(jsonPath("$.exercises[0].exerciseId").value(v1))
				.andExpect(jsonPath("$.exercises[0].exerciseVersion").value(1))
				.andExpect(jsonPath("$.exercises[0].newerVersionAvailable").value(true))
				.andExpect(jsonPath("$.exercises[0].currentExerciseId").value(v2));
	}

	@Test
	void avisoSegueACadeiaAteAVersaoCorrente() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String v1 = createLibraryExercise(coach, "Supino reto com barra");
		String trainingId = createTraining(coach, athleteId);
		addLibraryExercise(coach, trainingId, v1, 0);

		String v2 = editLibraryExercise(coach, v1, "Supino inclinado");
		String v3 = editLibraryExercise(coach, v2, "Supino declinado");

		mockMvc.perform(get("/api/training/trainings/{id}", trainingId).header("Authorization", bearer(coach)))
				.andExpect(jsonPath("$.exercises[0].newerVersionAvailable").value(true))
				.andExpect(jsonPath("$.exercises[0].currentExerciseId").value(v3));
	}

	@Test
	void exercicioPersonalizadoNaoTemAvisoDeVersao() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String trainingId = createTraining(coach, athleteId);
		mockMvc.perform(post("/api/training/trainings/{id}/exercises", trainingId)
						.header("Authorization", bearer(coach))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "exerciseName": "Meu exercício", "customExercise": true, "orderIndex": 0, "sets": 3,
								  "repetitions": 12, "load": 20.0, "intensityType": "RPE", "intensityValue": 7.0,
								  "restSeconds": 60 }
								"""))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/training/trainings/{id}", trainingId).header("Authorization", bearer(coach)))
				.andExpect(jsonPath("$.exercises[0].newerVersionAvailable").value(false))
				.andExpect(jsonPath("$.exercises[0].exerciseVersion").doesNotExist());
	}

	@Test
	void listagemDeTreinosTambemTrazOAviso() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String v1 = createLibraryExercise(coach, "Supino reto com barra");
		String trainingId = createTraining(coach, athleteId);
		addLibraryExercise(coach, trainingId, v1, 0);
		editLibraryExercise(coach, v1, "Supino reto com halteres");

		mockMvc.perform(get("/api/training/trainings/athlete/{id}", athleteId)
						.header("Authorization", bearer(coach)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].exercises[0].newerVersionAvailable").value(true));
	}

	@Test
	void versaoCorrenteRemovidaNaoGeraAviso() throws Exception {
		String coach = registerCoach(mockMvc, uniqueEmail("coach"));
		String athleteId = registerAthlete(mockMvc, coach);
		String v1 = createLibraryExercise(coach, "Supino reto com barra");
		String trainingId = createTraining(coach, athleteId);
		addLibraryExercise(coach, trainingId, v1, 0);
		String v2 = editLibraryExercise(coach, v1, "Supino reto com halteres");

		mockMvc.perform(delete("/api/library/exercises/{id}", v2).header("Authorization", bearer(coach)))
				.andExpect(status().isNoContent());

		// A sucessora foi removida: não há versão corrente para sugerir.
		mockMvc.perform(get("/api/training/trainings/{id}", trainingId).header("Authorization", bearer(coach)))
				.andExpect(jsonPath("$.exercises[0].newerVersionAvailable").value(false));
	}
}
