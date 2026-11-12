import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "./api-client";

/**
 * E1 — Gestão de Atletas (US-001, US-003, US-005, US-006).
 *
 * Contrato alinhado ao backend (AthleteController, /api/v1/athletes): o back
 * usa height/weight e enums em inglês (Sex, Goal). Este módulo traduz para o
 * formato da UI (heightCm/weightKg) nas bordas. O backend é a fonte de
 * verdade; o front nunca decide autorização (RN-01 / US-003: a API só
 * retorna atletas do coach logado).
 *
 * Nunca há exclusão física (US-006 / RF-06): status é o mecanismo de
 * soft-delete, e toda mudança de estado exige motivo (vai pro AuditLog).
 */

export type AthleteStatus = "ACTIVE" | "PAUSED" | "FINISHED";

export const SEXES = ["MALE", "FEMALE"] as const;
export type Sex = (typeof SEXES)[number];

export const GOALS = [
  "CUTTING",
  "HYPERTROPHY",
  "MAINTENANCE",
  "BULKING",
  "RECOMPOSITION",
] as const;
export type Goal = (typeof GOALS)[number];

export const SEX_LABEL: Record<Sex, string> = {
  MALE: "Masculino",
  FEMALE: "Feminino",
};

export const GOAL_LABEL: Record<Goal, string> = {
  CUTTING: "Cutting",
  HYPERTROPHY: "Hipertrofia",
  MAINTENANCE: "Manutenção",
  BULKING: "Bulking",
  RECOMPOSITION: "Recomposição",
};

/** Convite pendente retornado pelo backend (AthleteResponse.pendingInvite, T-077). */
export type PendingInviteSummary = {
  id: string;
  email: string;
  expiresAt: string;
};

/**
 * Estado de acesso do atleta ao app (T-082 / Issue #98):
 * - HAS_ACCESS: já possui credencial criada (hasCredential = true).
 * - INVITE_PENDING: convite ativo e aguardando aceite (pendingInvite != null).
 * - NO_ACCESS: sem credencial e sem convite pendente.
 */
export type AthleteAccessStatus = "HAS_ACCESS" | "INVITE_PENDING" | "NO_ACCESS";

export const ATHLETE_ACCESS_LABEL: Record<AthleteAccessStatus, string> = {
  HAS_ACCESS: "Tem acesso",
  INVITE_PENDING: "Convite pendente",
  NO_ACCESS: "Sem acesso",
};

export function getAthleteAccessStatus(
  athlete: Pick<Athlete, "hasCredential" | "pendingInvite">
): AthleteAccessStatus {
  if (athlete.hasCredential) {
    return "HAS_ACCESS";
  }
  if (athlete.pendingInvite) {
    return "INVITE_PENDING";
  }
  return "NO_ACCESS";
}

export type Athlete = {
  id: string; // ID único e imutável (US-002) — exibido na ficha
  name: string;
  email?: string | null;
  sex: Sex;
  birthDate: string; // ISO yyyy-mm-dd
  heightCm: number;
  weightKg: number;
  goal: Goal;
  status: AthleteStatus;
  createdAt: string;
  hasCredential?: boolean;
  pendingInvite?: PendingInviteSummary | null;
};

export type CreateAthletePayload = {
  name: string;
  email?: string;
  sex: Sex;
  birthDate: string;
  heightCm: number;
  weightKg: number;
  goal: Goal;
};

export type ChangeStatusPayload = {
  status: AthleteStatus;
  reason: string; // motivo obrigatório (US-005, RF-42)
};

// Formato do backend (AthleteResponse / T-077 / T-078).
type ApiAthlete = {
  id: string;
  name: string;
  email?: string | null;
  sex: Sex;
  birthDate: string;
  height: number;
  weight: number;
  goal: Goal;
  status: AthleteStatus;
  createdAt: string;
  hasCredential?: boolean;
  pendingInvite?: PendingInviteSummary | null;
};

function toAthlete(a: ApiAthlete): Athlete {
  return {
    id: a.id,
    name: a.name,
    email: a.email ?? null,
    sex: a.sex,
    birthDate: a.birthDate,
    heightCm: a.height,
    weightKg: a.weight,
    goal: a.goal,
    status: a.status,
    createdAt: a.createdAt,
    hasCredential: Boolean(a.hasCredential),
    pendingInvite: a.pendingInvite ?? null,
  };
}

// --- Chamadas ----------------------------------------------------------------

async function listAthletes(status?: AthleteStatus): Promise<Athlete[]> {
  const { data } = await apiClient.get<ApiAthlete[]>("/athletes", {
    params: status ? { status } : undefined,
  });
  return data.map(toAthlete);
}

async function getAthlete(id: string): Promise<Athlete> {
  const { data } = await apiClient.get<ApiAthlete>(`/athletes/${id}`);
  return toAthlete(data);
}

async function createAthlete(payload: CreateAthletePayload): Promise<Athlete> {
  const { heightCm, weightKg, email, ...rest } = payload;
  const cleanEmail = email?.trim();
  const { data } = await apiClient.post<ApiAthlete>("/athletes", {
    ...rest,
    ...(cleanEmail ? { email: cleanEmail } : {}),
    height: heightCm,
    weight: weightKg,
  });
  return toAthlete(data);
}

async function changeAthleteStatus(
  id: string,
  payload: ChangeStatusPayload
): Promise<Athlete> {
  const { data } = await apiClient.post<ApiAthlete>(`/athletes/${id}/status`, payload);
  return toAthlete(data);
}

// --- Hooks -----------------------------------------------------------------

export function useAthletes(status?: AthleteStatus) {
  return useQuery({
    queryKey: ["athletes", status ?? "all"],
    queryFn: () => listAthletes(status),
  });
}

export function useAthlete(id: string) {
  return useQuery({
    queryKey: ["athletes", id],
    queryFn: () => getAthlete(id),
    enabled: Boolean(id),
  });
}

export function useCreateAthlete() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createAthlete,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["athletes"] });
    },
  });
}

export function useChangeAthleteStatus(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: ChangeStatusPayload) => changeAthleteStatus(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["athletes"] });
    },
  });
}
