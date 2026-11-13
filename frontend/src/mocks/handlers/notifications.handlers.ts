import { http, HttpResponse } from "msw";
import type { NotificationPreferences, RegisterDevicePayload } from "@/lib/notifications";
import { MOCK_ATHLETE_CREDENTIALS, mockDb } from "../db";
import { extractBearerPayload } from "../jwt";

function resolveAthleteId(request: Request, explicitId?: string | null): string {
  if (explicitId) return explicitId;
  const payload = extractBearerPayload(request);
  if (payload?.athleteId) return payload.athleteId;
  return MOCK_ATHLETE_CREDENTIALS.athleteId;
}

export const notificationsHandlers = [
  http.get("/api/v1/notifications", ({ request }) => {
    const url = new URL(request.url);
    const athleteId = resolveAthleteId(request, url.searchParams.get("athleteId"));
    return HttpResponse.json(mockDb.notifications[athleteId] ?? []);
  }),

  http.post("/api/v1/notifications/:id/read", ({ params }) => {
    const now = new Date().toISOString();
    for (const items of Object.values(mockDb.notifications)) {
      const target = items.find((item) => item.id === params.id);
      if (target) {
        target.read = true;
        target.readAt = now;
        return HttpResponse.json(target);
      }
    }
    return HttpResponse.json({ message: "Notificação não encontrada." }, { status: 404 });
  }),

  http.post("/api/v1/notifications/read-all", ({ request }) => {
    const athleteId = resolveAthleteId(request);
    const items = mockDb.notifications[athleteId] ?? [];
    const now = new Date().toISOString();
    let marked = 0;

    for (const item of items) {
      if (!item.read) {
        item.read = true;
        item.readAt = now;
        marked += 1;
      }
    }

    return HttpResponse.json({ marked });
  }),

  http.get("/api/v1/notifications/preferences", ({ request }) => {
    const athleteId = resolveAthleteId(request);
    const prefs = mockDb.notificationPreferences[athleteId] ?? {
      emailEnabled: true,
      pushEnabled: true,
    };
    return HttpResponse.json(prefs);
  }),

  http.put("/api/v1/notifications/preferences", async ({ request }) => {
    const athleteId = resolveAthleteId(request);
    const body = (await request.json()) as NotificationPreferences;
    const updated: NotificationPreferences = {
      emailEnabled: Boolean(body.emailEnabled),
      pushEnabled: Boolean(body.pushEnabled),
    };
    mockDb.notificationPreferences[athleteId] = updated;
    return HttpResponse.json(updated);
  }),

  http.post("/api/v1/notifications/devices", async ({ request }) => {
    const athleteId = resolveAthleteId(request);
    const body = (await request.json()) as RegisterDevicePayload;
    const exists = mockDb.devices.some(
      (item) => item.athleteId === athleteId && item.token === body.token
    );
    if (!exists) {
      mockDb.devices.push({ athleteId, token: body.token, platform: body.platform });
    }
    return new HttpResponse(null, { status: 204 });
  }),

  http.delete("/api/v1/notifications/devices", ({ request }) => {
    const url = new URL(request.url);
    const token = url.searchParams.get("token");
    mockDb.devices = mockDb.devices.filter((item) => item.token !== token);
    return new HttpResponse(null, { status: 204 });
  }),
];
