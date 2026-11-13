import { http, HttpResponse } from "msw";
import {
  MOCK_ATHLETE_CREDENTIALS,
  MOCK_COACH_CREDENTIALS,
  mockDb,
  nextMockId,
} from "../db";
import { createMockJwt, extractBearerPayload } from "../jwt";

type LoginBody = {
  email?: string;
  password?: string;
};

type RegisterBody = {
  name?: string;
  email?: string;
  password?: string;
};

export const authHandlers = [
  http.post("/api/v1/auth/login", async ({ request }) => {
    const body = (await request.json()) as LoginBody;
    const email = body.email?.trim().toLowerCase() ?? "";

    if (!email || !body.password || body.password === "wrong") {
      return HttpResponse.json(
        { code: "INVALID_CREDENTIALS", message: "E-mail ou senha incorretos." },
        { status: 401 }
      );
    }

    const athlete = mockDb.athletes.find(
      (item) => item.email?.toLowerCase() === email && item.hasCredential
    );

    if (athlete) {
      const accessToken = createMockJwt({
        sub: athlete.id,
        role: "ATHLETE",
        athleteId: athlete.id,
        email: athlete.email ?? email,
        name: athlete.name,
      });
      return HttpResponse.json({
        accessToken,
        refreshToken: `mock-refresh-${athlete.id}`,
      });
    }

    const accessToken = createMockJwt({
      sub: mockDb.coach.id,
      role: "COACH",
      email: email || MOCK_COACH_CREDENTIALS.email,
      name: mockDb.coach.name,
    });

    return HttpResponse.json({
      accessToken,
      refreshToken: `mock-refresh-${mockDb.coach.id}`,
    });
  }),

  http.post("/api/v1/auth/register", async ({ request }) => {
    const body = (await request.json()) as RegisterBody;
    const email = body.email?.trim().toLowerCase() ?? "";
    const name = body.name?.trim() || "Novo Coach";

    if (!email || !body.password) {
      return HttpResponse.json(
        { code: "VALIDATION_ERROR", message: "Dados obrigatórios ausentes." },
        { status: 400 }
      );
    }

    const userId = nextMockId("coach");
    mockDb.coach = { id: userId, name, email };

    const accessToken = createMockJwt({
      sub: userId,
      role: "COACH",
      email,
      name,
    });

    return HttpResponse.json(
      {
        userId,
        accessToken,
        refreshToken: `mock-refresh-${userId}`,
      },
      { status: 201 }
    );
  }),

  http.post("/api/v1/auth/refresh", () => {
    const accessToken = createMockJwt({
      sub: mockDb.coach.id,
      role: "COACH",
      email: mockDb.coach.email,
      name: mockDb.coach.name,
    });
    return HttpResponse.json({
      accessToken,
      refreshToken: `mock-refresh-${mockDb.coach.id}`,
    });
  }),

  http.get("/api/v1/auth/me", ({ request }) => {
    const payload = extractBearerPayload(request);
    if (!payload || !payload.role) {
      return HttpResponse.json(
        { code: "UNAUTHORIZED", message: "Sessão inválida." },
        { status: 401 }
      );
    }

    if (payload.role === "ATHLETE") {
      const athlete =
        mockDb.athletes.find((item) => item.id === payload.athleteId) ??
        mockDb.athletes.find((item) => item.id === MOCK_ATHLETE_CREDENTIALS.athleteId);

      return HttpResponse.json({
        id: payload.sub ?? athlete?.id,
        role: "ATHLETE",
        athleteId: athlete?.id ?? payload.athleteId,
        name: athlete?.name ?? MOCK_ATHLETE_CREDENTIALS.name,
        email: athlete?.email ?? MOCK_ATHLETE_CREDENTIALS.email,
      });
    }

    return HttpResponse.json({
      id: payload.sub ?? mockDb.coach.id,
      role: "COACH",
      name: mockDb.coach.name,
      email: mockDb.coach.email,
    });
  }),
];
