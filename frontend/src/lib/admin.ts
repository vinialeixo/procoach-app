import { useMutation } from "@tanstack/react-query";
import { apiClient } from "./api-client";
import { saveSession } from "./auth";

/**
 * ============================================================================
 * ADMIN — tipos, query keys e contratos da área administrativa (T-107)
 * ============================================================================
 *
 * Contratos baseados em `docs/plans/painel-admin-coaches.md` (seções 5 e 8.2).
 * Os endpoints de backend serão entregues em T-103..T-106; enquanto isso,
 * assumimos os payloads documentados no plano.
 */

export type CoachStatus = "ACTIVE" | "SUSPENDED" | "CANCELLED";
export type CoachAccessStatus = "ACTIVE" | "INVITE_PENDING" | "DISABLED";

export type AdminCoach = {
  coachId: string;
  name: string;
  email: string;
  status: CoachStatus;
  accessStatus: CoachAccessStatus;
  athleteCount: number;
  activeAthleteCount: number;
  createdAt: string;
};

export type AdminCoachDetail = AdminCoach & {
  pausedAthleteCount: number;
  finishedAthleteCount: number;
  inviteExpiresAt?: string | null;
};

export type AdminAthlete = {
  athleteId: string;
  name: string;
  status: "ACTIVE" | "PAUSED" | "FINISHED";
  goal: string;
  accessStatus: "HAS_ACCESS" | "INVITE_PENDING" | "NO_ACCESS";
  createdAt: string;
};

export type AdminSummary = {
  totalCoaches: number;
  activeCoaches: number;
  suspendedCoaches: number;
  cancelledCoaches: number;
  pendingInvites: number;
  activeAthletes: number;
  adminCount: number;
};

export type AdminAccount = {
  id: string;
  username: string;
  email: string;
  enabled: boolean;
  mustChangePassword: boolean;
  createdAt: string;
};

export const adminKeys = {
  all: ["admin"] as const,
  summary: () => [...adminKeys.all, "summary"] as const,
  coaches: (filters?: {
    status?: CoachStatus;
    accessStatus?: CoachAccessStatus;
    search?: string;
    includeArchived?: boolean;
  }) => [...adminKeys.all, "coaches", filters ?? {}] as const,
  coach: (coachId: string) => [...adminKeys.all, "coaches", coachId] as const,
  coachAthletes: (coachId: string, status?: string) =>
    [...adminKeys.all, "coaches", coachId, "athletes", status ?? "ALL"] as const,
  admins: () => [...adminKeys.all, "admins"] as const,
};

export type ChangeOwnPasswordPayload = {
  currentPassword: string;
  newPassword: string;
};

async function changeOwnPassword(payload: ChangeOwnPasswordPayload): Promise<void> {
  // Suposição de contrato (T-106): PUT /api/v1/admin/me/password devolve novos tokens
  // com mustChangePassword=false para liberar o painel imediatamente.
  const { data } = await apiClient.put("/admin/me/password", payload);
  if (data?.accessToken) {
    saveSession(data.accessToken, data.refreshToken);
  }
}

export function useChangeOwnPassword() {
  return useMutation({ mutationFn: changeOwnPassword });
}
