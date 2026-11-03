import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "./api-client";

/**
 * E3 — Dietas versionadas (US-011, US-014, US-015, US-016, US-017).
 *
 * Contrato alinhado ao backend (DietController, /api/v1/diets): a criação
 * recebe athleteId no corpo, a listagem usa /diets/history?athleteId= e a
 * resposta traz os macros planos (calories/protein/...). Este módulo traduz
 * para o formato da UI (`macros` aninhado) nas bordas.
 * Os nomes de campos espelham o agregado de domínio
 * (com.fitness.procoach.nutrition: Diet + Macronutrients, T-050) e as regras
 * que ele impõe:
 *
 * - nunca sobrescrever (US-015 / regra inegociável 3): este módulo só expõe
 *   criação da v1 e criação de nova versão a partir de uma versão existente;
 *   não existe PUT/PATCH/DELETE aqui. No domínio, criar nova versão congela
 *   a anterior (readOnly) e exige changeReason;
 * - fluxo de status travado (US-017): DRAFT -> IN_REVIEW -> APPROVED ->
 *   ARCHIVED (terminal). DIET_STATUS_FLOW espelha a máquina de estados do
 *   domínio — a UI só oferece a transição seguinte e bloqueia as demais;
 * - auditoria (US-016): cada versão traz quem criou (createdBy), quando
 *   (createdAt) e o motivo (changeReason). createdBy/updatedBy são UUIDs de
 *   usuário: o núcleo não expõe nomes — a tela exibe o ID (mono).
 *
 * O domínio guarda apenas os totais de macronutrientes — sem refeições ou
 * alimentos por enquanto.
 */

export const DIET_OBJECTIVES = [
  "CUTTING",
  "HYPERTROPHY",
  "MAINTENANCE",
  "BULKING",
  "WEIGHT_LOSS",
] as const;

export type DietObjective = (typeof DIET_OBJECTIVES)[number];

export type DietStatus = "DRAFT" | "IN_REVIEW" | "APPROVED" | "ARCHIVED";

export type Macronutrients = {
  calories: number; // kcal
  protein: number; // g
  carbohydrates: number; // g
  fat: number; // g
};

// Uma linha = uma versão da dieta (v1, v2, ...). No domínio cada versão é
// uma instância do agregado Diet encadeada por previousVersionId.
export type DietVersion = {
  id: string;
  coachId: string; // UUID do coach (tenant) — espelho de CoachId
  athleteId: string; // UUID do atleta — espelho de AthleteId
  objective: DietObjective;
  macros: Macronutrients;
  status: DietStatus;
  version: number; // v1 = 1
  previousVersionId: string | null; // null na v1
  changeReason: string | null; // motivo da alteração (auditoria, US-016); null na v1
  createdBy: string; // UUID de quem criou ESTA versão
  updatedBy: string; // UUID do último usuário que tocou o registro
  createdAt: string; // ISO-8601 (Instant do domínio)
  updatedAt: string; // ISO-8601 (Instant do domínio)
  readOnly: boolean; // versão anterior já congelada — imutável
  deleted: boolean; // soft-delete (regra inegociável 5) — nunca renderizar
};

export type CreateDietPayload = {
  objective: DietObjective;
  macros: Macronutrients;
};

export type CreateDietVersionPayload = CreateDietPayload & {
  changeReason: string; // obrigatório no domínio (auditoria, US-016)
};

export type ChangeDietStatusPayload = {
  status: DietStatus;
};

// Espelho client-side da máquina de estados do domínio (US-017, critério 2:
// transições inválidas bloqueadas). Sempre oferecer no máximo a PRÓXIMA
// transição; ARCHIVED é terminal.
export const DIET_STATUS_FLOW: Record<DietStatus, DietStatus | null> = {
  DRAFT: "IN_REVIEW",
  IN_REVIEW: "APPROVED",
  APPROVED: "ARCHIVED",
  ARCHIVED: null,
};

export const OBJECTIVE_LABEL: Record<DietObjective, string> = {
  CUTTING: "Cutting",
  HYPERTROPHY: "Hipertrofia",
  MAINTENANCE: "Manutenção",
  BULKING: "Bulking",
  WEIGHT_LOSS: "Perda de peso",
};

export const STATUS_LABEL: Record<DietStatus, string> = {
  DRAFT: "Rascunho",
  IN_REVIEW: "Em revisão",
  APPROVED: "Aprovada",
  ARCHIVED: "Arquivada",
};

// Formato do backend (DietResponse).
type ApiDiet = {
  id: string;
  coachId: string;
  athleteId: string;
  objective: DietObjective;
  calories: number;
  protein: number;
  carbohydrates: number;
  fat: number;
  status: DietStatus;
  version: number;
  previousVersionId: string | null;
  changeReason: string | null;
  createdAt: string;
  updatedAt: string;
  readOnly: boolean;
  deleted: boolean;
  createdBy: string;
  updatedBy: string;
};

function toDietVersion(d: ApiDiet): DietVersion {
  const { calories, protein, carbohydrates, fat, ...rest } = d;
  return { ...rest, macros: { calories, protein, carbohydrates, fat } };
}

function toApiBody(payload: CreateDietPayload) {
  return { objective: payload.objective, ...payload.macros };
}

// --- Chamadas ----------------------------------------------------------------

async function listDiets(athleteId: string): Promise<DietVersion[]> {
  const { data } = await apiClient.get<ApiDiet[]>("/diets/history", {
    params: { athleteId },
  });
  return data.map(toDietVersion);
}

async function createDiet(
  athleteId: string,
  payload: CreateDietPayload
): Promise<DietVersion> {
  const { data } = await apiClient.post<ApiDiet>("/diets", {
    athleteId,
    ...toApiBody(payload),
  });
  return toDietVersion(data);
}

// Cria a próxima versão a partir de `versionId`; o backend congela a versão
// de origem (readOnly). Nunca é um update — é um POST de nova versão.
async function createDietVersion(
  _athleteId: string,
  versionId: string,
  payload: CreateDietVersionPayload
): Promise<DietVersion> {
  const { data } = await apiClient.post<ApiDiet>(`/diets/${versionId}/versions`, {
    ...toApiBody(payload),
    changeReason: payload.changeReason,
  });
  return toDietVersion(data);
}

async function changeDietStatus(
  _athleteId: string,
  versionId: string,
  payload: ChangeDietStatusPayload
): Promise<DietVersion> {
  const { data } = await apiClient.post<ApiDiet>(`/diets/${versionId}/status`, payload);
  return toDietVersion(data);
}

// --- Balanço energético (T-030) ---------------------------------------------

export type EnergyClassification = "DEFICIT" | "MAINTENANCE" | "SURPLUS";

// Calorias da dieta atual contra o GET da avaliação mais recente. Calculado
// pelo núcleo (GET /diets/energy-balance?athleteId=) — o front só exibe.
export type EnergyBalance = {
  dietId: string;
  calories: number;
  tmbKcal: number;
  getKcal: number;
  balanceKcal: number; // calorias da dieta - GET; negativo = déficit
  balancePercent: number;
  classification: EnergyClassification;
  basedOnAssessmentDate: string; // ISO yyyy-mm-dd
  weightKg: number;
};

export const CLASSIFICATION_LABEL: Record<EnergyClassification, string> = {
  DEFICIT: "Déficit",
  MAINTENANCE: "Manutenção",
  SURPLUS: "Superávit",
};

async function getEnergyBalance(athleteId: string): Promise<EnergyBalance> {
  const { data } = await apiClient.get<EnergyBalance>("/diets/energy-balance", {
    params: { athleteId },
  });
  return data;
}

// Versão corrente da cadeia: a única que o domínio permite mutar (não está
// readOnly nem deleted). Na prática é sempre a de maior número — v(n) vira
// readOnly assim que v(n+1) é criada a partir dela.
export function findCurrentDietVersion(
  versions: DietVersion[]
): DietVersion | undefined {
  return [...versions]
    .sort((a, b) => b.version - a.version)
    .find((v) => !v.readOnly && !v.deleted);
}

// --- Hooks -----------------------------------------------------------------

export function useDiets(athleteId: string) {
  return useQuery({
    queryKey: ["diets", athleteId],
    queryFn: () => listDiets(athleteId),
    enabled: Boolean(athleteId),
  });
}

export function useCreateDiet(athleteId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateDietPayload) => createDiet(athleteId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["diets", athleteId] });
    },
  });
}

export function useCreateDietVersion(athleteId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      versionId,
      payload,
    }: {
      versionId: string;
      payload: CreateDietVersionPayload;
    }) => createDietVersion(athleteId, versionId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["diets", athleteId] });
    },
  });
}

export function useChangeDietStatus(athleteId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      versionId,
      payload,
    }: {
      versionId: string;
      payload: ChangeDietStatusPayload;
    }) => changeDietStatus(athleteId, versionId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["diets", athleteId] });
    },
  });
}

// 404 (atleta sem dieta) e 409 (sem avaliação) são estados esperados da tela,
// não falhas transitórias: sem retry.
export function useEnergyBalance(athleteId: string) {
  return useQuery({
    queryKey: ["diets", "energy-balance", athleteId],
    queryFn: () => getEnergyBalance(athleteId),
    enabled: Boolean(athleteId),
    retry: false,
  });
}
