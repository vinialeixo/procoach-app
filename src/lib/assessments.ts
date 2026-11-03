import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "./api-client";

/**
 * E2 — Avaliação Física (US-007, US-008, US-009, US-010).
 *
 * CONTRATO ASSUMIDO: os endpoints REST de avaliação (T-024) e o motor de
 * cálculos (T-025) ainda não existem no backend — os caminhos e formatos
 * abaixo são suposição; ajustar aqui quando o núcleo for implementado.
 * Os nomes de campos espelham o agregado de domínio que já existe
 * (com.fitness.procoach.assessment: Assessment + BodyMeasurements, T-049).
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
  imc: number;
  tmbKcal: number;
  getKcal: number;
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

// --- Chamadas (contratos assumidos) ----------------------------------------

async function listAssessments(athleteId: string): Promise<Assessment[]> {
  const { data } = await apiClient.get(`/athletes/${athleteId}/assessments`);
  return data;
}

async function createAssessment(
  athleteId: string,
  payload: CreateAssessmentPayload
): Promise<Assessment> {
  const { data } = await apiClient.post(
    `/athletes/${athleteId}/assessments`,
    payload
  );
  return data;
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
