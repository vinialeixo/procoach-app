import { useMutation } from "@tanstack/react-query";
import { apiClient } from "./api-client";
import { decodeJwtPayload } from "./jwt";

/**
 * ============================================================================
 * AUTH — integração real com o núcleo Java (T-008/T-011)
 * ============================================================================
 *
 * Contrato oficial implementado em AuthController (com.fitness.procoach.security):
 *
 *   POST /api/v1/auth/login          -> LoginResult
 *   POST /api/v1/auth/register       -> userId + tokens (cadastro de coach)
 *   POST /api/v1/auth/refresh        -> novo par de tokens
 *
 * MFA/TOTP está desligado (backlog T-059/T-060): o backend só o exige com
 * procoach.security.mfa.enabled=true. O código do front foi removido neste
 * commit e pode ser recuperado do histórico do git ao reintroduzir.
 *
 * O api-client anexa o header `Authorization: Bearer <token>` a cada chamada.
 * O papel (COACH/ATHLETE) vem no claim `role` do JWT — o front só o usa para
 * navegação/UI; a autorização real é validada no núcleo a cada requisição.
 */

// ============================================================================
// Roles
// ============================================================================

export type Role = "COACH" | "ATHLETE";

// ============================================================================
// Login
// ============================================================================

export type LoginCredentials = {
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
// Registro de Coach
// ============================================================================

export type RegisterPayload = {
  name: string;
  email: string;
  password: string;
};

async function register(
  payload: RegisterPayload
): Promise<{ userId: string }> {
  const { data } = await apiClient.post("/auth/register", payload);

  // Auto-login do núcleo: o cadastro já devolve o par de tokens.
  saveSession(data.accessToken, data.refreshToken);

  return { userId: data.userId };
}

// ============================================================================
// React Query Hooks
// ============================================================================

export function useLogin() {
  return useMutation({ mutationFn: login });
}

export function useRegister() {
  return useMutation({ mutationFn: register });
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
  return Boolean(localStorage.getItem("procoach.token"));
}

export function logout() {
  localStorage.removeItem("procoach.token");
  localStorage.removeItem("procoach.refreshToken");
}

// ============================================================================
// Papel do usuário
// ============================================================================

/**
 * Papel (COACH/ATHLETE) a partir do claim `role` do JWT salvo.
 *
 * IMPORTANTE:
 *
 * O frontend usa o role somente para:
 *
 * - escolher layout;
 * - escolher rota;
 * - esconder/mostrar elementos da interface.
 *
 * A autorização REAL é sempre validada pelo backend (RBAC / isolamento por
 * perfil, RN-01) a cada requisição — nunca confiar neste valor para
 * segurança.
 */
export function getRole(): Role | null {
  const token = localStorage.getItem("procoach.token");

  if (!token) {
    return null;
  }

  const role = decodeJwtPayload(token)?.role;

  if (role === "COACH" || role === "ATHLETE") {
    return role;
  }

  return null;
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
  if (role === "ATHLETE") {
    return "/athlete";
  }

  if (role === "COACH") {
    return "/coach";
  }

  return "/login";
}
