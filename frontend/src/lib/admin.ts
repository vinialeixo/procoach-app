import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "./api-client";
import { saveSession } from "./auth";

/**
 * ============================================================================
 * ADMIN — tipos, query keys e contratos da área administrativa (T-107 / T-108)
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
  inviteExpiresAt?: string | null;
};

export type AdminCoachDetail = AdminCoach & {
  pausedAthleteCount: number;
  finishedAthleteCount: number;
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

export type CoachInvitePayload = {
  token: string;
  expiresAt: string;
  emailSent: boolean;
};

export type CreateCoachPayload = {
  name: string;
  email: string;
};

export type CreateCoachResponse = {
  coachId: string;
  userId: string;
  name: string;
  email: string;
  status: CoachStatus;
  accessStatus: CoachAccessStatus;
  invite: CoachInvitePayload;
};

export type AdminCoachesFilters = {
  status?: CoachStatus;
  accessStatus?: CoachAccessStatus;
  search?: string;
  includeArchived?: boolean;
};

export type AdminCoachesResponse = {
  items: AdminCoach[];
  total: number;
};

export const adminKeys = {
  all: ["admin"] as const,
  summary: () => [...adminKeys.all, "summary"] as const,
  coaches: (filters?: AdminCoachesFilters) =>
    [...adminKeys.all, "coaches", filters ?? {}] as const,
  coach: (coachId: string) => [...adminKeys.all, "coaches", coachId] as const,
  coachAthletes: (coachId: string, status?: string) =>
    [...adminKeys.all, "coaches", coachId, "athletes", status ?? "ALL"] as const,
  admins: () => [...adminKeys.all, "admins"] as const,
};

export function coachInviteUrl(token: string): string {
  const origin =
    typeof window !== "undefined" ? window.location.origin : "http://localhost:5173";
  return `${origin}/convite-coach/${token}`;
}

async function fetchAdminSummary(): Promise<AdminSummary> {
  const { data } = await apiClient.get<AdminSummary>("/admin/summary");
  return data;
}

async function fetchAdminCoaches(
  filters?: AdminCoachesFilters
): Promise<AdminCoachesResponse> {
  const { data } = await apiClient.get<AdminCoachesResponse>("/admin/coaches", {
    params: {
      ...(filters?.status ? { status: filters.status } : {}),
      ...(filters?.accessStatus ? { accessStatus: filters.accessStatus } : {}),
      ...(filters?.search ? { search: filters.search } : {}),
      ...(filters?.includeArchived !== undefined
        ? { includeArchived: filters.includeArchived }
        : {}),
    },
  });
  return data;
}

async function createCoach(
  payload: CreateCoachPayload
): Promise<CreateCoachResponse> {
  const { data } = await apiClient.post<CreateCoachResponse>(
    "/admin/coaches",
    payload
  );
  return data;
}

async function resendCoachInvite(coachId: string): Promise<CoachInvitePayload> {
  const { data } = await apiClient.post<CoachInvitePayload>(
    `/admin/coaches/${coachId}/invites/resend`
  );
  return data;
}

export function useAdminSummary() {
  return useQuery({
    queryKey: adminKeys.summary(),
    queryFn: fetchAdminSummary,
  });
}

export function useAdminCoaches(filters?: AdminCoachesFilters) {
  return useQuery({
    queryKey: adminKeys.coaches(filters),
    queryFn: () => fetchAdminCoaches(filters),
  });
}

export function useCreateCoach() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createCoach,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: adminKeys.coaches() });
      void queryClient.invalidateQueries({ queryKey: adminKeys.summary() });
    },
  });
}

export function useResendCoachInvite() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: resendCoachInvite,
    onSuccess: (_data, coachId) => {
      void queryClient.invalidateQueries({ queryKey: adminKeys.coaches() });
      void queryClient.invalidateQueries({ queryKey: adminKeys.coach(coachId) });
      void queryClient.invalidateQueries({ queryKey: adminKeys.summary() });
    },
  });
}

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
