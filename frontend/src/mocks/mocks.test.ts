import { afterAll, afterEach, beforeAll, describe, expect, it } from "vitest";
import { setupServer } from "msw/node";
import { decodeJwtPayload } from "@/lib/jwt";
import { handlers } from "./handlers";
import {
  MOCK_ATHLETE_CREDENTIALS,
  MOCK_COACH_CREDENTIALS,
  mockDb,
  resetMockDb,
} from "./db";

const server = setupServer(...handlers);

beforeAll(() => server.listen({ onUnhandledFrame: "error" }));
afterEach(() => {
  server.resetHandlers();
  resetMockDb();
});
afterAll(() => server.close());

type FetchOptions = {
  method?: string;
  body?: string;
  headers?: Record<string, string>;
};

async function jsonFetch<T>(url: string, init?: FetchOptions): Promise<{ status: number; data: T }> {
  const origin =
    typeof window !== "undefined" && window.location?.origin && window.location.origin !== "null"
      ? window.location.origin
      : "http://localhost";
  const response = await fetch(`${origin}${url}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers ?? {}),
    },
  });
  const text = await response.text();
  const data = (text ? JSON.parse(text) : null) as T;
  return { status: response.status, data };
}

describe("MSW mock layer", () => {
  it("resetMockDb() restores initial seed state after mutations", async () => {
    expect(mockDb.athletes).toHaveLength(3);

    const created = await jsonFetch<{ id: string }>("/api/v1/athletes", {
      method: "POST",
      body: JSON.stringify({
        name: "Atleta Temporário",
        email: "temp@procoach.app",
        sex: "MALE",
        birthDate: "1994-05-10",
        height: 175,
        weight: 78,
        goal: "MAINTENANCE",
      }),
    });

    expect(created.status).toBe(201);
    expect(mockDb.athletes).toHaveLength(4);

    resetMockDb();
    expect(mockDb.athletes).toHaveLength(3);
  });

  it("authenticates coach and athlete with decodable JWTs and /auth/me", async () => {
    const coachLogin = await jsonFetch<{ accessToken: string }>("/api/v1/auth/login", {
      method: "POST",
      body: JSON.stringify({
        email: MOCK_COACH_CREDENTIALS.email,
        password: "secret",
      }),
    });
    expect(coachLogin.status).toBe(200);
    expect(decodeJwtPayload(coachLogin.data.accessToken)?.role).toBe("COACH");

    const athleteLogin = await jsonFetch<{ accessToken: string }>("/api/v1/auth/login", {
      method: "POST",
      body: JSON.stringify({
        email: MOCK_ATHLETE_CREDENTIALS.email,
        password: "secret",
      }),
    });
    expect(athleteLogin.status).toBe(200);
    const athletePayload = decodeJwtPayload(athleteLogin.data.accessToken);
    expect(athletePayload?.role).toBe("ATHLETE");
    expect(athletePayload?.athleteId).toBe(MOCK_ATHLETE_CREDENTIALS.athleteId);

    const me = await jsonFetch<{ role: string; athleteId?: string }>("/api/v1/auth/me", {
      headers: { Authorization: `Bearer ${athleteLogin.data.accessToken}` },
    });
    expect(me.status).toBe(200);
    expect(me.data.role).toBe("ATHLETE");
    expect(me.data.athleteId).toBe(MOCK_ATHLETE_CREDENTIALS.athleteId);

    const invalid = await jsonFetch<{ code: string }>("/api/v1/auth/login", {
      method: "POST",
      body: JSON.stringify({ email: MOCK_COACH_CREDENTIALS.email, password: "wrong" }),
    });
    expect(invalid.status).toBe(401);
  });

  it("enforces athlete invite conflicts (409) and revocation", async () => {
    const lucasId = MOCK_ATHLETE_CREDENTIALS.athleteId;
    const conflictCredential = await jsonFetch<{ code: string }>("/api/v1/auth/invites", {
      method: "POST",
      body: JSON.stringify({ athleteId: lucasId, email: "novo@procoach.app" }),
    });
    expect(conflictCredential.status).toBe(409);
    expect(conflictCredential.data.code).toBe("ATHLETE_HAS_CREDENTIAL");

    const rafaelId = mockDb.athletes[2].id;
    const conflictEmail = await jsonFetch<{ code: string }>("/api/v1/auth/invites", {
      method: "POST",
      body: JSON.stringify({ athleteId: rafaelId, email: MOCK_ATHLETE_CREDENTIALS.email }),
    });
    expect(conflictEmail.status).toBe(409);
    expect(conflictEmail.data.code).toBe("EMAIL_IN_USE");

    const createdInvite = await jsonFetch<{ invitePath: string }>("/api/v1/auth/invites", {
      method: "POST",
      body: JSON.stringify({ athleteId: rafaelId, email: "rafael.novo@procoach.app" }),
    });
    expect(createdInvite.status).toBe(201);
    expect(createdInvite.data.invitePath).toMatch(/^\/convite\//);

    const invitesList = await jsonFetch<Array<{ id: string; status: string }>>(
      `/api/v1/athletes/${rafaelId}/invites`
    );
    expect(invitesList.data[0].status).toBe("PENDING");

    const revoked = await jsonFetch<{ status: string }>(
      `/api/v1/athletes/${rafaelId}/invites/${invitesList.data[0].id}/revoke`,
      { method: "POST" }
    );
    expect(revoked.status).toBe(200);
    expect(revoked.data.status).toBe("REVOKED");
  });

  it("freezes previous diet version on new version, enforces athlete APPROVED rule, and returns 409 energy-balance without assessment", async () => {
    const lucasId = MOCK_ATHLETE_CREDENTIALS.athleteId;
    const marianaId = mockDb.athletes[1].id;

    // Atleta sem dieta aprovada recebe 404
    const marianaDiet = await jsonFetch<{ message: string }>(
      `/api/v1/diets?athleteId=${marianaId}`
    );
    expect(marianaDiet.status).toBe(404);

    // Atleta sem avaliação recebe 409 no balanço energético
    const marianaEnergy = await jsonFetch<{ code: string }>(
      `/api/v1/diets/energy-balance?athleteId=${marianaId}`
    );
    expect(marianaEnergy.status).toBe(409);

    // Nova versão congela a v2 anterior (readOnly: true)
    const newVersion = await jsonFetch<{ id: string; version: number; readOnly: boolean }>(
      "/api/v1/diets/diet-lucas-v2/versions",
      {
        method: "POST",
        body: JSON.stringify({
          objective: "HYPERTROPHY",
          calories: 3100,
          protein: 210,
          carbohydrates: 400,
          fat: 73,
          changeReason: "Progressão calórica",
        }),
      }
    );
    expect(newVersion.status).toBe(201);
    expect(newVersion.data.version).toBe(3);
    expect(newVersion.data.readOnly).toBe(false);

    const oldVersion = mockDb.diets.find((item) => item.id === "diet-lucas-v2");
    expect(oldVersion?.readOnly).toBe(true);

    // Lucas tem avaliação, logo o balanço energético responde 200
    const lucasEnergy = await jsonFetch<{ dietId: string; calories: number }>(
      `/api/v1/diets/energy-balance?athleteId=${lucasId}`
    );
    expect(lucasEnergy.status).toBe(200);
    expect(lucasEnergy.data.calories).toBe(3100);

    // CRUD de refeições e itens (PR #133): retorna ApiDiet com totals recalculados
    const addedMeal = await jsonFetch<{
      id: string;
      meals: Array<{ id: string; name: string; items: Array<{ id: string }> }>;
      totals: { calories: number };
    }>(`/api/v1/diets/${newVersion.data.id}/meals`, {
      method: "POST",
      body: JSON.stringify({ name: "Ceia", order: 4, time: "22:00" }),
    });
    expect(addedMeal.status).toBe(201);
    const createdMeal = addedMeal.data.meals.find((m) => m.name === "Ceia");
    expect(createdMeal).toBeDefined();

    const addedItem = await jsonFetch<{
      meals: Array<{ id: string; items: Array<{ id: string; name: string }> }>;
      totals: { calories: number };
    }>(`/api/v1/diets/${newVersion.data.id}/meals/${createdMeal!.id}/items`, {
      method: "POST",
      body: JSON.stringify({
        name: "Caseína",
        source: "CUSTOM",
        kind: "SUPPLEMENT_CALORIC",
        state: "RAW",
        quantityG: 30,
        calories: 110,
        protein: 24,
        carbohydrates: 2,
        fat: 0.5,
      }),
    });
    expect(addedItem.status).toBe(201);
    expect(addedItem.data.totals.calories).toBe(1355);

    // Versão congelada (readOnly: true) bloqueia edição de refeições com 409
    const frozenMutation = await jsonFetch<{ message: string }>(
      "/api/v1/diets/diet-lucas-v2/meals",
      {
        method: "POST",
        body: JSON.stringify({ name: "Bloqueada", order: 5 }),
      }
    );
    expect(frozenMutation.status).toBe(409);
  });

  it("supports assessments creation, training versioning, and notifications read state", async () => {
    const lucasId = MOCK_ATHLETE_CREDENTIALS.athleteId;

    const assess = await jsonFetch<{ metrics: { imc: number; weightChangeKg: number } }>(
      "/api/v1/assessments",
      {
        method: "POST",
        body: JSON.stringify({
          athleteId: lucasId,
          assessmentDate: "2026-11-10",
          weightKg: 84.0,
          heightCm: 178,
        }),
      }
    );
    expect(assess.status).toBe(201);
    expect(assess.data.metrics.weightChangeKg).toBe(1.5);

    const clonedPlan = await jsonFetch<{ id: string; version: number }>(
      "/api/training/plans/plan-lucas-v2/versions",
      {
        method: "POST",
        body: JSON.stringify({
          newName: "Upper / Lower v3",
          newDescription: "Novo bloco",
          copySessions: true,
        }),
      }
    );
    expect(clonedPlan.status).toBe(201);
    expect(clonedPlan.data.version).toBe(3);

    const sessions = await jsonFetch<Array<{ id: string }>>(
      `/api/training/trainings/athlete/${lucasId}?planId=${clonedPlan.data.id}`
    );
    expect(sessions.data).toHaveLength(2);

    const readAll = await jsonFetch<{ marked: number }>("/api/v1/notifications/read-all", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${
          (
            await jsonFetch<{ accessToken: string }>("/api/v1/auth/login", {
              method: "POST",
              body: JSON.stringify({
                email: MOCK_ATHLETE_CREDENTIALS.email,
                password: "secret",
              }),
            })
          ).data.accessToken
        }`,
      },
    });
    expect(readAll.status).toBe(200);
    expect(readAll.data.marked).toBe(2);
  });
});
