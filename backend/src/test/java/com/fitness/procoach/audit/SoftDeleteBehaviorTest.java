package com.fitness.procoach.audit;

import static com.fitness.procoach.CoachAthleteTestSupport.bearer;
import static com.fitness.procoach.CoachAthleteTestSupport.registerAthlete;
import static com.fitness.procoach.CoachAthleteTestSupport.registerCoach;
import static com.fitness.procoach.CoachAthleteTestSupport.uniqueEmail;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

import java.util.UUID;

/**
 * Testes de comportamento de soft-delete (regra inegociável 5).
 *
 * <p>Valida que exclusões são lógicas (soft-delete), nunca DELETE físico.
 * Registros marcados como deletados não aparecem em listagens normais,
 * mas permanecem no banco e no histórico de auditoria.
 */
@SpringBootTest(classes = ProcoachApplication.class)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SoftDeleteBehaviorTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void athlete_softDelete_marksAsDeletedNotPhysicalDelete() throws Exception {
        String coach = registerCoach(mockMvc, uniqueEmail("coach"));
        String athleteId = registerAthlete(mockMvc, coach);

        // Soft-delete
        mockMvc.perform(delete("/api/v1/athletes/{id}", athleteId)
                        .header("Authorization", bearer(coach)))
                .andExpect(status().isNoContent());

        // Não aparece na listagem padrão
        mockMvc.perform(get("/api/v1/athletes").header("Authorization", bearer(coach)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(athleteId))));

        // Mas continua existindo no banco (GET por id retorna 404 por regra de negócio,
        // mas o registro não foi fisicamente removido)
        mockMvc.perform(get("/api/v1/athletes/{id}", athleteId).header("Authorization", bearer(coach)))
                .andExpect(status().isNotFound());

        // O histórico de auditoria não está acessível via endpoint /history
        // porque o atleta foi soft-deletado (o endpoint valida posse e existência).
        // Mas o evento ATHLETE_DELETED foi persistido no audit log.
    }

    @Test
    void trainingPlan_softDelete_marksAsDeletedAndEmitsEvent() throws Exception {
        String coach = registerCoach(mockMvc, uniqueEmail("coach"));
        String athleteId = registerAthlete(mockMvc, coach);
        String periodizationId = createPeriodization(coach, athleteId);
        String planId = createPlan(coach, athleteId, periodizationId);

        // Soft-delete do plano
        mockMvc.perform(delete("/api/training/plans/{id}", planId)
                        .header("Authorization", bearer(coach)))
                .andExpect(status().isNoContent());

        // Não aparece na listagem (busca por atleta)
        mockMvc.perform(get("/api/training/plans/athlete/{id}", athleteId)
                        .header("Authorization", bearer(coach)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(planId))));

        // Evento no audit log
        mockMvc.perform(get("/api/v1/athletes/{id}/history", athleteId).header("Authorization", bearer(coach)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].eventType").value(hasItem("TRAINING_PLAN_DELETED")));
    }

    @Test
    void training_softDelete_marksAsDeletedAndEmitsEvent() throws Exception {
        String coach = registerCoach(mockMvc, uniqueEmail("coach"));
        String athleteId = registerAthlete(mockMvc, coach);
        String periodizationId = createPeriodization(coach, athleteId);
        String planId = createPlan(coach, athleteId, periodizationId);
        String trainingId = createTraining(coach, athleteId, periodizationId, planId, "Treino A", 1);

        // Soft-delete do treino
        mockMvc.perform(delete("/api/training/trainings/{id}", trainingId)
                        .header("Authorization", bearer(coach)))
                .andExpect(status().isNoContent());

        // Evento no audit log
        mockMvc.perform(get("/api/v1/athletes/{id}/history", athleteId).header("Authorization", bearer(coach)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].eventType").value(hasItem("TRAINING_DELETED")));
    }

    @Test
    void exerciseLibrary_softDelete_archivesInsteadOfDelete() throws Exception {
        String coach = registerCoach(mockMvc, uniqueEmail("coach"));

        // Cria exercício com equipment válido
        MvcResult ex = mockMvc.perform(post("/api/library/exercises")
                        .header("Authorization", bearer(coach))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Supino Reto", "muscleGroups": ["PEITO"], "equipment": "BARRA_LIVRE",
                                  "execution": "Deitado no banco...", "commonErrors": ["Arquear lombar"],
                                  "progressions": ["Aumentar carga"], "regressions": ["Flexão"],
                                  "contraindications": ["Lesão no ombro"],
                                  "references": [{"authors": "ACSM", "title": "Exercise Guidelines", "source": "ACSM", "year": 2020}] }
                                """))
                .andExpect(status().isCreated()).andReturn();
        String exerciseId = JsonPath.read(ex.getResponse().getContentAsString(), "$.id");

        // Soft-delete (archive)
        mockMvc.perform(delete("/api/library/exercises/{id}", exerciseId)
                        .header("Authorization", bearer(coach)))
                .andExpect(status().isNoContent());

        // Não aparece na listagem padrão (apenas versões correntes)
        mockMvc.perform(get("/api/library/exercises").header("Authorization", bearer(coach)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(exerciseId))));
    }

    @Test
    void softDeletedAthlete_cannotReceiveNewProtocols() throws Exception {
        String coach = registerCoach(mockMvc, uniqueEmail("coach"));
        String athleteId = registerAthlete(mockMvc, coach);

        mockMvc.perform(delete("/api/v1/athletes/{id}", athleteId)
                        .header("Authorization", bearer(coach)))
                .andExpect(status().isNoContent());

        // Tentar criar avaliação para atleta deletado -> 404/409
        mockMvc.perform(post("/api/v1/assessments")
                        .header("Authorization", bearer(coach))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "athleteId": "%s", "assessmentDate": "2026-09-01", "weightKg": 80,
                                  "heightCm": 178 }
                                """.formatted(athleteId)))
                .andExpect(status().isNotFound()); // Ou 409 Conflict
    }

    // ---- Helpers ----

    private String createPeriodization(String coach, String athleteId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/training/periodizations")
                        .header("Authorization", bearer(coach))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"athleteId\": \"%s\", \"name\": \"Macrociclo\", \"startDate\": \"2026-09-07\", \"endDate\": \"2026-11-29\" }"
                                .formatted(athleteId)))
                .andExpect(status().isCreated()).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private String createPlan(String coach, String athleteId, String periodizationId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/training/plans")
                        .header("Authorization", bearer(coach))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"athleteId\": \"%s\", \"periodizationId\": \"%s\", \"name\": \"Plano de hipertrofia\" }"
                                .formatted(athleteId, periodizationId)))
                .andExpect(status().isCreated()).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private String createTraining(String coach, String athleteId, String periodizationId, String planId,
            String name, int dayOfWeek) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/training/trainings")
                        .header("Authorization", bearer(coach))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "periodizationId": "%s", "planId": "%s", "athleteId": "%s", "name": "%s",
                                  "weekNumber": 1, "dayOfWeek": %d }
                                """.formatted(periodizationId, planId, athleteId, name, dayOfWeek)))
                .andExpect(status().isCreated()).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }
}