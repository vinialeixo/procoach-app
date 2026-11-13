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

export type InviteStatus = "PENDING" | "ACCEPTED" | "REVOKED" | "EXPIRED";

/** Resumo do convite (InviteSummaryResponse do backend, T-077). O token nunca é exposto. */
export type AthleteInvite = {
  id: string;
  athleteId: string;
  email: string;
  expiresAt: string;
  createdAt: string;
  status: InviteStatus;
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
 * Lista convites de um atleta (T-077).
 * Contrato real: GET /api/v1/athletes/{athleteId}/invites
 * -> array de {id, athleteId, email, expiresAt, createdAt, status}.
 */
async function listAthleteInvites(athleteId: string): Promise<AthleteInvite[]> {
  const { data } = await apiClient.get<AthleteInvite[]>(`/athletes/${athleteId}/invites`);
  return data;
}

/**
 * Revogação de convite do atleta (T-077).
 * Contrato real: POST /api/v1/athletes/{athleteId}/invites/{inviteId}/revoke
 * -> 200 com o resumo do convite revogado; 404 (inexistente/de outro coach)
 * ou 409 (não está pendente). Sem fallbacks: o contrato é estável.
 */
async function revokeAthleteInvite(
  athleteId: string,
  inviteId: string
): Promise<AthleteInvite> {
  const { data } = await apiClient.post<AthleteInvite>(
    `/athletes/${athleteId}/invites/${inviteId}/revoke`
  );
  return data;
}

/** Link completo para o coach repassar ao atleta. */
export function inviteUrl(invite: Pick<Invite, "invitePath">): string {
  return `${window.location.origin}${invite.invitePath}`;
}

/** Formata data ISO-8601 de expiração do convite para exibição em pt-BR. */
export function formatInviteExpiration(iso: string): string {
  try {
    const d = new Date(iso);
    if (isNaN(d.getTime())) return iso;
    return d.toLocaleDateString("pt-BR", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  } catch {
    return iso;
  }
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

    // Códigos estáveis do backend (T-079): ATHLETE_HAS_CREDENTIAL / EMAIL_IN_USE.
    if (code === "ATHLETE_HAS_CREDENTIAL") {
      return "Este atleta já possui uma credencial de acesso ativa no aplicativo.";
    }
    if (code === "EMAIL_IN_USE") {
      return "Este e-mail já pertence a outro usuário cadastrado no sistema. Utilize outro e-mail para o convite.";
    }
    // Código desconhecido/ausente: não vaza a mensagem crua do backend para o
    // coach; usa a orientação genérica (o backend pode mudar os textos).
    return "Conflito ao gerar convite. O atleta já possui acesso ou o e-mail já está em uso.";
  }

  return "Não foi possível gerar o convite. Tente novamente.";
}
