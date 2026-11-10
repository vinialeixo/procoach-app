import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "./api-client";

export type AthleteNotification = {
  id: string;
  type: string;
  message: string;
  referenceType: string | null;
  referenceId: string | null;
  createdAt: string;
  read: boolean;
  readAt: string | null;
};

/**
 * T-075 — canais externos do aviso (NotificationController, T-071). O aviso
 * dentro do app sempre existe; e-mail e push só respeitam estas preferências.
 * Sem registro no backend, ambos vêm ligados.
 */
export type NotificationPreferences = {
  emailEnabled: boolean;
  pushEnabled: boolean;
};

export type DevicePlatform = "ANDROID" | "IOS" | "WEB";

export type RegisterDevicePayload = {
  token: string;
  platform: DevicePlatform;
};

const notificationKey = (athleteId: string) => ["notifications", athleteId];
const preferencesKey = (athleteId: string) => ["notification-preferences", athleteId] as const;
const deviceTokenStoragePrefix = "procoach.pushDevice.";

// ============================================================================
// T-075 — preferências de canal
// ============================================================================

async function getNotificationPreferences(): Promise<NotificationPreferences> {
  const { data } = await apiClient.get<NotificationPreferences>("/notifications/preferences");
  return data;
}

export function useNotificationPreferences(athleteId: string) {
  return useQuery({
    queryKey: preferencesKey(athleteId),
    queryFn: getNotificationPreferences,
    enabled: Boolean(athleteId),
    retry: false,
  });
}

export function useUpdateNotificationPreferences(athleteId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (preferences: NotificationPreferences) => {
      const { data } = await apiClient.put<NotificationPreferences>(
        "/notifications/preferences",
        preferences
      );
      return data;
    },
    onSuccess: (preferences) =>
      queryClient.setQueryData(preferencesKey(athleteId), preferences),
  });
}

// ============================================================================
// T-075 — dispositivos de push
// ============================================================================

export function useRegisterNotificationDevice() {
  return useMutation({
    mutationFn: async (payload: RegisterDevicePayload) => {
      await apiClient.post("/notifications/devices", payload);
    },
  });
}

export function useUnregisterNotificationDevice() {
  return useMutation({
    mutationFn: async (token: string) => {
      await apiClient.delete("/notifications/devices", { params: { token } });
    },
  });
}

/**
 * Plataforma enviada ao backend. iPads recentes se apresentam como macOS no
 * user agent; por isso o toque (maxTouchPoints) também é considerado.
 */
export function detectDevicePlatform(
  userAgent: string,
  maxTouchPoints = 0
): DevicePlatform {
  if (/android/i.test(userAgent)) return "ANDROID";
  if (/iphone|ipad|ipod/i.test(userAgent)) return "IOS";
  if (/macintosh/i.test(userAgent) && maxTouchPoints > 1) return "IOS";
  return "WEB";
}

/**
 * Identificador estável deste navegador/instalação. O token de push de
 * verdade é emitido pelo provedor (FCM/APNs/Web Push) na T-076; até lá este
 * identificador permite registrar e remover o dispositivo pelo contrato real
 * do backend (`POST/DELETE /notifications/devices`) sem inventar um aparelho.
 */
export function createBrowserDeviceToken(): string {
  const cryptoApi = globalThis.crypto;
  const id =
    cryptoApi && typeof cryptoApi.randomUUID === "function"
      ? cryptoApi.randomUUID()
      : `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 12)}`;
  return `web-${id}`;
}

/** Token registrado por este navegador para o atleta (se houver). */
export function getBrowserDeviceToken(athleteId: string): string | null {
  return localStorage.getItem(`${deviceTokenStoragePrefix}${athleteId}`);
}

export function saveBrowserDeviceToken(athleteId: string, token: string) {
  localStorage.setItem(`${deviceTokenStoragePrefix}${athleteId}`, token);
}

export function clearBrowserDeviceToken(athleteId: string) {
  localStorage.removeItem(`${deviceTokenStoragePrefix}${athleteId}`);
}

/** O mesmo navegador pode estar registrado em contas diferentes. */
export function detectCurrentDevicePlatform(): DevicePlatform {
  return detectDevicePlatform(navigator.userAgent, navigator.maxTouchPoints ?? 0);
}

async function listNotifications(athleteId: string): Promise<AthleteNotification[]> {
  const { data } = await apiClient.get<AthleteNotification[]>("/notifications", {
    params: { athleteId },
  });
  return data;
}

export function useAthleteNotifications(athleteId: string) {
  return useQuery({
    queryKey: notificationKey(athleteId),
    queryFn: () => listNotifications(athleteId),
    enabled: Boolean(athleteId),
    retry: false,
  });
}

export function useMarkNotificationRead(athleteId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (notificationId: string) => {
      const { data } = await apiClient.post<AthleteNotification>(
        `/notifications/${notificationId}/read`
      );
      return data;
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: notificationKey(athleteId) }),
  });
}

export function useMarkAllNotificationsRead(athleteId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      const { data } = await apiClient.post<{ marked: number }>("/notifications/read-all");
      return data;
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: notificationKey(athleteId) }),
  });
}
