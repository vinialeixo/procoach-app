import axios, { AxiosError, InternalAxiosRequestConfig } from "axios";
import { queryClient } from "./query-client";

export const TOKEN_STORAGE_KEY = "procoach.token";
export const REFRESH_TOKEN_STORAGE_KEY = "procoach.refreshToken";

// Base URL: em dev, o Vite faz proxy de /api -> http://localhost:8080 (ver vite.config.ts).
// Em produção, configurar VITE_API_BASE_URL apontando pro núcleo Java.
const baseURL = import.meta.env.VITE_API_BASE_URL ?? "/api/v1";

export const apiClient = axios.create({
  baseURL,
  headers: { "Content-Type": "application/json" },
});

type RetryableRequestConfig = InternalAxiosRequestConfig & {
  _retry?: boolean;
};

type RefreshTokenResponse = {
  accessToken: string;
  refreshToken?: string;
};

type SessionExpiredHandler = () => void;
type PasswordChangeRequiredHandler = () => void;

let sessionExpiredHandler: SessionExpiredHandler | null = null;
let passwordChangeRequiredHandler: PasswordChangeRequiredHandler | null = null;
let refreshPromise: Promise<string> | null = null;

export function setSessionExpiredHandler(
  handler: SessionExpiredHandler | null
): void {
  sessionExpiredHandler = handler;
}

export function setPasswordChangeRequiredHandler(
  handler: PasswordChangeRequiredHandler | null
): void {
  passwordChangeRequiredHandler = handler;
}

export function resetRefreshStateForTests(): void {
  refreshPromise = null;
  sessionExpiredHandler = null;
  passwordChangeRequiredHandler = null;
}

function isAuthEndpoint(url?: string): boolean {
  if (!url) {
    return false;
  }
  return /(?:^|\/)auth\/(?:login|refresh)(?:\?|$)/.test(url);
}

function defaultRedirectToLogin(): void {
  if (typeof window === "undefined") {
    return;
  }
  if (window.location.pathname !== "/login") {
    window.location.assign("/login?reason=session-expired");
  }
}

function handleSessionExpired(): void {
  localStorage.removeItem(TOKEN_STORAGE_KEY);
  localStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY);
  queryClient.clear();

  if (sessionExpiredHandler) {
    sessionExpiredHandler();
    return;
  }
  defaultRedirectToLogin();
}

async function performTokenRefresh(refreshToken: string): Promise<string> {
  try {
    const { data } = await apiClient.post<RefreshTokenResponse>(
      "/auth/refresh",
      { refreshToken }
    );

    if (!data?.accessToken) {
      throw new Error("Refresh response missing accessToken");
    }

    localStorage.setItem(TOKEN_STORAGE_KEY, data.accessToken);
    if (data.refreshToken) {
      localStorage.setItem(REFRESH_TOKEN_STORAGE_KEY, data.refreshToken);
    }

    return data.accessToken;
  } catch (refreshError) {
    handleSessionExpired();
    throw refreshError;
  }
}

// Anexa o token salvo (se existir) em toda requisição autenticada.
apiClient.interceptors.request.use((config) => {
  if (!isAuthEndpoint(config.url)) {
    const token = localStorage.getItem(TOKEN_STORAGE_KEY);
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  }
  return config;
});

// Em 401 (exceto endpoints públicos de auth), tenta renovar o token via
// /auth/refresh com lock para requisições concorrentes e repete 1x.
// Se falhar ou não houver refreshToken, limpa sessão + cache e redireciona.
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const status = error.response?.status;
    const errorCode = (error.response?.data as { code?: string } | undefined)?.code;
    if (status === 403 && errorCode === "PASSWORD_CHANGE_REQUIRED") {
      if (passwordChangeRequiredHandler) {
        passwordChangeRequiredHandler();
      } else if (
        typeof window !== "undefined" &&
        window.location.pathname !== "/admin/alterar-senha"
      ) {
        window.location.assign("/admin/alterar-senha");
      }
      return Promise.reject(error);
    }

    const originalRequest = error.config as RetryableRequestConfig | undefined;

    if (status !== 401 || !originalRequest || isAuthEndpoint(originalRequest.url)) {
      return Promise.reject(error);
    }

    if (originalRequest._retry) {
      handleSessionExpired();
      return Promise.reject(error);
    }

    const refreshToken = localStorage.getItem(REFRESH_TOKEN_STORAGE_KEY);
    if (!refreshToken) {
      handleSessionExpired();
      return Promise.reject(error);
    }

    originalRequest._retry = true;

    if (!refreshPromise) {
      refreshPromise = performTokenRefresh(refreshToken).finally(() => {
        refreshPromise = null;
      });
    }

    const newAccessToken = await refreshPromise;
    originalRequest.headers.set("Authorization", `Bearer ${newAccessToken}`);
    return apiClient(originalRequest);
  }
);
