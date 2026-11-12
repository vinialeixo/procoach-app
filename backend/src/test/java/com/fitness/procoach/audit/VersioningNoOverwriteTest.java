package com.fitness.procoach.audit;

import static com.fitness.procoach.CoachAthleteTestSupport.bearer;
import static com.fitness.procoach.CoachAthleteTestSupport.registerAthlete;
import static com.fitness.procoach.CoachAthleteTestSupport.registerCoach;
import static com.fitness.procoach.CoachAthleteTestSupport.uniqueEmail;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitness.procoach.ProcoachApplication;
import com.fitness.procoach.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
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
 * Testes de versionamento sem sobrescrita (US-040, US-041, regra inegociável 3).
 *
 * <p>Valida que Diet, TrainingPlan e Assessment criam novas versões em vez de
 * sobrescrever, mantendo versões anteriores imutáveis e consultáveis.
 */
@SpringBootTest(classes = ProcoachApplication.class)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class VersioningNoOverwriteTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.fitness.procoach.security.application.AuthService authService;

    // ---- Diet versioning (US-015, US-016) ----

    @Test
    void diet_createsNewVersionInsteadOfOverwriting() throws Exception {
        String coach = registerCoach(mockMvc, uniqueEmail("coach"));
        String athleteId = registerAthlete(mockMvc, coach);

        // Cria v1
        MvcResult v1 = mockMvc.perform(post("/api/v1/diets")
                        .header("Authorization", bearer(coach))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "athleteId": "%s", "objective": "CUTTING", "calories": 2000,
                                  "protein": 150, "carbohydrates": 200, "fat": 60 }
                                """.formatted(athleteId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.readOnly").value(false))
                .andReturn();

        String v1Id = JsonPath.read(v1.getResponse().getContentAsString(), "$.id");

        // Aprova v1
        mockMvc.perform(post("/api/v1/diets/{id}/status", v1Id)
                        .header("Authorization", bearer(coach))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"status\": \"IN_REVIEW\" }"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/diets/{id}/status", v1Id)
                        .header("Authorization", bearer(coach))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"status\": \"APPROVED\" }"))
                .andExpect(status().isOk());

        // Cria v2 (nova versão, não sobrescreve)
        MvcResult v2 = mockMvc.perform(post("/api/v1/diets/{id}/versions", v1Id)
                        .header("Authorization", bearer(coach))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "objective": "CUTTING", "calories": 1800, "protein": 160,
                                  "carbohydrates": 180, "fat": 50, "changeReason": "Redução calórica" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.readOnly").value(false))
                .andExpect(jsonPath("$.calories").value(1800.0))
                .andReturn();

        String v2Id = JsonPath.read(v2.getResponse().getContentAsString(), "$.id");

        // v1 continua existindo e imutável (readOnly = true)
        mockMvc.perform(get("/api/v1/diets/{id}", v1Id)
                        .header("Authorization", bearer(coach)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.calories").value(2000.0))
                .andExpect(jsonPath("$.readOnly").value(true));

        // v2 é a corrente
        mockMvc.perform(get("/api/v1/diets/{id}", v2Id)
                        .header("Authorization", bearer(coach)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.calories").value(1800.0));

        // Histórico mostra ambas as versões
        mockMvc.perform(get("/api/v1/diets/history").param("athleteId", athleteId)
                        .header("Authorization", bearer(coach)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].version").value(1))
                .andExpect(jsonPath("$[1].version").value(2));
    }

    @Test
    void diet_versionChain_maintainsPreviousVersionReference() throws Exception {
        String coach = registerCoach(mockMvc, uniqueEmail("coach"));
        String athleteId = registerAthlete(mockMvc, coach);

        MvcResult v1 = mockMvc.perform(post("/api/v1/diets")
                        .header("Authorization", bearer(coach))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validDietJson(athleteId, 2500)))
                .andExpect(status().isCreated()).andReturn();
        String v1Id = JsonPath.read(v1.getResponse().getContentAsString(), "$.id");

        MvcResult v2 = mockMvc.perform(post("/api/v1/diets/{id}/versions", v1Id)
                        .header("Authorization", bearer(coach))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(versionJson(2200, "ajuste 1")))
                .andExpect(status().isCreated()).andReturn();
        String v2Id = JsonPath.read(v2.getResponse().getContentAsString(), "$.id");

        MvcResult v3 = mockMvc.perform(post("/api/v1/diets/{id}/versions", v2Id)
                        .header("Authorization", bearer(coach))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(versionJson(2000, "ajuste 2")))
                .andExpect(status().isCreated()).andReturn();
        String v3Id = JsonPath.read(v3.getResponse().getContentAsString(), "$.id");

        // v3 referencia v2, v2 referencia v1
        mockMvc.perform(get("/api/v1/diets/{id}", v3Id).header("Authorization", bearer(coach)))
                .andExpect(jsonPath("$.previousVersionId").value(v2Id));
        mockMvc.perform(get("/api/v1/diets/{id}", v2Id).header("Authorization", bearer(coach)))
                .andExpect(jsonPath("$.previousVersionId").value(v1Id));
        mockMvc.perform(get("/api/v1/diets/{id}", v1Id).header("Authorization", bearer(coach)))
                .andExpect(jsonPath("$.previousVersionId").doesNotExist());
    }

    @Test
    void diet_previousVersionsAreReadOnly_cannotBeModified() throws Exception {
        String coach = registerCoach(mockMvc, uniqueEmail("coach"));
        String athleteId = registerAthlete(mockMvc, coach);

        MvcResult v1 = mockMvc.perform(post("/api/v1/diets")
                        .header("Authorization", bearer(coach))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validDietJson(athleteId, 2500)))
                .andExpect(status().isCreated()).andReturn();
        String v1Id = JsonPath.read(v1.getResponse().getContentAsString(), "$.id");

        MvcResult v2 = mockMvc.perform(post("/api/v1/diets/{id}/versions", v1Id)
                        .header("Authorization", bearer(coach))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(versionJson(2200, "ajuste")))
                .andExpect(status().isCreated()).andReturn();

        // Tentar criar versão a partir de v1 (read-only) deve falhar com 409
        mockMvc.perform(post("/api/v1/diets/{id}/versions", v1Id)
                        .header("Authorization", bearer(coach))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(versionJson(2000, "tentativa")))
                .andExpect(status().isConflict());
    }

    @Test
    void diet_changeReason_isMandatoryForNewVersion() throws Exception {
        String coach = registerCoach(mockMvc, uniqueEmail("coach"));
        String athleteId = registerAthlete(mockMvc, coach);

        MvcResult v1 = mockMvc.perform(post("/api/v1/diets")
                        .header("Authorization", bearer(coach))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validDietJson(athleteId, 2500)))
                .andExpect(status().isCreated()).andReturn();
        String v1Id = JsonPath.read(v1.getResponse().getContentAsString(), "$.id");

        // Sem changeReason -> 400
        mockMvc.perform(post("/api/v1/diets/{id}/versions", v1Id)
                        .header("Authorization", bearer(coach))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "objective": "CUTTING", "calories": 2200, "protein": 150,
                                  "carbohydrates": 200, "fat": 60 }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.changeReason").exists());
    }

    // ---- TrainingPlan versioning (T-067) ----

    @Test
    void trainingPlan_createsNewVersionCopiesSessionsLeavesOriginalIntact() throws Exception {
        String coach = registerCoach(mockMvc, uniqueEmail("coach"));
        String athleteId = registerAthlete(mockMvc, coach);
        String periodizationId = createPeriodization(coach, athleteId);

        // Cria plano v1 com sessões
        String v1 = createPlan(coach, athleteId, periodizationId);
        String a = createTraining(coach, athleteId, periodizationId, v1, "Treino A", 1);
        String b = createTraining(coach, athleteId, periodizationId, v1, "Treino B", 3);
        addExercise(coach, a, "Supino", 0);
        addExercise(coach, a, "Agachamento", 1);
        addExercise(coach, b, "Remada", 0);

        // Cria v2
        String v2 = newVersion(coach, v1);

        // v2 tem cópias das sessões (mesmos nomes, prescrições), em DRAFT
        mockMvc.perform(get("/api/training/trainings/athlete/{id}", athleteId).param("planId", v2)
                        .header("Authorization", bearer(coach)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].name").value(hasItems("Treino A", "Treino B")))
                .andExpect(jsonPath("$[*].status").value(hasItem("DRAFT")))
                .andExpect(jsonPath("$[?(@.name=='Treino A')].exercises.length()").value(hasItem(2)))
                .andExpect(jsonPath("$[?(@.name=='Treino A')].planId").value(hasItem(v2)));

        // v1 continua intacta com as mesmas sessões e prescrições originais
        mockMvc.perform(get("/api/training/trainings/athlete/{id}", athleteId).param("planId", v1)
                        .header("Authorization", bearer(coach)))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.name=='Treino A')].exercises.length()").value(hasItem(2)))
                .andExpect(jsonPath("$[*].id").value(hasItems(a, b)));
    }

    @Test
    void trainingPlan_versionChain_maintainsPreviousVersionReference() throws Exception {
        String coach = registerCoach(mockMvc, uniqueEmail("coach"));
        String athleteId = registerAthlete(mockMvc, coach);
        String periodizationId = createPeriodization(coach, athleteId);

        String v1 = createPlan(coach, athleteId, periodizationId);
        String v2 = newVersion(coach, v1);
        String v3 = newVersion(coach, v2);

        // v3 referencia v2, v2 referencia v1
        mockMvc.perform(get("/api/training/plans/{id}", v3).header("Authorization", bearer(coach)))
                .andExpect(jsonPath("$.previousVersionId").value(v2));
        mockMvc.perform(get("/api/training/plans/{id}", v2).header("Authorization", bearer(coach)))
                .andExpect(jsonPath("$.previousVersionId").value(v1));
        mockMvc.perform(get("/api/training/plans/{id}", v1).header("Authorization", bearer(coach)))
                .andExpect(jsonPath("$.previousVersionId").doesNotExist());
    }

    @Test
    void trainingPlan_editingV2SessionsDoesNotAffectV1() throws Exception {
        String coach = registerCoach(mockMvc, uniqueEmail("coach"));
        String athleteId = registerAthlete(mockMvc, coach);
        String periodizationId = createPeriodization(coach, athleteId);
        String v1 = createPlan(coach, athleteId, periodizationId);
        String a = createTraining(coach, athleteId, periodizationId, v1, "Treino A", 1);
        addExercise(coach, a, "Supino", 0);
        String v2 = newVersion(coach, v1);

        MvcResult v2Sessions = mockMvc.perform(get("/api/training/trainings/athlete/{id}", athleteId).param("planId", v2)
                        .header("Authorization", bearer(coach)))
                .andExpect(jsonPath("$.length()").value(1)).andReturn();
        String v2Training = JsonPath.read(v2Sessions.getResponse().getContentAsString(), "$[0].id");
        String v2Prescription = JsonPath.read(v2Sessions.getResponse().getContentAsString(), "$[0].exercises[0].id");

        // Edita prescrição na v2
        mockMvc.perform(put("/api/training/trainings/{id}/exercises/{pid}", v2Training, v2Prescription)
                        .header("Authorization", bearer(coach))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "exerciseName": "Supino", "sets": 6, "repetitions": 8, "load": 70.0,
                                  "intensityType": "RPE", "intensityValue": 9.0, "restSeconds": 120 }
                                """))
                .andExpect(status().isOk());

        // v1 inalterada
        mockMvc.perform(get("/api/training/trainings/{id}", a).header("Authorization", bearer(coach)))
                .andExpect(jsonPath("$.exercises[0].sets").value(4))
                .andExpect(jsonPath("$.exercises[0].repetitions").value(10));
        // v2 alterada
        mockMvc.perform(get("/api/training/trainings/{id}", v2Training).header("Authorization", bearer(coach)))
                .andExpect(jsonPath("$.exercises[0].sets").value(6));
    }

    @Test
    void trainingPlan_activatingV2FinishesV1AndPublishesNewSessions() throws Exception {
        String coach = registerCoach(mockMvc, uniqueEmail("coach"));
        String athleteId = registerAthlete(mockMvc, coach);
        String athlete = athleteToken(athleteId);
        String periodizationId = createPeriodization(coach, athleteId);
        String v1 = createPlan(coach, athleteId, periodizationId);
        createTraining(coach, athleteId, periodizationId, v1, "Treino A", 1);
        createTraining(coach, athleteId, periodizationId, v1, "Treino B", 3);
        activatePlan(coach, v1);
        String v2 = newVersion(coach, v1);
        activatePlan(coach, v2);

        // v1 vira histórico (FINISHED)
        mockMvc.perform(get("/api/training/plans/{id}", v1).header("Authorization", bearer(coach)))
                .andExpect(jsonPath("$.status").value("FINISHED"));
        // v2 passa a valer (ACTIVE)
        mockMvc.perform(get("/api/training/plans/{id}", v2).header("Authorization", bearer(coach)))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        // Sessões da v1 FINISHED
        mockMvc.perform(get("/api/training/trainings/athlete/{id}", athleteId).param("planId", v1)
                        .header("Authorization", bearer(athlete)))
                .andExpect(jsonPath("$[*].status").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("FINISHED"))));
        // Sessões da v2 ACTIVE
        mockMvc.perform(get("/api/training/trainings/athlete/{id}", athleteId).param("planId", v2)
                        .header("Authorization", bearer(athlete)))
                .andExpect(jsonPath("$[*].status").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("ACTIVE"))));
    }

    // ---- Assessment versioning (US-008) ----

    @Test
    void assessment_neverOverwrites_createsNewRecordEachTime() throws Exception {
        String coach = registerCoach(mockMvc, uniqueEmail("coach"));
        String athleteId = registerAthlete(mockMvc, coach);

        // Registra 3 avaliações em datas diferentes
        mockMvc.perform(post("/api/v1/assessments")
                        .header("Authorization", bearer(coach))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assessmentJson(athleteId, "2026-09-01", 80.0)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/assessments")
                        .header("Authorization", bearer(coach))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assessmentJson(athleteId, "2026-08-01", 82.0)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/assessments")
                        .header("Authorization", bearer(coach))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assessmentJson(athleteId, "2026-07-01", 84.0)))
                .andExpect(status().isCreated());

        // Histórico retorna todas em ordem cronológica (mais antiga primeiro)
        mockMvc.perform(get("/api/v1/assessments").param("athleteId", athleteId)
                        .header("Authorization", bearer(coach)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].assessmentDate").value("2026-07-01"))
                .andExpect(jsonPath("$[1].assessmentDate").value("2026-08-01"))
                .andExpect(jsonPath("$[2].assessmentDate").value("2026-09-01"))
                .andExpect(jsonPath("$[0].weightKg").value(84.0))
                .andExpect(jsonPath("$[1].weightKg").value(82.0))
                .andExpect(jsonPath("$[2].weightKg").value(80.0));
    }

    @Test
    void assessment_canQueryAnyPreviousAssessmentById() throws Exception {
        String coach = registerCoach(mockMvc, uniqueEmail("coach"));
        String athleteId = registerAthlete(mockMvc, coach);

        MvcResult a1 = mockMvc.perform(post("/api/v1/assessments")
                        .header("Authorization", bearer(coach))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assessmentJson(athleteId, "2026-07-01", 84.0)))
                .andExpect(status().isCreated()).andReturn();
        String id1 = JsonPath.read(a1.getResponse().getContentAsString(), "$.id");

        MvcResult a2 = mockMvc.perform(post("/api/v1/assessments")
                        .header("Authorization", bearer(coach))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assessmentJson(athleteId, "2026-08-01", 82.0)))
                .andExpect(status().isCreated()).andReturn();
        String id2 = JsonPath.read(a2.getResponse().getContentAsString(), "$.id");

        // Consulta cada uma individualmente
        mockMvc.perform(get("/api/v1/assessments/{id}", id1).header("Authorization", bearer(coach)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assessmentDate").value("2026-07-01"))
                .andExpect(jsonPath("$.weightKg").value(84.0));
        mockMvc.perform(get("/api/v1/assessments/{id}", id2).header("Authorization", bearer(coach)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assessmentDate").value("2026-08-01"))
                .andExpect(jsonPath("$.weightKg").value(82.0));
    }

    // ---- Helpers ----

    private String validDietJson(String athleteId, int calories) {
        return """
                { "athleteId": "%s", "objective": "HYPERTROPHY", "calories": %d,
                  "protein": 180, "carbohydrates": 250, "fat": 80 }
                """.formatted(athleteId, calories);
    }

    private String versionJson(int calories, String reason) {
        return """
                { "objective": "HYPERTROPHY", "calories": %d, "protein": 180,
                  "carbohydrates": 250, "fat": 80, "changeReason": "%s" }
                """.formatted(calories, reason);
    }

    private String assessmentJson(String athleteId, String date, double weight) {
        // Use String.format with Locale.US to ensure dot as decimal separator
        String weightStr = String.format(java.util.Locale.US, "%.1f", weight);
        return """
                { "athleteId": "%s", "assessmentDate": "%s", "weightKg": %s,
                  "heightCm": 178.0, "bodyFatPercentage": 15.0, "muscleMassKg": 65.0,
                  "waistCm": 82.0, "hipCm": 96.0 }
                """.formatted(athleteId, date, weightStr);
    }

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

    private void addExercise(String coach, String trainingId, String name, int orderIndex) throws Exception {
        mockMvc.perform(post("/api/training/trainings/{id}/exercises", trainingId)
                        .header("Authorization", bearer(coach))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "exerciseName": "%s", "customExercise": true, "orderIndex": %d, "sets": 4,
                                  "repetitions": 10, "load": 60.0, "intensityType": "RPE", "intensityValue": 8.0,
                                  "restSeconds": 90 }
                                """.formatted(name, orderIndex)))
                .andExpect(status().isOk());
    }

    private String newVersion(String coach, String planId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/training/plans/{id}/versions", planId)
                        .header("Authorization", bearer(coach))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"newName\": \"Plano v2\", \"newDescription\": \"ajuste\" }"))
                .andExpect(status().isCreated()).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private void activatePlan(String coach, String planId) throws Exception {
        mockMvc.perform(post("/api/training/plans/{id}/transition", planId)
                        .header("Authorization", bearer(coach))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"targetStatus\": \"IN_REVIEW\" }"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/training/plans/{id}/transition", planId)
                        .header("Authorization", bearer(coach))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"targetStatus\": \"ACTIVE\" }"))
                .andExpect(status().isOk());
    }

    private String athleteToken(String athleteId) {
        return authService.registerAthleteCredential(
                        uniqueEmail("atleta"), "senha-Forte-123", UUID.fromString(athleteId))
                .tokens().accessToken();
    }
}