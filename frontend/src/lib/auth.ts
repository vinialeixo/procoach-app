import { useMutation } from "@tanstack/react-query";
import { apiClient } from "./api-client";
import { decodeJwtPayload } from "./jwt";

/**
 * ============================================================================
 * AUTH — integração real com o núcleo Java (T-008/T-011/T-107)
 * ============================================================================
 *
 * Contrato oficial implementado em AuthController (com.fitness.procoach.security):
 *
 *   POST /api/v1/auth/login          -> LoginResult (aceita e-mail ou username)
 *   POST /api/v1/auth/refresh        -> novo par de tokens
 *
 * O cadastro público de coach (/auth/register) foi removido (US-043 / T-107):
 * novos coaches entram exclusivamente por convite emitido pelo administrador.
 *
 * O api-client anexa o header `Authorization: Bearer <token>` a cada chamada.
 * O papel (ADMIN/COACH/ATHLETE) vem no claim `role` do JWT — o front só o usa
 * para navegação/UI; a autorização real é validada no núcleo a cada requisição.
 */

// ============================================================================
// Roles
// ============================================================================

export type Role = "COACH" | "ATHLETE" | "ADMIN";

// ============================================================================
// Login
// ============================================================================

export type LoginCredentials = {
  /** Aceita e-mail (coach/atleta/admin) ou username (ex.: "admin"). */
  email: string;
  password: string;
};

export type LoginResult = {
  token: string;
  refreshToken: string;
};

async function login(credentials: LoginCredentials): Promise<LoginResult> {
  const { data } = await apiClient.post("/auth/login", credentials);

  const token: string = data.accessToken;
  const refreshToken: string = data.refreshToken;

  saveSession(token, refreshToken);

  return { token, refreshToken };
}

// ============================================================================
// React Query Hooks
// ============================================================================

export function useLogin() {
  return useMutation({ mutationFn: login });
}

// ============================================================================
// Sessão (localStorage)
// ============================================================================

export function saveSession(token: string, refreshToken?: string) {
  localStorage.setItem("procoach.token", token);
  if (refreshToken) {
    localStorage.setItem("procoach.refreshToken", refreshToken);
  }
}

export function isAuthenticated(): boolean {
  const token = localStorage.getItem("procoach.token");
  if (!token) {
    return false;
  }

  const payload = decodeJwtPayload(token);
  if (!payload) {
    return false;
  }

  if (typeof payload.exp === "number") {
    const isExpired = payload.exp <= Date.now() / 1000;
    if (isExpired) {
      return Boolean(localStorage.getItem("procoach.refreshToken"));
    }
  }

  return true;
}

export function logout() {
  localStorage.removeItem("procoach.token");
  localStorage.removeItem("procoach.refreshToken");
}

// ============================================================================
// Papel do usuário e estado da senha
// ============================================================================

/**
 * Papel (ADMIN/COACH/ATHLETE) a partir do claim `role` do JWT salvo.
 *
 * IMPORTANTE: o frontend usa o role somente para layout, rota e exibição de UI.
 * A autorização REAL é sempre validada pelo backend (RBAC / RN-01).
 */
export function getRole(): Role | null {
  const token = localStorage.getItem("procoach.token");

  if (!token) {
    return null;
  }

  const role = decodeJwtPayload(token)?.role;

  if (role === "COACH" || role === "ATHLETE" || role === "ADMIN") {
    return role;
  }

  return null;
}

/**
 * Indica se o JWT atual exige troca obrigatória de senha antes de liberar
 * o painel administrativo (primeiro login do admin bootstrap ou admin recém-criado).
 */
export function mustChangePassword(): boolean {
  const token = localStorage.getItem("procoach.token");
  if (!token) {
    return false;
  }
  return decodeJwtPayload(token)?.mustChangePassword === true;
}

/**
 * Retorna o athleteId presente no access token da sessão de atleta.
 * Este identificador só seleciona quais dados solicitar; autorização e
 * isolamento continuam sendo verificados pelo backend em cada requisição.
 */
export function getAthleteId(): string | null {
  const token = localStorage.getItem("procoach.token");
  if (!token) return null;

  const payload = decodeJwtPayload(token);
  return payload?.role === "ATHLETE" && typeof payload.athleteId === "string"
    ? payload.athleteId
    : null;
}

// ============================================================================
// Rota inicial por persona
// ============================================================================

export function homeForRole(role: Role | null): string {
  if (role === "ADMIN") {
    return "/admin";
  }

  if (role === "ATHLETE") {
    return "/athlete";
  }

  if (role === "COACH") {
    return "/coach";
  }

  return "/login";
}
