import { useMutation, useQuery, useQueryClient, type QueryClient } from "@tanstack/react-query";
import { apiClient } from "./api-client";

/**
 * Coach training module (T-040).
 *
 * Interface for screens: athlete -> periodization -> plan version -> sessions
 * -> exercise prescriptions. HTTP paths, backend DTOs and query invalidation
 * stay here so pages do not need to know the transport contract.
 *
 * The training controllers are mounted under /api/training and /api/library,
 * while the rest of this frontend uses /api/v1. Keep the authenticated
 * apiClient adapter (including its 401 handling), overriding only its base URL
 * for these routes. VITE_API_BASE_URL may point either at the server root or
 * at its /api/v1 prefix.
 */

const configuredApiBase = import.meta.env.VITE_API_BASE_URL;
const apiRootBaseURL = configuredApiBase
  ? configuredApiBase.replace(/\/api\/v1\/?$/, "") || "/"
  : "/";

export const MUSCLE_GROUPS = [
  "PEITO",
  "COSTAS",
  "OMBROS",
  "BICEPS",
  "TRICEPS",
  "ANTEBRACO",
  "QUADRICEPS",
  "POSTERIOR_DE_COXA",
  "GLUTEOS",
  "ADUTORES",
  "ABDUTORES",
  "PANTURRILHA",
  "ABDOMEN",
  "LOMBAR",
  "TRAPECIO",
  "FOREARM",
] as const;

export type MuscleGroup = (typeof MUSCLE_GROUPS)[number];
export type TrainingStatus = "DRAFT" | "IN_REVIEW" | "ACTIVE" | "FINISHED";
export type IntensityType = "PERCENT_1RM" | "RPE" | "RIR" | "ABSOLUTE_LOAD";

export type ExerciseReference = {
  authors: string;
  title: string;
  source: string;
  year: number | null;
};

export type Exercise = {
  id: string;
  name: string;
  muscleGroups: MuscleGroup[];
  equipment: string;
  execution: string;
  commonErrors: string[];
  progressions: string[];
  regressions: string[];
  contraindications: string[];
  references: ExerciseReference[];
  version: number;
  superseded: boolean;
};

export type Periodization = {
  id: string;
  athleteId: string;
  name: string;
  description: string | null;
  startDate: string | null;
  endDate: string | null;
  status: string;
  deleted: boolean;
};

export type TrainingPlan = {
  id: string;
  athleteId: string;
  periodizationId: string;
  name: string;
  description: string | null;
  version: number;
  previousVersionId: string | null;
  status: TrainingStatus;
  deleted: boolean;
};

export type ExercisePrescription = {
  id: string;
  exerciseId: string | null;
  exerciseName: string;
  customExercise: boolean;
  orderIndex: number;
  sets: number;
  repetitions: number;
  load: number;
  intensityType: IntensityType;
  intensityValue: number | null;
  restSeconds: number;
  notes: string | null;
  exerciseVersion: number | null;
  newerVersionAvailable: boolean;
  currentExerciseId: string | null;
};

export type TrainingSession = {
  id: string;
  periodizationId: string;
  planId: string | null;
  athleteId: string;
  name: string;
  description: string | null;
  weekNumber: number;
  dayOfWeek: number;
  status: TrainingStatus;
  exercises: ExercisePrescription[];
  deleted: boolean;
};

type CreatePeriodizationPayload = {
  athleteId: string;
  name: string;
  description: string;
  startDate: string;
  endDate: string;
};

type CreatePlanPayload = {
  athleteId: string;
  periodizationId: string;
  name: string;
  description: string;
};

type CreateSessionPayload = {
  athleteId: string;
  periodizationId: string;
  planId: string;
  name: string;
  description: string;
  weekNumber: number;
  dayOfWeek: number;
};

export type AddExercisePayload = {
  exerciseId: string;
  orderIndex: number;
  sets: number;
  repetitions: number;
  load: number;
  intensityType: IntensityType;
  intensityValue: number | null;
  restSeconds: number;
};

/** Corpo do PUT /trainings/{id}/exercises/{prescriptionId} (T-068/T-073). */
export type UpdateExercisePrescriptionPayload = {
  exerciseId: string | null;
  exerciseName: string;
  sets: number;
  repetitions: number;
  load: number;
  intensityType: IntensityType;
  intensityValue: number;
  restSeconds: number;
  notes: string | null;
};

/**
 * Converte a prescrição atual no corpo de edição. Quando `currentExercise` é
 * informado, a prescrição passa a apontar para a versão corrente da biblioteca
 * (T-069) preservando séries, repetições, carga, intensidade e descanso.
 */
export function prescriptionUpdatePayload(
  prescription: ExercisePrescription,
  currentExercise?: Pick<Exercise, "id" | "name">
): UpdateExercisePrescriptionPayload {
  if (prescription.intensityValue === null) {
    throw new Error(
      "Preencha o valor da intensidade antes de trocar a versão do exercício."
    );
  }
  return {
    exerciseId: currentExercise?.id ?? prescription.exerciseId,
    exerciseName: currentExercise?.name ?? prescription.exerciseName,
    sets: prescription.sets,
    repetitions: prescription.repetitions,
    load: prescription.load,
    intensityType: prescription.intensityType,
    intensityValue: prescription.intensityValue,
    restSeconds: prescription.restSeconds,
    notes: prescription.notes,
  };
}

const keys = {
  periodizations: (athleteId: string) => ["training", "periodizations", athleteId],
  plans: (athleteId: string) => ["training", "plans", athleteId],
  sessions: (periodizationId: string) => ["training", "sessions", periodizationId],
  athleteSessions: (athleteId: string, planId: string) =>
    ["training", "athlete-sessions", athleteId, planId],
  exercises: (name: string, muscleGroup: MuscleGroup | "") =>
    ["training", "exercises", name, muscleGroup],
};

function rootRequestConfig() {
  return { baseURL: apiRootBaseURL };
}

async function listExercises(
  name: string,
  muscleGroup: MuscleGroup | ""
): Promise<Exercise[]> {
  const { data } = await apiClient.get<Exercise[]>("/api/library/exercises", {
    ...rootRequestConfig(),
    params: { name: name || undefined, muscleGroup: muscleGroup || undefined },
  });
  return data.filter((exercise) => !exercise.superseded);
}

async function listPeriodizations(athleteId: string): Promise<Periodization[]> {
  const { data } = await apiClient.get<Periodization[]>(
    `/api/training/periodizations/athlete/${athleteId}`,
    rootRequestConfig()
  );
  return data.filter((periodization) => !periodization.deleted);
}

async function listPlans(athleteId: string): Promise<TrainingPlan[]> {
  const { data } = await apiClient.get<TrainingPlan[]>(
    `/api/training/plans/athlete/${athleteId}`,
    rootRequestConfig()
  );
  return data.filter((plan) => !plan.deleted);
}

async function listSessions(periodizationId: string): Promise<TrainingSession[]> {
  const { data } = await apiClient.get<TrainingSession[]>(
    `/api/training/trainings/periodization/${periodizationId}`,
    rootRequestConfig()
  );
  return data.filter((session) => !session.deleted);
}

async function listAthleteSessions(
  athleteId: string,
  planId: string
): Promise<TrainingSession[]> {
  const { data } = await apiClient.get<TrainingSession[]>(
    `/api/training/trainings/athlete/${athleteId}`,
    { ...rootRequestConfig(), params: { planId } }
  );
  return data.filter((session) => !session.deleted);
}

export function useExercises(
  name: string,
  muscleGroup: MuscleGroup | "",
  enabled = true
) {
  return useQuery({
    queryKey: keys.exercises(name, muscleGroup),
    queryFn: () => listExercises(name, muscleGroup),
    enabled,
  });
}

export function usePeriodizations(athleteId: string) {
  return useQuery({
    queryKey: keys.periodizations(athleteId),
    queryFn: () => listPeriodizations(athleteId),
    enabled: Boolean(athleteId),
  });
}

export function useTrainingPlans(athleteId: string) {
  return useQuery({
    queryKey: keys.plans(athleteId),
    queryFn: () => listPlans(athleteId),
    enabled: Boolean(athleteId),
  });
}

export function useTrainingSessions(periodizationId: string) {
  return useQuery({
    queryKey: keys.sessions(periodizationId),
    queryFn: () => listSessions(periodizationId),
    enabled: Boolean(periodizationId),
  });
}

export function useAthleteTrainingSessions(athleteId: string, planId: string) {
  return useQuery({
    queryKey: keys.athleteSessions(athleteId, planId),
    queryFn: () => listAthleteSessions(athleteId, planId),
    enabled: Boolean(athleteId && planId),
  });
}

export function useCreatePeriodization(athleteId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: Omit<CreatePeriodizationPayload, "athleteId">) => {
      const { data } = await apiClient.post<Periodization>(
        "/api/training/periodizations",
        { ...payload, athleteId },
        rootRequestConfig()
      );
      return data;
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: keys.periodizations(athleteId) }),
  });
}

export function useCreateTrainingPlan(athleteId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: Omit<CreatePlanPayload, "athleteId">) => {
      const { data } = await apiClient.post<TrainingPlan>(
        "/api/training/plans",
        { ...payload, athleteId },
        rootRequestConfig()
      );
      return data;
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: keys.plans(athleteId) }),
  });
}

export function useCreateTrainingSession(athleteId: string, periodizationId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: Omit<CreateSessionPayload, "athleteId" | "periodizationId">) => {
      const { data } = await apiClient.post<TrainingSession>(
        "/api/training/trainings",
        { ...payload, athleteId, periodizationId },
        rootRequestConfig()
      );
      return data;
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: keys.sessions(periodizationId) }),
  });
}

export function useAddExerciseToSession(periodizationId: string, sessionId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: AddExercisePayload) => {
      const { data } = await apiClient.post<TrainingSession>(
        `/api/training/trainings/${sessionId}/exercises`,
        {
          ...payload,
          exerciseName: null,
          customExercise: false,
          notes: null,
        },
        rootRequestConfig()
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: keys.sessions(periodizationId) });
    },
  });
}

export function useReorderSessionExercises(periodizationId: string, sessionId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (orderedPrescriptionIds: string[]) => {
      const { data } = await apiClient.put<TrainingSession>(
        `/api/training/trainings/${sessionId}/exercises/reorder`,
        { orderedPrescriptionIds },
        rootRequestConfig()
      );
      return data;
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: keys.sessions(periodizationId) }),
  });
}

async function updatePrescription(
  sessionId: string,
  prescriptionId: string,
  payload: UpdateExercisePrescriptionPayload
): Promise<TrainingSession> {
  const { data } = await apiClient.put<TrainingSession>(
    `/api/training/trainings/${sessionId}/exercises/${prescriptionId}`,
    payload,
    rootRequestConfig()
  );
  return data;
}

async function removePrescription(
  sessionId: string,
  prescriptionId: string
): Promise<TrainingSession> {
  const { data } = await apiClient.delete<TrainingSession>(
    `/api/training/trainings/${sessionId}/exercises/${prescriptionId}`,
    rootRequestConfig()
  );
  return data;
}

function invalidateSessionCaches(
  queryClient: QueryClient,
  periodizationId: string
) {
  queryClient.invalidateQueries({ queryKey: keys.sessions(periodizationId) });
  queryClient.invalidateQueries({ queryKey: ["training", "athlete-sessions"] });
}

/** T-073 — edita uma prescrição já criada, inclusive em treino ativo. */
export function useUpdateExercisePrescription(
  periodizationId: string,
  sessionId: string
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      prescriptionId,
      payload,
    }: {
      prescriptionId: string;
      payload: UpdateExercisePrescriptionPayload;
    }) => updatePrescription(sessionId, prescriptionId, payload),
    onSuccess: () => invalidateSessionCaches(queryClient, periodizationId),
  });
}

/** T-073 — remove uma prescrição do treino (inclusive ativo). */
export function useRemoveExercisePrescription(
  periodizationId: string,
  sessionId: string
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (prescriptionId: string) =>
      removePrescription(sessionId, prescriptionId),
    onSuccess: () => invalidateSessionCaches(queryClient, periodizationId),
  });
}

/**
 * T-073 — troca a prescrição pela versão corrente do exercício da biblioteca.
 * Busca o exercício corrente (T-069) e reenvia a prescrição com o novo
 * `exerciseId`/nome, mantendo os demais parâmetros.
 */
export function useApplyCurrentExerciseVersion(
  periodizationId: string,
  sessionId: string
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (prescription: ExercisePrescription) => {
      if (!prescription.currentExerciseId) {
        throw new Error("Este exercício não tem uma versão mais nova.");
      }
      const { data: currentExercise } = await apiClient.get<Exercise>(
        `/api/library/exercises/${prescription.currentExerciseId}`,
        rootRequestConfig()
      );
      return updatePrescription(
        sessionId,
        prescription.id,
        prescriptionUpdatePayload(prescription, currentExercise)
      );
    },
    onSuccess: () => invalidateSessionCaches(queryClient, periodizationId),
  });
}

export function useTransitionTrainingPlan(athleteId: string, periodizationId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ planId, targetStatus }: { planId: string; targetStatus: TrainingStatus }) => {
      const { data } = await apiClient.post<TrainingPlan>(
        `/api/training/plans/${planId}/transition`,
        { targetStatus },
        rootRequestConfig()
      );
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: keys.plans(athleteId) });
      queryClient.invalidateQueries({ queryKey: keys.sessions(periodizationId) });
    },
  });
}

export function useTransitionTrainingSession(
  periodizationId: string,
  sessionId: string
) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (targetStatus: TrainingStatus) => {
      const { data } = await apiClient.post<TrainingSession>(
        `/api/training/trainings/${sessionId}/transition`,
        { targetStatus },
        rootRequestConfig()
      );
      return data;
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: keys.sessions(periodizationId) }),
  });
}
