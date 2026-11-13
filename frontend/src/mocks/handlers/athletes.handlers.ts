import { http, HttpResponse } from "msw";
import type { AthleteStatus, Goal, Sex } from "@/lib/athletes";
import { mockDb, nextMockId, type MockApiAthlete, type MockInviteRecord } from "../db";
import { createMockJwt } from "../jwt";

type CreateAthleteBody = {
  name: string;
  email?: string;
  sex: Sex;
  birthDate: string;
  height: number;
  weight: number;
  goal: Goal;
};

type ChangeStatusBody = {
  status: AthleteStatus;
  reason: string;
};

type CreateInviteBody = {
  athleteId: string;
  email: string;
};

type AcceptInviteBody = {
  password: string;
};

export const athletesHandlers = [
  http.get("/api/v1/athletes", ({ request }) => {
    const url = new URL(request.url);
    const status = url.searchParams.get("status");
    const items = status
      ? mockDb.athletes.filter((athlete) => athlete.status === status)
      : mockDb.athletes;
    return HttpResponse.json(items);
  }),

  http.get("/api/v1/athletes/:id", ({ params }) => {
    const athlete = mockDb.athletes.find((item) => item.id === params.id);
    if (!athlete) {
      return HttpResponse.json({ message: "Atleta não encontrado." }, { status: 404 });
    }
    return HttpResponse.json(athlete);
  }),

  http.post("/api/v1/athletes", async ({ request }) => {
    const body = (await request.json()) as CreateAthleteBody;
    const cleanEmail = body.email?.trim().toLowerCase() || null;

    if (
      cleanEmail &&
      mockDb.athletes.some((item) => item.email?.toLowerCase() === cleanEmail)
    ) {
      return HttpResponse.json(
        { code: "EMAIL_IN_USE", message: "Já existe um atleta com este e-mail." },
        { status: 409 }
      );
    }

    const created: MockApiAthlete = {
      id: nextMockId("athlete"),
      name: body.name.trim(),
      email: cleanEmail,
      sex: body.sex,
      birthDate: body.birthDate,
      height: body.height,
      weight: body.weight,
      goal: body.goal,
      status: "ACTIVE",
      createdAt: new Date().toISOString(),
      hasCredential: false,
      pendingInvite: null,
    };

    mockDb.athletes.unshift(created);
    return HttpResponse.json(created, { status: 201 });
  }),

  http.post("/api/v1/athletes/:id/status", async ({ params, request }) => {
    const body = (await request.json()) as ChangeStatusBody;
    const athlete = mockDb.athletes.find((item) => item.id === params.id);

    if (!athlete) {
      return HttpResponse.json({ message: "Atleta não encontrado." }, { status: 404 });
    }
    if (!body.reason?.trim()) {
      return HttpResponse.json({ message: "Motivo obrigatório." }, { status: 400 });
    }
    if (athlete.status === "FINISHED" && body.status !== "FINISHED") {
      return HttpResponse.json(
        { message: "Atleta finalizado não pode ser reativado." },
        { status: 409 }
      );
    }

    athlete.status = body.status;
    return HttpResponse.json(athlete);
  }),

  http.post("/api/v1/auth/invites", async ({ request }) => {
    const body = (await request.json()) as CreateInviteBody;
    const athlete = mockDb.athletes.find((item) => item.id === body.athleteId);

    if (!athlete) {
      return HttpResponse.json({ message: "Atleta não encontrado." }, { status: 404 });
    }
    if (athlete.hasCredential) {
      return HttpResponse.json(
        { code: "ATHLETE_HAS_CREDENTIAL", message: "Atleta já possui credencial." },
        { status: 409 }
      );
    }

    const email = body.email.trim().toLowerCase();
    const emailUsedByAnother =
      email === mockDb.coach.email.toLowerCase() ||
      mockDb.athletes.some(
        (item) =>
          item.id !== athlete.id &&
          item.hasCredential &&
          item.email?.toLowerCase() === email
      );

    if (emailUsedByAnother) {
      return HttpResponse.json(
        { code: "EMAIL_IN_USE", message: "Este e-mail já está em uso." },
        { status: 409 }
      );
    }

    // Revoga convite pendente anterior do mesmo atleta antes de emitir o novo.
    for (const inv of mockDb.invites) {
      if (inv.athleteId === athlete.id && inv.status === "PENDING") {
        inv.status = "REVOKED";
      }
    }

    const now = new Date();
    const expiresAt = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000).toISOString();
    const inviteId = nextMockId("inv");
    const token = `mock-token-${inviteId}`;

    const record: MockInviteRecord = {
      id: inviteId,
      athleteId: athlete.id,
      email,
      expiresAt,
      createdAt: now.toISOString(),
      status: "PENDING",
      token,
    };

    mockDb.invites.unshift(record);
    athlete.email = email;
    athlete.pendingInvite = { id: inviteId, email, expiresAt };

    return HttpResponse.json(
      {
        athleteId: athlete.id,
        email,
        token,
        invitePath: `/convite/${token}`,
        expiresAt,
      },
      { status: 201 }
    );
  }),

  http.get("/api/v1/auth/invites/:token", ({ params }) => {
    const invite = mockDb.invites.find(
      (item) => item.token === params.token && item.status === "PENDING"
    );
    if (!invite) {
      return HttpResponse.json({ message: "Convite inválido ou expirado." }, { status: 404 });
    }
    const athlete = mockDb.athletes.find((item) => item.id === invite.athleteId);
    if (!athlete) {
      return HttpResponse.json({ message: "Convite inválido." }, { status: 404 });
    }
    return HttpResponse.json({
      athleteName: athlete.name,
      email: invite.email,
      expiresAt: invite.expiresAt,
    });
  }),

  http.post("/api/v1/auth/invites/:token/accept", async ({ params, request }) => {
    const body = (await request.json()) as AcceptInviteBody;
    if (!body.password?.trim()) {
      return HttpResponse.json({ message: "Senha obrigatória." }, { status: 400 });
    }

    const invite = mockDb.invites.find(
      (item) => item.token === params.token && item.status === "PENDING"
    );
    if (!invite) {
      return HttpResponse.json({ message: "Convite inválido ou expirado." }, { status: 404 });
    }

    const athlete = mockDb.athletes.find((item) => item.id === invite.athleteId);
    if (!athlete) {
      return HttpResponse.json({ message: "Atleta não encontrado." }, { status: 404 });
    }

    invite.status = "ACCEPTED";
    athlete.hasCredential = true;
    athlete.email = invite.email;
    athlete.pendingInvite = null;

    const accessToken = createMockJwt({
      sub: athlete.id,
      role: "ATHLETE",
      athleteId: athlete.id,
      email: invite.email,
      name: athlete.name,
    });

    return HttpResponse.json({
      accessToken,
      refreshToken: `mock-refresh-${athlete.id}`,
    });
  }),

  http.get("/api/v1/athletes/:athleteId/invites", ({ params }) => {
    const invites = mockDb.invites
      .filter((item) => item.athleteId === params.athleteId)
      .map(({ token: _token, ...summary }) => summary);
    return HttpResponse.json(invites);
  }),

  http.post("/api/v1/athletes/:athleteId/invites/:inviteId/revoke", ({ params }) => {
    const invite = mockDb.invites.find(
      (item) => item.id === params.inviteId && item.athleteId === params.athleteId
    );
    if (!invite) {
      return HttpResponse.json({ message: "Convite não encontrado." }, { status: 404 });
    }
    if (invite.status !== "PENDING") {
      return HttpResponse.json(
        { message: "Apenas convites pendentes podem ser revogados." },
        { status: 409 }
      );
    }

    invite.status = "REVOKED";
    const athlete = mockDb.athletes.find((item) => item.id === params.athleteId);
    if (athlete && athlete.pendingInvite?.id === invite.id) {
      athlete.pendingInvite = null;
    }

    const { token: _token, ...summary } = invite;
    return HttpResponse.json(summary);
  }),
];
