import axios from "axios";

// Base URL: em dev, o Vite faz proxy de /api -> http://localhost:8080 (ver vite.config.ts).
// Em produção, configurar VITE_API_BASE_URL apontando pro núcleo Java.
const baseURL = import.meta.env.VITE_API_BASE_URL ?? "/api/v1";

export const apiClient = axios.create({
  baseURL,
  headers: { "Content-Type": "application/json" },
});

// Anexa o token salvo (se existir) em toda requisição autenticada.
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem("procoach.token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Em 401, limpa a sessão local — o RBAC/isolamento multi-tenant é sempre
// validado no núcleo (ver AGENT.md / RN-01), o front nunca decide autorização.
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem("procoach.token");
      localStorage.removeItem("procoach.refreshToken");
    }
    return Promise.reject(error);
  }
);
