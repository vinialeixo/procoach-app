import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "./api-client";

/**
 * E1 — Gestão de Atletas (US-001, US-003, US-005, US-006).
 *
 * CONTRATO ASSUMIDO a partir do SPEC.md: os endpoints de atleta (T-018,
 * T-021) ainda não existem no backend. Os caminhos e formatos abaixo são
 * suposição — ajustar aqui quando o núcleo for implementado. O backend é a
 * fonte de verdade; o front nunca decide autorização (RN-01 / US-003: a API
 * só retorna atletas do coach logado).
 *
 * Nunca há exclusão física (US-006 / RF-06): status é o mecanismo de
 * soft-delete, e toda mudança de estado exige motivo (vai pro AuditLog).
 */

export type AthleteStatus = "ACTIVE" | "PAUSED" | "FINISHED";

export type Athlete = {
  id: string; // ID único e imutável (US-002) — exibido na ficha
  name: string;
  sex: "M" | "F" | "OTHER";
  birthDate: string; // ISO yyyy-mm-dd
  heightCm: number;
  weightKg: number;
  goal: string;
  status: AthleteStatus;
  createdAt: string;
};

export type CreateAthletePayload = {
  name: string;
  sex: "M" | "F" | "OTHER";
  birthDate: string;
  heightCm: number;
  weightKg: number;
  goal: string;
};

export type ChangeStatusPayload = {
  status: AthleteStatus;
  reason: string; // motivo obrigatório -> AuditLog (US-005, RF-42)
};

// --- Chamadas (contratos assumidos) ----------------------------------------

async function listAthletes(status?: AthleteStatus): Promise<Athlete[]> {
  const { data } = await apiClient.get("/athletes", {
    params: status ? { status } : undefined,
  });
  return data;
}

async function getAthlete(id: string): Promise<Athlete> {
  const { data } = await apiClient.get(`/athletes/${id}`);
  return data;
}

async function createAthlete(payload: CreateAthletePayload): Promise<Athlete> {
  const { data } = await apiClient.post("/athletes", payload);
  return data;
}

async function changeAthleteStatus(
  id: string,
  payload: ChangeStatusPayload
): Promise<Athlete> {
  const { data } = await apiClient.post(`/athletes/${id}/status`, payload);
  return data;
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
