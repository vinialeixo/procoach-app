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
 *   POST /api/v1/auth/mfa/verify     -> tokens (etapa 2: challengeId + código)
 *   POST /api/v1/auth/register       -> userId + tokens (cadastro de coach)
 *   POST /api/v1/auth/mfa/setup      -> { secret, otpauthUrl }  (Bearer)
 *   POST /api/v1/auth/mfa/setup/confirm -> { confirmed: true }  (Bearer)
 *   POST /api/v1/auth/refresh        -> novo par de tokens
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

export type LoginResult =
  | {
      status: "authenticated";
      token: string;
      refreshToken: string;
    }
  | {
      status: "mfa_required";
      challengeId: string;
    };

async function login(credentials: LoginCredentials): Promise<LoginResult> {
  const { data } = await apiClient.post("/auth/login", credentials);

  if (data.mfaRequired) {
    return {
      status: "mfa_required",
      challengeId: data.challengeId,
    };
  }

  const token: string = data.accessToken;
  const refreshToken: string = data.refreshToken;

  saveSession(token, refreshToken);

  return { status: "authenticated", token, refreshToken };
}

// ============================================================================
// MFA
// ============================================================================

export type MfaVerification = {
  challengeId: string;
  code: string;
};

async function verifyMfa(
  payload: MfaVerification
): Promise<{ token: string; refreshToken: string }> {
  const { data } = await apiClient.post("/auth/mfa/verify", payload);

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
// Setup de MFA
// ============================================================================

export type MfaSetupStart = {
  secret: string;
  otpauthUrl: string;
};

async function startMfaSetup(): Promise<MfaSetupStart> {
  const { data } = await apiClient.post("/auth/mfa/setup");

  return {
    secret: data.secret,
    otpauthUrl: data.otpauthUrl,
  };
}

async function confirmMfaSetup(payload: {
  code: string;
}): Promise<{ confirmed: boolean }> {
  const { data } = await apiClient.post("/auth/mfa/setup/confirm", payload);

  return { confirmed: data.confirmed };
}

// ============================================================================
// React Query Hooks
// ============================================================================

export function useLogin() {
  return useMutation({ mutationFn: login });
}

export function useVerifyMfa() {
  return useMutation({ mutationFn: verifyMfa });
}

export function useRegister() {
  return useMutation({ mutationFn: register });
}

export function useStartMfaSetup() {
  return useMutation({ mutationFn: startMfaSetup });
}

export function useConfirmMfaSetup() {
  return useMutation({ mutationFn: confirmMfaSetup });
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
