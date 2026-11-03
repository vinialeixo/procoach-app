import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "./api-client";

/**
 * E2 — Avaliação Física (US-007, US-008, US-009, US-010).
 *
 * Contrato alinhado ao backend (AssessmentController, /api/v1/assessments):
 * a listagem e a criação usam athleteId como query/corpo, e a resposta é
 * plana, com `metrics` (IMC, TMB, GET e evolução de peso) calculado pelo núcleo
 * (T-025; o GET usa fator de atividade sedentário 1,2 até haver o dado por atleta). Os nomes de campos espelham o
 * agregado de domínio (com.fitness.procoach.assessment: Assessment +
 * BodyMeasurements, T-049).
 *
 * Regras do domínio respeitadas pelo front:
 * - assessmentDate é obrigatória (US-007) — validada no form antes de salvar;
 * - avaliações nunca são sobrescritas (US-008 / regra inegociável 3): este
 *   módulo só expõe listagem e criação — não existe PUT/PATCH/DELETE aqui;
 * - IMC, TMB, GET e evolução de peso (US-010, T-025) são calculados pelo
 *   backend e vêm prontos em `metrics` — o front não recalcula, a fonte de
 *   verdade é o núcleo.
 */

export type BodyMeasurementsPayload = {
  weightKg: number;
  heightCm: number;
  bodyFatPercentage?: number;
  muscleMassKg?: number;
  waistCm?: number;
  hipCm?: number;
};

// Métricas derivadas (US-010 / T-025). weightChange* é em relação à avaliação
// anterior do mesmo atleta; null na primeira avaliação.
export type AssessmentMetrics = {
  imc: number | null;
  tmbKcal: number | null;
  getKcal: number | null;
  weightChangeKg: number | null;
  weightChangePercent: number | null;
};

export type Assessment = {
  id: string;
  athleteId: string;
  assessmentDate: string; // ISO yyyy-mm-dd
  measurements: {
    weightKg: number;
    heightCm: number;
    bodyFatPercentage: number | null;
    muscleMassKg: number | null;
    waistCm: number | null;
    hipCm: number | null;
  };
  metrics: AssessmentMetrics;
  createdAt: string;
};

export type CreateAssessmentPayload = {
  assessmentDate: string;
  measurements: BodyMeasurementsPayload;
};

// Formato do backend (AssessmentResponse).
type ApiAssessment = {
  id: string;
  athleteId: string;
  assessmentDate: string;
  weightKg: number;
  heightCm: number;
  bodyFatPercentage: number | null;
  muscleMassKg: number | null;
  waistCm: number | null;
  hipCm: number | null;
  createdAt: string;
  metrics: AssessmentMetrics; // T-025: calculado pelo núcleo
};

function toAssessment(a: ApiAssessment): Assessment {
  return {
    id: a.id,
    athleteId: a.athleteId,
    assessmentDate: a.assessmentDate,
    measurements: {
      weightKg: a.weightKg,
      heightCm: a.heightCm,
      bodyFatPercentage: a.bodyFatPercentage,
      muscleMassKg: a.muscleMassKg,
      waistCm: a.waistCm,
      hipCm: a.hipCm,
    },
    metrics: a.metrics,
    createdAt: a.createdAt,
  };
}

// --- Chamadas ----------------------------------------------------------------

async function listAssessments(athleteId: string): Promise<Assessment[]> {
  const { data } = await apiClient.get<ApiAssessment[]>("/assessments", {
    params: { athleteId },
  });
  return data.map(toAssessment);
}

async function createAssessment(
  athleteId: string,
  payload: CreateAssessmentPayload
): Promise<Assessment> {
  const { data } = await apiClient.post<ApiAssessment>("/assessments", {
    athleteId,
    assessmentDate: payload.assessmentDate,
    ...payload.measurements,
  });
  return toAssessment(data);
}

// --- Hooks -----------------------------------------------------------------

export function useAssessments(athleteId: string) {
  return useQuery({
    queryKey: ["assessments", athleteId],
    queryFn: () => listAssessments(athleteId),
    enabled: Boolean(athleteId),
  });
}

export function useCreateAssessment(athleteId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateAssessmentPayload) =>
      createAssessment(athleteId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["assessments", athleteId] });
      // A nova avaliação pode atualizar peso/altura exibidos na ficha.
      queryClient.invalidateQueries({ queryKey: ["athletes"] });
    },
  });
}
