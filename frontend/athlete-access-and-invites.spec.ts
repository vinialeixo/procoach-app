import { expect, test } from "@playwright/test";

/**
 * Integração real frontend <-> backend do estado de acesso do atleta
 * (T-077 / T-082, issues #98 e #99): cria a credencial do coach, cadastra o
 * atleta, gera o convite pelo mesmo contrato usado pela SPA, confere a
 * listagem (sem token), o badge de convite pendente e a revogação na UI.
 *
 * Roda com o backend Java em http://127.0.0.1:8080 e o Vite em :5173
 * (ver `playwright.config.ts`).
 */

const backendUrl = "http://127.0.0.1:8080";
const apiUrl = `${backendUrl}/api/v1`;

function unique(prefix: string) {
  const suffix = `${Date.now()}-${Math.floor(Math.random() * 1_000_000)}`;
  return `${prefix}-${suffix}@test.com`;
}

async function registerCoach(
  request: import("@playwright/test").APIRequestContext,
  email: string
) {
  const response = await request.post(`${apiUrl}/auth/register`, {
    data: { name: "Coach de integração", email, password: "senha123!" },
  });
  expect(response.status()).toBe(201);
  const body = (await response.json()) as { accessToken: string };
  return body.accessToken;
}

async function createAthlete(
  request: import("@playwright/test").APIRequestContext,
  accessToken: string,
  name: string,
  email?: string
) {
  const response = await request.post(`${apiUrl}/athletes`, {
    headers: { Authorization: `Bearer ${accessToken}` },
    data: {
      name,
      ...(email ? { email } : {}),
      sex: "FEMALE",
      birthDate: "1995-07-20",
      height: 165.5,
      weight: 60.2,
      goal: "HYPERTROPHY",
    },
  });
  expect(response.status()).toBe(201);
  return (await response.json()) as {
    id: string;
    hasCredential: boolean;
    pendingInvite: null | { id: string; email: string; expiresAt: string };
  };
}

async function signInAsCoach(
  page: import("@playwright/test").Page,
  accessToken: string
) {
  await page.addInitScript((token) => {
    window.localStorage.setItem("procoach.token", token);
  }, accessToken);
}

test("convite pendente aparece na ficha, some ao revogar e o backend reflete (T-077/T-082)", async ({
  page,
  request,
}) => {
  const coachToken = await registerCoach(request, unique("coach-athlete-access"));
  const athleteEmail = unique("atleta-convite");
  const athlete = await createAthlete(
    request,
    coachToken,
    "Atleta Convite Integração",
    athleteEmail
  );

  // Atleta recém-cadastrado: sem credencial e sem convite pendente.
  expect(athlete.hasCredential).toBe(false);
  expect(athlete.pendingInvite).toBeNull();

  const inviteResponse = await request.post(`${apiUrl}/auth/invites`, {
    headers: { Authorization: `Bearer ${coachToken}` },
    data: { athleteId: athlete.id, email: athleteEmail },
  });
  expect(inviteResponse.status()).toBe(201);
  const invite = (await inviteResponse.json()) as {
    athleteId: string;
    email: string;
    token: string;
    invitePath: string;
    expiresAt: string;
  };
  expect(invite.athleteId).toBe(athlete.id);
  expect(invite.invitePath).toBe(`/convite/${invite.token}`);

  // A ficha do atleta (mesma query usada pela SPA) passa a trazer o convite.
  const detailResponse = await request.get(`${apiUrl}/athletes/${athlete.id}`, {
    headers: { Authorization: `Bearer ${coachToken}` },
  });
  expect(detailResponse.status()).toBe(200);
  const detail = (await detailResponse.json()) as {
    hasCredential: boolean;
    pendingInvite: { id: string; email: string; expiresAt: string };
  };
  expect(detail.hasCredential).toBe(false);
  expect(detail.pendingInvite.email).toBe(athleteEmail);

  // A listagem (T-077) devolve o histórico/status e nunca o token.
  const listResponse = await request.get(
    `${apiUrl}/athletes/${athlete.id}/invites`,
    { headers: { Authorization: `Bearer ${coachToken}` } }
  );
  expect(listResponse.status()).toBe(200);
  const invites = (await listResponse.json()) as Array<{
    id: string;
    athleteId: string;
    email: string;
    expiresAt: string;
    createdAt: string;
    status: string;
    token?: string;
  }>;
  expect(invites).toHaveLength(1);
  expect(invites[0]).toMatchObject({
    athleteId: athlete.id,
    email: athleteEmail,
    status: "PENDING",
  });
  expect(invites[0]).not.toHaveProperty("token");
  expect(invites[0]).not.toHaveProperty("invitePath");
  expect(invites[0].expiresAt).toBeTruthy();
  expect(invites[0].createdAt).toBeTruthy();

  // UI real: entra como coach e abre a ficha do atleta.
  await signInAsCoach(page, coachToken);
  await page.goto(`/coach/athletes/${athlete.id}`);

  await expect(
    page.getByRole("heading", { name: "Atleta Convite Integração" })
  ).toBeVisible();
  await expect(page.getByText("Convite pendente").first()).toBeVisible();
  await expect(
    page.getByText(`Convite pendente: ${athleteEmail}`)
  ).toBeVisible();

  // Revoga pela UI (o window.confirm é aceito no handler de diálogo).
  page.on("dialog", (dialog) => dialog.accept());
  await page.getByRole("button", { name: "Revogar convite" }).click();

  await expect(
    page.getByText(/O atleta ainda não tem acesso nem convite pendente/)
  ).toBeVisible();
  await expect(page.getByText("Sem acesso").first()).toBeVisible();

  // O backend (fonte de verdade) também passou a devolver REVOKED.
  const revokedListResponse = await request.get(
    `${apiUrl}/athletes/${athlete.id}/invites`,
    { headers: { Authorization: `Bearer ${coachToken}` } }
  );
  const revokedInvites = (await revokedListResponse.json()) as Array<{
    status: string;
  }>;
  expect(revokedInvites).toHaveLength(1);
  expect(revokedInvites[0].status).toBe("REVOKED");
});

test("aceite do convite vira credencial ativa e desabilita novo convite (T-077/T-079)", async ({
  page,
  request,
}) => {
  const coachToken = await registerCoach(request, unique("coach-athlete-accept"));
  const athleteEmail = unique("atleta-aceite");
  const athlete = await createAthlete(
    request,
    coachToken,
    "Atleta Aceite Integração",
    athleteEmail
  );

  const inviteResponse = await request.post(`${apiUrl}/auth/invites`, {
    headers: { Authorization: `Bearer ${coachToken}` },
    data: { athleteId: athlete.id, email: athleteEmail },
  });
  expect(inviteResponse.status()).toBe(201);
  const { token } = (await inviteResponse.json()) as { token: string };

  // O atleta define a senha pelo link público e já sai autenticado.
  const acceptResponse = await request.post(
    `${apiUrl}/auth/invites/${token}/accept`,
    { data: { password: "senha-Forte-123" } }
  );
  expect(acceptResponse.status()).toBe(200);
  const session = (await acceptResponse.json()) as { accessToken: string };
  expect(session.accessToken).toBeTruthy();

  // A credencial passa a existir: hasCredential = true e sem pendência.
  const detailResponse = await request.get(`${apiUrl}/athletes/${athlete.id}`, {
    headers: { Authorization: `Bearer ${coachToken}` },
  });
  const detail = (await detailResponse.json()) as {
    hasCredential: boolean;
    pendingInvite: unknown;
  };
  expect(detail.hasCredential).toBe(true);
  expect(detail.pendingInvite).toBeNull();

  const listResponse = await request.get(
    `${apiUrl}/athletes/${athlete.id}/invites`,
    { headers: { Authorization: `Bearer ${coachToken}` } }
  );
  const invites = (await listResponse.json()) as Array<{ status: string }>;
  expect(invites[0].status).toBe("ACCEPTED");

  // Novo convite para quem já tem credencial: 409 acionável (T-079).
  const conflictResponse = await request.post(`${apiUrl}/auth/invites`, {
    headers: { Authorization: `Bearer ${coachToken}` },
    data: { athleteId: athlete.id, email: unique("outro-email") },
  });
  expect(conflictResponse.status()).toBe(409);
  const conflict = (await conflictResponse.json()) as { code: string };
  expect(conflict.code).toBe("ATHLETE_HAS_CREDENTIAL");

  // UI: badge "Tem acesso" e botão de novo convite desabilitado.
  await signInAsCoach(page, coachToken);
  await page.goto(`/coach/athletes/${athlete.id}`);

  await expect(
    page.getByRole("heading", { name: "Atleta Aceite Integração" })
  ).toBeVisible();
  await expect(page.getByText("Tem acesso").first()).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Convidar para o app" })
  ).toBeDisabled();
});
