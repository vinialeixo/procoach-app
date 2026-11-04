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

const notificationKey = (athleteId: string) => ["notifications", athleteId];

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
