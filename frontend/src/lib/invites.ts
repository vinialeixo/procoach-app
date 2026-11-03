import { useMutation, useQuery } from "@tanstack/react-query";
import { apiClient } from "./api-client";
import { saveSession } from "./auth";

/**
 * Convite do atleta (primeira credencial) — contrato do backend
 * (AthleteInviteController, /api/v1/auth/invites):
 *
 *   POST /auth/invites                 -> coach gera o convite (Bearer, COACH)
 *   GET  /auth/invites/{token}         -> público: nome do atleta + e-mail
 *   POST /auth/invites/{token}/accept  -> público: define a senha e abre a sessão
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

/** Link completo para o coach repassar ao atleta. */
export function inviteUrl(invite: Pick<Invite, "invitePath">): string {
  return `${window.location.origin}${invite.invitePath}`;
}

export function useCreateInvite() {
  return useMutation({ mutationFn: createInvite });
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
