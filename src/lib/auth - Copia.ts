import { useMutation } from "@tanstack/react-query";
import { apiClient } from "./api-client";
import { decodeJwtPayload } from "./jwt";

/**
 * RF-01: Coach autentica com e-mail/senha + MFA (T-008/T-011).
 * Contrato real do núcleo (módulo com.fitness.procoach.security):
 *
 *   1) POST /api/v1/auth/login { email, password } -> se o MFA estiver ativo,
 *      retorna { mfaRequired: true, challengeId } (desafio de 5 min); senão
 *      retorna { accessToken, refreshToken, role, ... }.
 *   2) POST /api/v1/auth/mfa/verify { challengeId, code } -> par de tokens.
 *
 * O access token carrega os claims `role` ("COACH"/"ATHLETE") e, para atleta,
 * `athleteId` — o RBAC/isolamento é validado sempre no núcleo (RN-01).
 */

export type Role = "COACH" | "ATHLETE";

export type LoginCredentials = {
  email: string;
  password: string;
};

export type LoginResult =
  | { status: "authenticated"; token: string; refreshToken: string }
  | { status: "mfa_required"; challengeId: string };

export type MfaVerification = {
  challengeId: string;
  code: string;
};

async function login(credentials: LoginCredentials): Promise<LoginResult> {
  const { data } = await apiClient.post("/auth/login", credentials);
  if (data.mfaRequired) {
    return { status: "mfa_required", challengeId: data.challengeId };
  }
  return {
    status: "authenticated",
    token: data.accessToken,
    refreshToken: data.refreshToken,
  };
}

async function verifyMfa(
  payload: MfaVerification
): Promise<{ token: string; refreshToken: string }> {
  const { data } = await apiClient.post("/auth/mfa/verify", payload);
  return { token: data.accessToken, refreshToken: data.refreshToken };
}

// --- Registro de coach (T-011) -------------------------------------------
// POST /api/v1/auth/register cria a conta (ROLE_COACH) e já retorna a sessão
// autenticada — salvamos os tokens aqui para o passo seguinte (setup de MFA)
// chamar endpoints autenticados.
export type RegisterPayload = {
  name: string;
  email: string;
  password: string;
};

async function register(payload: RegisterPayload): Promise<{ userId: string }> {
  const { data } = await apiClient.post("/auth/register", payload);
  saveSession(data.accessToken, data.refreshToken);
  return { userId: data.userId };
}

// --- Setup de MFA (T-011) -------------------------------------------------
// Passo 1: POST /auth/mfa/setup (Bearer) -> segredo TOTP + otpauth URL p/ QR.
// Passo 2: POST /auth/mfa/setup/confirm { code } -> ativa o MFA na conta.
export type MfaSetupStart = { secret: string; otpauthUrl: string };

async function startMfaSetup(): Promise<MfaSetupStart> {
  const { data } = await apiClient.post("/auth/mfa/setup");
  return { secret: data.secret, otpauthUrl: data.otpauthUrl };
}

async function confirmMfaSetup(payload: {
  code: string;
}): Promise<{ confirmed: boolean }> {
  const { data } = await apiClient.post("/auth/mfa/setup/confirm", payload);
  return { confirmed: data.confirmed };
}

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

// --- Sessão ----------------------------------------------------------------

export function saveSession(token: string, refreshToken?: string) {
  localStorage.setItem("procoach.token", token);
  if (refreshToken) {
    localStorage.setItem("procoach.refreshToken", refreshToken);
  }
}

export function isAuthenticated() {
  return Boolean(localStorage.getItem("procoach.token"));
}

export function logout() {
  localStorage.removeItem("procoach.token");
  localStorage.removeItem("procoach.refreshToken");
}

// --- Papel do usuário (Coach x Atleta) ------------------------------------
// Lido do payload do JWT para ESCOLHER layout/rotas e ESCONDER UI. Isto NÃO
// é autorização: o RBAC/isolamento multi-tenant (RN-01) é validado sempre no
// núcleo Java — o front só reage a 401/403 (ver AGENTS.md / jwt.ts).
export function getRole(): Role | null {
  const token = localStorage.getItem("procoach.token");
  if (!token) return null;
  return decodeJwtPayload(token)?.role ?? null;
}

// Rota inicial por persona: coach cai no painel, atleta na área dele.
export function homeForRole(role: Role | null): string {
  return role === "ATHLETE" ? "/athlete" : "/";
}
