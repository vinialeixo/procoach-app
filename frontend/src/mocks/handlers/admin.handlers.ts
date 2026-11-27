import { http, HttpResponse } from "msw";
import { mockDb, nextMockId } from "../db";
import type { AdminCoach, CoachAccessStatus, CoachStatus } from "@/lib/admin";

type CreateCoachBody = {
  name?: string;
  email?: string;
};

export const adminHandlers = [
  http.get("/api/v1/admin/summary", () => {
    const activeCoaches = mockDb.adminCoaches.filter((c) => c.status === "ACTIVE").length;
    const suspendedCoaches = mockDb.adminCoaches.filter((c) => c.status === "SUSPENDED").length;
    const cancelledCoaches = mockDb.adminCoaches.filter((c) => c.status === "CANCELLED").length;
    const pendingInvites = mockDb.adminCoaches.filter(
      (c) => c.accessStatus === "INVITE_PENDING" && c.status !== "CANCELLED"
    ).length;
    const activeAthletes = mockDb.adminCoaches
      .filter((c) => c.status !== "CANCELLED")
      .reduce((acc, c) => acc + c.activeAthleteCount, 0);

    return HttpResponse.json({
      totalCoaches: mockDb.adminCoaches.filter((c) => c.status !== "CANCELLED").length,
      activeCoaches,
      suspendedCoaches,
      cancelledCoaches,
      pendingInvites,
      activeAthletes,
      adminCount: 2,
    });
  }),

  http.get("/api/v1/admin/coaches", ({ request }) => {
    const url = new URL(request.url);
    const status = url.searchParams.get("status") as CoachStatus | null;
    const accessStatus = url.searchParams.get("accessStatus") as CoachAccessStatus | null;
    const search = url.searchParams.get("search")?.trim().toLowerCase() ?? "";
    const includeArchived = url.searchParams.get("includeArchived") === "true";

    const items = mockDb.adminCoaches.filter((c) => {
      if (!includeArchived && c.status === "CANCELLED") return false;
      if (status && c.status !== status) return false;
      if (accessStatus && c.accessStatus !== accessStatus) return false;
      if (
        search &&
        !c.name.toLowerCase().includes(search) &&
        !c.email.toLowerCase().includes(search)
      ) {
        return false;
      }
      return true;
    });

    return HttpResponse.json({ items, total: items.length });
  }),

  http.post("/api/v1/admin/coaches", async ({ request }) => {
    const body = (await request.json()) as CreateCoachBody;
    const name = body.name?.trim() ?? "";
    const email = body.email?.trim().toLowerCase() ?? "";

    if (!name || !email) {
      return HttpResponse.json(
        { code: "VALIDATION_ERROR", message: "Nome e e-mail são obrigatórios." },
        { status: 400 }
      );
    }

    const exists = mockDb.adminCoaches.some((c) => c.email.toLowerCase() === email);
    if (exists) {
      return HttpResponse.json(
        {
          code: "EMAIL_ALREADY_REGISTERED",
          message: "Já existe um usuário cadastrado com este e-mail.",
        },
        { status: 409 }
      );
    }

    const coachId = nextMockId("coach");
    const userId = nextMockId("user");
    const token = `tok_${nextMockId("invite")}`;
    const expiresAt = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString();

    const newCoach: AdminCoach = {
      coachId,
      name,
      email,
      status: "ACTIVE",
      accessStatus: "INVITE_PENDING",
      athleteCount: 0,
      activeAthleteCount: 0,
      createdAt: new Date().toISOString(),
      inviteExpiresAt: expiresAt,
    };

    mockDb.adminCoaches.unshift(newCoach);

    return HttpResponse.json(
      {
        coachId,
        userId,
        name,
        email,
        status: "ACTIVE",
        accessStatus: "INVITE_PENDING",
        invite: { token, expiresAt, emailSent: false },
      },
      { status: 201 }
    );
  }),

  http.post("/api/v1/admin/coaches/:coachId/invites/resend", ({ params }) => {
    const coach = mockDb.adminCoaches.find((c) => c.coachId === params.coachId);
    if (!coach) {
      return HttpResponse.json({ code: "COACH_NOT_FOUND" }, { status: 404 });
    }
    const token = `tok_${nextMockId("invite")}`;
    const expiresAt = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString();
    coach.accessStatus = "INVITE_PENDING";
    coach.inviteExpiresAt = expiresAt;
    return HttpResponse.json({ token, expiresAt, emailSent: false });
  }),
];
