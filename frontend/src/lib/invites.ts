import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "./api-client";
import { saveSession } from "./auth";

/**
 * Convite do atleta (primeira credencial) — contrato do backend
 * (AthleteInviteController, /api/v1/auth/invites e T-077 / T-079):
 *
 *   POST /auth/invites                         -> coach gera o convite (Bearer, COACH)
 *   GET  /auth/invites/{token}                 -> público: nome do atleta + e-mail
 *   POST /auth/invites/{token}/accept          -> público: define a senha e abre a sessão
 *   GET  /athletes/{athleteId}/invites         -> lista de convites do atleta (T-077)
 *   POST /athletes/{athleteId}/invites/{id}/revoke -> revoga convite pendente (T-077)
 *
 * O convite vale 7 dias e uma única vez; o token só aparece na resposta da
 * criação. Não há envio de e-mail: o coach copia o link e repassa ao atleta.
 * Convite inexistente, expirado, revogado ou já usado responde 404 genérico.
 */

export type CreateInvitePayload = {
  athleteId: string;
  email: string;
};

export type Invite = {
  athleteId: string;
  email: string;
  token: string;
  invitePath: string; // rota do front, ex.: /convite/{token}
  expiresAt: string; // ISO-8601
};

export type InvitePreview = {
  athleteName: string;
  email: string;
  expiresAt: string;
};

export type AthleteInvite = {
  id: string;
  athleteId: string;
  email: string;
  expiresAt: string;
  createdAt?: string;
  status?: "PENDING" | "ACCEPTED" | "REVOKED" | "EXPIRED";
  token?: string;
  invitePath?: string;
};

const INVITE_STORAGE_PREFIX = "pc_invite_path_";

export function rememberInviteLink(athleteId: string, invitePath: string) {
  try {
    sessionStorage.setItem(`${INVITE_STORAGE_PREFIX}${athleteId}`, invitePath);
  } catch {
    // sessionStorage indisponível
  }
}

export function getCachedInvitePath(athleteId: string): string | null {
  try {
    return sessionStorage.getItem(`${INVITE_STORAGE_PREFIX}${athleteId}`);
  } catch {
    return null;
  }
}

export function clearCachedInvitePath(athleteId: string) {
  try {
    sessionStorage.removeItem(`${INVITE_STORAGE_PREFIX}${athleteId}`);
  } catch {
    // ignore
  }
}

async function createInvite(payload: CreateInvitePayload): Promise<Invite> {
  const { data } = await apiClient.post<Invite>("/auth/invites", payload);
  return data;
}

async function getInvitePreview(token: string): Promise<InvitePreview> {
  const { data } = await apiClient.get<InvitePreview>(`/auth/invites/${token}`);
  return data;
}

async function acceptInvite(token: string, password: string): Promise<void> {
  const { data } = await apiClient.post(`/auth/invites/${token}/accept`, { password });
  // O aceite já devolve a sessão do atleta (auto-login).
  saveSession(data.accessToken, data.refreshToken);
}

/**
 * Lista convites de um atleta (T-077 / T-082).
 * Contrato assumido: GET /api/v1/athletes/{athleteId}/invites
 * Fallback defensivo: se o endpoint retornar 404 (back em dev),
 * retorna array vazio sem quebrar a UI.
 */
async function listAthleteInvites(athleteId: string): Promise<AthleteInvite[]> {
  try {
    const { data } = await apiClient.get<AthleteInvite[]>(`/athletes/${athleteId}/invites`);
    return Array.isArray(data) ? data : [];
  } catch (error) {
    const status = (error as { response?: { status?: number } }).response?.status;
    if (status === 404) {
      return [];
    }
    throw error;
  }
}

/**
 * Revogação de convite do atleta (T-077 / T-082).
 * Contrato assumido: POST /api/v1/athletes/{athleteId}/invites/{inviteId}/revoke
 * Fallbacks defensivos: DELETE /athletes/{athleteId}/invites/{inviteId}
 * ou POST /auth/invites/{inviteId}/revoke.
 */
async function revokeAthleteInvite(athleteId: string, inviteId: string): Promise<void> {
  try {
    await apiClient.post(`/athletes/${athleteId}/invites/${inviteId}/revoke`);
  } catch (error) {
    const status = (error as { response?: { status?: number } }).response?.status;
    if (status === 404 || status === 405) {
      try {
        await apiClient.delete(`/athletes/${athleteId}/invites/${inviteId}`);
      } catch (deleteError) {
        const delStatus = (deleteError as { response?: { status?: number } }).response?.status;
        if (delStatus === 404 || delStatus === 405) {
          await apiClient.post(`/auth/invites/${inviteId}/revoke`);
        } else {
          throw deleteError;
        }
      }
    } else {
      throw error;
    }
  }
}

/** Link completo para o coach repassar ao atleta. */
export function inviteUrl(invite: Pick<Invite, "invitePath">): string {
  return `${window.location.origin}${invite.invitePath}`;
}

export function useCreateInvite() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (payload: CreateInvitePayload) => {
      const invite = await createInvite(payload);
      rememberInviteLink(payload.athleteId, invite.invitePath);
      return invite;
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ["athlete-invites", data.athleteId] });
      queryClient.invalidateQueries({ queryKey: ["athletes", data.athleteId] });
      queryClient.invalidateQueries({ queryKey: ["athletes"] });
    },
  });
}

// 404 aqui é um estado esperado da tela ("convite inválido ou expirado"), não
// uma falha transitória: sem retry.
export function useInvitePreview(token: string) {
  return useQuery({
    queryKey: ["invites", token],
    queryFn: () => getInvitePreview(token),
    enabled: Boolean(token),
    retry: false,
  });
}

export function useAcceptInvite(token: string) {
  return useMutation({ mutationFn: (password: string) => acceptInvite(token, password) });
}

export function useAthleteInvites(athleteId: string) {
  return useQuery({
    queryKey: ["athlete-invites", athleteId],
    queryFn: () => listAthleteInvites(athleteId),
    enabled: Boolean(athleteId),
  });
}

export function useRevokeAthleteInvite(athleteId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (inviteId: string) => revokeAthleteInvite(athleteId, inviteId),
    onSuccess: () => {
      clearCachedInvitePath(athleteId);
      queryClient.invalidateQueries({ queryKey: ["athlete-invites", athleteId] });
      queryClient.invalidateQueries({ queryKey: ["athletes", athleteId] });
      queryClient.invalidateQueries({ queryKey: ["athletes"] });
    },
  });
}

/**
 * Mapeia erros 409 acionáveis do convite (T-079 / Issue #98):
 * - ATHLETE_HAS_CREDENTIAL: o atleta já tem usuário cadastrado
 * - EMAIL_IN_USE: e-mail já em uso por outro usuário
 */
export function getInviteErrorMessage(error: unknown): string {
  const err = error as {
    response?: {
      status?: number;
      data?: { code?: string; message?: string };
    };
  };

  if (err.response?.status === 409) {
    const code = err.response.data?.code;
    const msg = err.response.data?.message ?? "";

    if (code === "ATHLETE_HAS_CREDENTIAL" || /já possui acesso/i.test(msg)) {
      return "Este atleta já possui uma credencial de acesso ativa no aplicativo.";
    }
    if (
      code === "EMAIL_IN_USE" ||
      code === "EMAIL_ALREADY_REGISTERED" ||
      /já cadastrado/i.test(msg) ||
      /em uso/i.test(msg)
    ) {
      return "Este e-mail já pertence a outro usuário cadastrado no sistema. Utilize outro e-mail para o convite.";
    }
    return msg || "Conflito ao gerar convite. O atleta já possui acesso ou o e-mail já está em uso.";
  }

  return "Não foi possível gerar o convite. Tente novamente.";
}
